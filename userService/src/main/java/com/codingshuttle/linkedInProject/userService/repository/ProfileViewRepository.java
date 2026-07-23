package com.codingshuttle.linkedInProject.userService.repository;

import com.codingshuttle.linkedInProject.userService.entity.ProfileView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProfileViewRepository extends JpaRepository<ProfileView, Long> {

    Optional<ProfileView> findByProfileUserIdAndViewerUserId(Long profileUserId, Long viewerUserId);

    Page<ProfileView> findByProfileUserIdOrderByLastViewedAtDesc(Long profileUserId, Pageable pageable);

    long countByProfileUserIdAndLastViewedAtAfter(Long profileUserId, LocalDateTime after);
}
