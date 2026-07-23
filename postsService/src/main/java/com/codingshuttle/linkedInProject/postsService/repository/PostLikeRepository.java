package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    void deleteByPostId(Long postId);

    // --- Batch variants: one query for a whole page instead of one per post ---

    /** [postId, likeCount] rows for the given posts. Posts with no likes are absent. */
    @Query("select pl.postId, count(pl) from PostLike pl where pl.postId in :ids group by pl.postId")
    List<Object[]> countByPostIdIn(@Param("ids") List<Long> ids);

    /** Which of these posts the user has liked. */
    @Query("select pl.postId from PostLike pl where pl.userId = :userId and pl.postId in :ids")
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
