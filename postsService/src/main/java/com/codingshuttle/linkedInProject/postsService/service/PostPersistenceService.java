package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.dto.PostCreateRequestDto;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.entity.PostHashtag;
import com.codingshuttle.linkedInProject.postsService.repository.PostHashtagRepository;
import com.codingshuttle.linkedInProject.postsService.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The persist / un-persist half of the post-creation saga, in its own bean so
 * each method is a genuinely independent transaction. This is what makes the
 * saga a saga: {@link #persist} COMMITS before later steps run, so a later
 * failure has to be undone by {@link #deletePostAndTags} - a compensation - not
 * by a transaction rollback. (Were these methods on the orchestrator bean, the
 * self-invocation would skip the proxy and share one transaction, defeating the
 * point.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostPersistenceService {

    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";

    private final PostRepository postRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public Post persist(PostCreateRequestDto dto, Long userId, String imageUrl) {
        Post post = modelMapper.map(dto, Post.class);
        post.setUserId(userId);
        post.setImageUrl(imageUrl);
        post.setVisibility("CONNECTIONS".equalsIgnoreCase(dto.getVisibility()) ? "CONNECTIONS" : "PUBLIC");
        post.setStatus(dto.isDraft() ? DRAFT : PUBLISHED);

        post = postRepository.save(post);
        syncHashtags(post);
        log.info("saga step: persisted post {} (committed)", post.getId());
        return post;
    }

    @Transactional
    public void deletePostAndTags(Long postId) {
        // A freshly created post has no likes/comments/saves yet, so only the
        // post and its hashtag rows need removing.
        postHashtagRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
        log.info("saga compensation: deleted post {}", postId);
    }

    private void syncHashtags(Post post) {
        postHashtagRepository.deleteByPostId(post.getId());
        for (String tag : HashtagExtractor.extract(post.getContent())) {
            PostHashtag row = new PostHashtag();
            row.setPostId(post.getId());
            row.setTag(tag);
            postHashtagRepository.save(row);
        }
    }
}
