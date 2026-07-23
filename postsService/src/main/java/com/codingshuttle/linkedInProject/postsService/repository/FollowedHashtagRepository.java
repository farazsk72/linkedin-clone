package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.FollowedHashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowedHashtagRepository extends JpaRepository<FollowedHashtag, Long> {

    List<FollowedHashtag> findByUserIdOrderByTagAsc(Long userId);

    Optional<FollowedHashtag> findByUserIdAndTag(Long userId, String tag);

    void deleteByUserIdAndTag(Long userId, String tag);
}
