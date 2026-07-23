package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Every read here filters on status. Drafts belong to exactly one query - the
 * author's own draft list - and leaking one into a feed, profile or tag page
 * would publish something the author never chose to publish.
 */
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);

    Page<Post> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status,
                                                         Pageable pageable);

    Page<Post> findByUserIdAndStatusAndVisibilityOrderByCreatedAtDesc(Long userId, String status,
                                                                      String visibility,
                                                                      Pageable pageable);

    Page<Post> findByUserIdInAndStatusOrderByCreatedAtDesc(List<Long> userIds, String status,
                                                           Pageable pageable);

    Page<Post> findByUserIdInAndStatusAndVisibilityOrderByCreatedAtDesc(
            List<Long> userIds, String status, String visibility, Pageable pageable);

    @Query("select p from Post p where p.visibility = 'PUBLIC' and p.status = 'PUBLISHED' and p.id in "
            + "(select h.postId from PostHashtag h where h.tag = :tag) "
            + "order by p.createdAt desc")
    Page<Post> findPublicByHashtag(@Param("tag") String tag, Pageable pageable);

    // distinct, so a post carrying two followed tags does not appear twice.
    @Query("select distinct p from Post p where p.visibility = 'PUBLIC' and p.status = 'PUBLISHED' and p.id in "
            + "(select h.postId from PostHashtag h where h.tag in :tags) "
            + "order by p.createdAt desc")
    Page<Post> findPublicByHashtagsIn(@Param("tags") List<String> tags, Pageable pageable);
}
