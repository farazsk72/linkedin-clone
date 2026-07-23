package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.postsService.dto.PageResponse;
import com.codingshuttle.linkedInProject.postsService.dto.PostDto;
import com.codingshuttle.linkedInProject.postsService.dto.TrendingHashtagDto;
import com.codingshuttle.linkedInProject.postsService.entity.FollowedHashtag;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.postsService.repository.FollowedHashtagRepository;
import com.codingshuttle.linkedInProject.postsService.repository.PostHashtagRepository;
import com.codingshuttle.linkedInProject.postsService.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HashtagService {

    private final FollowedHashtagRepository followedHashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    private String normalise(String tag) {
        if(tag == null || tag.isBlank()) {
            throw new BadRequestException("Tag is required");
        }
        return tag.trim().toLowerCase().replaceFirst("^#", "");
    }

    @Transactional
    public void followTag(String tag) {
        Long userId = AuthContextHolder.getCurrentUserId();
        String normalised = normalise(tag);

        // Idempotent - following a tag twice changes nothing.
        if(followedHashtagRepository.findByUserIdAndTag(userId, normalised).isPresent()) {
            return;
        }
        FollowedHashtag followed = new FollowedHashtag();
        followed.setUserId(userId);
        followed.setTag(normalised);
        followedHashtagRepository.save(followed);
        log.info("User with ID: {} followed tag: {}", userId, normalised);
    }

    @Transactional
    public void unfollowTag(String tag) {
        Long userId = AuthContextHolder.getCurrentUserId();
        followedHashtagRepository.deleteByUserIdAndTag(userId, normalise(tag));
    }

    public List<String> getFollowedTags() {
        Long userId = AuthContextHolder.getCurrentUserId();
        return followedHashtagRepository.findByUserIdOrderByTagAsc(userId).stream()
                .map(FollowedHashtag::getTag)
                .toList();
    }

    /** Public posts carrying any tag the caller follows, newest first. */
    public PageResponse<PostDto> getFollowedTagFeed(int page, int size) {
        Long userId = AuthContextHolder.getCurrentUserId();

        List<String> tags = getFollowedTags();
        if(tags.isEmpty()) {
            return PageResponse.fromContent(Page.empty(pageRequest(page, size)), List.of());
        }

        Page<Post> posts = postRepository.findPublicByHashtagsIn(tags, pageRequest(page, size));
        return PageResponse.fromContent(posts, postService.toDtos(posts.getContent(), userId));
    }

    /** Top tags over the last `days` days. days and limit are both clamped. */
    public List<TrendingHashtagDto> getTrending(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(Math.min(Math.max(days, 1), 90));
        PageRequest pageRequest = PageRequest.of(0, Math.min(Math.max(limit, 1), 50));

        return postHashtagRepository.findTrending(since, pageRequest).stream()
                .map((row) -> new TrendingHashtagDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }
}
