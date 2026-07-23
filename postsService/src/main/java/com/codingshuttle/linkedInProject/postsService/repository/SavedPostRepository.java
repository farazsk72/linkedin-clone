package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    void deleteByPostId(Long postId);

    /** Ordered by when it was saved, not when the post was written. */
    Page<SavedPost> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Which of these posts the user has saved - one query for a whole page. */
    @Query("select sp.postId from SavedPost sp where sp.userId = :userId and sp.postId in :ids")
    List<Long> findSavedPostIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
