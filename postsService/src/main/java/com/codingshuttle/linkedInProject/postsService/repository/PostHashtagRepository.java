package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.PostHashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    List<PostHashtag> findByPostId(Long postId);

    /**
     * Tags for a whole page of posts in one query. Ordered by id so each post's
     * tags come back in insertion (extraction) order, matching findByPostId.
     */
    List<PostHashtag> findByPostIdInOrderByIdAsc(List<Long> postIds);

    void deleteByPostId(Long postId);

    /**
     * Tags ranked by how many public, published posts carry them since `since`.
     * Joined to Post so a tag on a draft or a connections-only post does not
     * inflate the ranking - trending is a public, discovery-facing signal.
     * Returns rows of [tag, count]; the Pageable caps the result.
     */
    @Query("select ph.tag, count(ph) from PostHashtag ph, Post p "
            + "where ph.postId = p.id and p.status = 'PUBLISHED' and p.visibility = 'PUBLIC' "
            + "and p.createdAt >= :since "
            + "group by ph.tag order by count(ph) desc, ph.tag asc")
    List<Object[]> findTrending(@Param("since") LocalDateTime since, Pageable pageable);
}
