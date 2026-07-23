package com.codingshuttle.linkedInProject.userService.service;

import com.codingshuttle.linkedInProject.userService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.userService.dto.ChangePasswordRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.PageResponse;
import com.codingshuttle.linkedInProject.userService.dto.UpdateProfileRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.UserDto;
import com.codingshuttle.linkedInProject.userService.entity.User;
import com.codingshuttle.linkedInProject.userService.event.UserUpdatedEvent;
import com.codingshuttle.linkedInProject.userService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.userService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.userService.repository.BlockRepository;
import com.codingshuttle.linkedInProject.userService.repository.RefreshTokenRepository;
import com.codingshuttle.linkedInProject.userService.repository.UserRepository;
import com.codingshuttle.linkedInProject.userService.utils.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlockRepository blockRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<Long, UserUpdatedEvent> userUpdatedEventKafkaTemplate;

    public UserDto getUserById(Long userId) {
        log.info("Getting user with ID: {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found " +
                "with ID: "+userId));
        return modelMapper.map(user, UserDto.class);
    }

    public PageResponse<UserDto> searchUsers(String query, int page, int size) {
        Long currentUserId = AuthContextHolder.getCurrentUserId();
        log.info("Searching users with query: {}, page: {}", query, page);

        if(query == null || query.isBlank()) {
            return PageResponse.from(Page.empty(), (element) -> null);
        }

        // Clamped - the size lands straight in a SQL LIMIT.
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by("name").ascending());

        // The caller plus anyone they have blocked or been blocked by. Always
        // non-empty (the caller is in it), so the NOT IN stays valid.
        Set<Long> excludeIds = new HashSet<>(blockRepository.findAllRelatedUserIds(currentUserId));
        excludeIds.add(currentUserId);

        return PageResponse.from(userRepository.search(query.trim(), excludeIds, pageRequest),
                (element) -> modelMapper.map(element, UserDto.class));
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequestDto dto) {
        log.info("Updating profile of user with ID: {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found " +
                "with ID: "+userId));

        String previousName = user.getName();

        // Null means "not supplied, leave alone"; a blank string clears the
        // field. Name is the exception - it is NOT NULL, so blanking it would
        // fail at the database with an opaque constraint error.
        if(dto.getName() != null) {
            if(dto.getName().isBlank()) {
                throw new BadRequestException("Name cannot be blank");
            }
            user.setName(dto.getName().trim());
        }
        if(dto.getHeadline() != null) user.setHeadline(trimToNull(dto.getHeadline()));
        if(dto.getAbout() != null) user.setAbout(trimToNull(dto.getAbout()));
        if(dto.getAvatarUrl() != null) user.setAvatarUrl(trimToNull(dto.getAvatarUrl()));
        if(dto.getLocation() != null) user.setLocation(trimToNull(dto.getLocation()));
        if(dto.getCurrentCompany() != null) user.setCurrentCompany(trimToNull(dto.getCurrentCompany()));

        user = userRepository.save(user);

        // Only the name is mirrored in the connections graph, so a headline
        // edit does not need to bother ConnectionsService.
        if(!user.getName().equals(previousName)) {
            userUpdatedEventKafkaTemplate.send("user_updated_topic", UserUpdatedEvent.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .build());
        }

        return modelMapper.map(user, UserDto.class);
    }

    @Transactional
    public void changePassword(ChangePasswordRequestDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Changing password for user with ID: {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found " +
                "with ID: "+userId));

        // Requiring the current password is what stops a stolen or borrowed
        // token from being escalated into permanent account takeover.
        if(dto.getCurrentPassword() == null
                || !BCrypt.match(dto.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if(dto.getNewPassword() == null || dto.getNewPassword().length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters");
        }
        if(dto.getNewPassword().equals(dto.getCurrentPassword())) {
            throw new BadRequestException("New password must differ from the current one");
        }

        user.setPassword(BCrypt.hash(dto.getNewPassword()));
        // saveAndFlush, so the new hash is written before the revoke query
        // clears the persistence context underneath it.
        userRepository.saveAndFlush(user);

        // End every other session. Access tokens already issued still work for
        // up to their 15-minute expiry - the gateway has no revocation list to
        // consult - but no new ones can be minted from the old refresh tokens.
        int revoked = refreshTokenRepository.revokeAllForUser(userId);
        log.info("Revoked {} refresh tokens for user with ID: {}", revoked, userId);
    }

    private String trimToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
