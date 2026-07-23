package com.codingshuttle.linkedInProject.userService.service;

import com.codingshuttle.linkedInProject.userService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.userService.dto.UserDto;
import com.codingshuttle.linkedInProject.userService.entity.Block;
import com.codingshuttle.linkedInProject.userService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.userService.repository.BlockRepository;
import com.codingshuttle.linkedInProject.userService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public void block(Long blockedUserId) {
        Long userId = AuthContextHolder.getCurrentUserId();

        if(userId.equals(blockedUserId)) {
            throw new BadRequestException("You cannot block yourself");
        }
        userRepository.findById(blockedUserId)
                .orElseThrow(() -> new BadRequestException("No such user"));

        // Blocking twice is a no-op. The unique constraint would reject it, but
        // catching it here keeps the endpoint idempotent rather than 500-ing.
        if(blockRepository.existsByBlockerUserIdAndBlockedUserId(userId, blockedUserId)) {
            return;
        }

        Block block = new Block();
        block.setBlockerUserId(userId);
        block.setBlockedUserId(blockedUserId);
        blockRepository.save(block);
        log.info("User with ID: {} blocked user with ID: {}", userId, blockedUserId);
    }

    @Transactional
    public void unblock(Long blockedUserId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        blockRepository.deleteByBlockerUserIdAndBlockedUserId(userId, blockedUserId);
    }

    /** Only who I blocked - not who blocked me, which I have no right to see. */
    public List<UserDto> getBlockedUsers() {
        Long userId = AuthContextHolder.getCurrentUserId();

        return blockRepository.findByBlockerUserId(userId).stream()
                .map((block) -> userRepository.findById(block.getBlockedUserId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map((user) -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}
