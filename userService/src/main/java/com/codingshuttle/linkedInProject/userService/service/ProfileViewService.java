package com.codingshuttle.linkedInProject.userService.service;

import com.codingshuttle.linkedInProject.userService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.userService.dto.PageResponse;
import com.codingshuttle.linkedInProject.userService.dto.ProfileViewerDto;
import com.codingshuttle.linkedInProject.userService.entity.ProfileView;
import com.codingshuttle.linkedInProject.userService.repository.ProfileViewRepository;
import com.codingshuttle.linkedInProject.userService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileViewService {

    private final ProfileViewRepository profileViewRepository;
    private final UserRepository userRepository;

    /**
     * Recorded by the profile endpoint itself. Viewing your own profile is not
     * a view, and neither is an unauthenticated read.
     */
    @Transactional
    public void record(Long profileUserId, Long viewerUserId) {
        if(viewerUserId == null || viewerUserId.equals(profileUserId)) return;

        ProfileView view = profileViewRepository
                .findByProfileUserIdAndViewerUserId(profileUserId, viewerUserId)
                .orElseGet(() -> {
                    ProfileView fresh = new ProfileView();
                    fresh.setProfileUserId(profileUserId);
                    fresh.setViewerUserId(viewerUserId);
                    return fresh;
                });

        view.setLastViewedAt(LocalDateTime.now());
        profileViewRepository.save(view);
    }

    /** Always the caller's own viewers - there is no way to ask about anyone else. */
    public PageResponse<ProfileViewerDto> getMyViewers(int page, int size) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Getting profile viewers for user with ID: {}", userId);

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));

        return PageResponse.from(
                profileViewRepository.findByProfileUserIdOrderByLastViewedAtDesc(userId, pageRequest),
                this::toDto);
    }

    /** Distinct viewers in the last 7 days, for the badge on the profile page. */
    public long getRecentViewerCount() {
        Long userId = AuthContextHolder.getCurrentUserId();
        return profileViewRepository.countByProfileUserIdAndLastViewedAtAfter(
                userId, LocalDateTime.now().minusDays(7));
    }

    private ProfileViewerDto toDto(ProfileView view) {
        ProfileViewerDto dto = new ProfileViewerDto();
        dto.setUserId(view.getViewerUserId());
        dto.setLastViewedAt(view.getLastViewedAt());

        // A deleted viewer leaves the row behind; the name stays null rather
        // than failing the whole page.
        userRepository.findById(view.getViewerUserId()).ifPresent((user) -> {
            dto.setName(user.getName());
            dto.setHeadline(user.getHeadline());
            dto.setAvatarUrl(user.getAvatarUrl());
        });
        return dto;
    }
}
