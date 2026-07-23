package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);

    /**
     * [postId, commentCount] for a whole page in one query. Counts every
     * comment row including replies, exactly as countByPostId does per-post.
     */
    @Query("select c.postId, count(c) from Comment c where c.postId in :ids group by c.postId")
    List<Object[]> countByPostIdIn(@Param("ids") List<Long> ids);

    void deleteByPostId(Long postId);

    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    void deleteByParentCommentId(Long parentCommentId);
}
