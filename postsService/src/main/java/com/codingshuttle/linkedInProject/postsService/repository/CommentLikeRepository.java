package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    long countByCommentId(Long commentId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    void deleteByCommentId(Long commentId);

    /** Used when a post or a parent comment takes a whole set of comments with it. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CommentLike cl where cl.commentId in :commentIds")
    void deleteByCommentIdIn(@Param("commentIds") List<Long> commentIds);

    // --- Batch variants for rendering a whole comment thread in a few queries ---

    @Query("select cl.commentId, count(cl) from CommentLike cl where cl.commentId in :ids group by cl.commentId")
    List<Object[]> countByCommentIdIn(@Param("ids") List<Long> ids);

    @Query("select cl.commentId from CommentLike cl where cl.userId = :userId and cl.commentId in :ids")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
