package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.postsService.client.ConnectionsGateway;
import com.codingshuttle.linkedInProject.postsService.dto.PageResponse;
import com.codingshuttle.linkedInProject.postsService.dto.PersonDto;
import com.codingshuttle.linkedInProject.postsService.dto.PostDto;
import com.codingshuttle.linkedInProject.postsService.dto.PostUpdateRequestDto;
import com.codingshuttle.linkedInProject.postsService.dto.RepostRequestDto;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.entity.PostHashtag;
import com.codingshuttle.linkedInProject.postsService.event.PostReposted;
import com.codingshuttle.linkedInProject.postsService.repository.PostHashtagRepository;
import com.codingshuttle.linkedInProject.postsService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.postsService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.postsService.entity.Comment;
import com.codingshuttle.linkedInProject.postsService.repository.CommentLikeRepository;
import com.codingshuttle.linkedInProject.postsService.repository.CommentRepository;
import com.codingshuttle.linkedInProject.postsService.repository.PostLikeRepository;
import com.codingshuttle.linkedInProject.postsService.repository.PostRepository;
import com.codingshuttle.linkedInProject.postsService.repository.SavedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";

    private final CommentLikeRepository commentLikeRepository;
    private final SavedPostRepository savedPostRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final ModelMapper modelMapper;
    private final ConnectionsGateway connectionsGateway;
    private final OutboxWriter outboxWriter;
    private final PostEventPublisher eventPublisher;

    // Post creation now lives in PostCreationSaga (upload -> persist -> publish,
    // with compensations). PostService keeps the reads, edits, repost, and the
    // shared toDto builder.

    public PostDto getPostById(Long postId) {
        log.info("Getting the post with ID: {}", postId);

        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found " +
                "with ID: "+postId));

        Long currentUserId = AuthContextHolder.getCurrentUserId();

        // A draft is visible only to its author, and reported as not-found to
        // anyone else so the permalink cannot confirm it exists.
        if(DRAFT.equals(post.getStatus()) && !post.getUserId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Post not found with ID: "+postId);
        }
        // Same for a CONNECTIONS-only post seen by a non-connection.
        if("CONNECTIONS".equals(post.getVisibility())
                && !canSeeConnectionsOnly(post.getUserId(), currentUserId)) {
            throw new ResourceNotFoundException("Post not found with ID: "+postId);
        }

        return toDto(post, currentUserId);
    }

    public PageResponse<PostDto> getAllPostsOfUser(Long userId, int page, int size) {
        log.info("Getting posts of a user with ID: {}, page: {}", userId, page);

        Long currentUserId = AuthContextHolder.getCurrentUserId();

        // A stranger viewing a profile must not see CONNECTIONS-only posts.
        // Filtering in the query rather than after paging, so page counts stay
        // honest instead of pages arriving partly empty.
        Page<Post> posts = canSeeConnectionsOnly(userId, currentUserId)
                ? postRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, PUBLISHED,
                        pageRequest(page, size))
                : postRepository.findByUserIdAndStatusAndVisibilityOrderByCreatedAtDesc(userId,
                        PUBLISHED, "PUBLIC", pageRequest(page, size));

        return PageResponse.fromContent(posts, toDtos(posts.getContent(), currentUserId));
    }

    /**
     * Public posts by the people the caller follows. Kept separate from the
     * main feed because following is not a connection - a followed user's
     * CONNECTIONS-only posts must stay hidden, so this asks for PUBLIC only.
     */
    public PageResponse<PostDto> getFollowingFeed(int page, int size) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Building following-feed for user with ID: {}, page: {}", userId, page);

        List<Long> followedIds = connectionsGateway.getFollowingIds();
        if(followedIds.isEmpty()) {
            return PageResponse.fromContent(Page.empty(pageRequest(page, size)), List.of());
        }

        Page<Post> posts = postRepository.findByUserIdInAndStatusAndVisibilityOrderByCreatedAtDesc(
                followedIds, PUBLISHED, "PUBLIC", pageRequest(page, size));
        return PageResponse.fromContent(posts, toDtos(posts.getContent(), userId));
    }

    /** The caller's own unpublished posts - there is no way to ask for anyone else's. */
    public PageResponse<PostDto> getMyDrafts(int page, int size) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Getting drafts for user with ID: {}", userId);

        Page<Post> posts = postRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, DRAFT,
                pageRequest(page, size));
        return PageResponse.fromContent(posts, toDtos(posts.getContent(), userId));
    }

    @Transactional
    public PostDto publishDraft(Long postId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} publishing the draft with ID: {}", userId, postId);

        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found " +
                "with ID: "+postId));

        if(!post.getUserId().equals(userId)) {
            throw new BadRequestException("You cannot publish someone else's post");
        }
        if(!DRAFT.equals(post.getStatus())) {
            throw new BadRequestException("That post is already published");
        }

        post.setStatus(PUBLISHED);
        post = postRepository.saveAndFlush(post);

        // Connections and mentioned users are told now, not when the draft was written.
        eventPublisher.notifyConnections(post, userId);
        eventPublisher.notifyMentions(post, userId);

        return toDto(post, userId);
    }

    public PageResponse<PostDto> getPostsByHashtag(String tag, int page, int size) {
        Long currentUserId = AuthContextHolder.getCurrentUserId();
        log.info("Getting posts for hashtag: {}, page: {}", tag, page);

        String normalised = tag == null ? "" : tag.trim().toLowerCase().replaceFirst("^#", "");

        // Only public posts surface through tag browsing - a hashtag is a
        // discovery surface, and honouring per-viewer visibility here would
        // mean a connection check per author on every page.
        Page<Post> posts = postRepository.findPublicByHashtag(normalised, pageRequest(page, size));
        return PageResponse.fromContent(posts, toDtos(posts.getContent(), currentUserId));
    }

    @Transactional
    public PostDto repost(Long originalPostId, RepostRequestDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} reposting the post with ID: {}", userId, originalPostId);

        Post original = postRepository.findById(originalPostId).orElseThrow(()
                -> new ResourceNotFoundException("Post not found with ID: "+originalPostId));

        // Reposting a repost points at the underlying original, so the chain
        // never grows past one level.
        Long targetId = original.getOriginalPostId() != null
                ? original.getOriginalPostId()
                : original.getId();
        Post target = postRepository.findById(targetId).orElseThrow(()
                -> new ResourceNotFoundException("Post not found with ID: "+targetId));

        // An unpublished post cannot be reshared - reposting it would publish
        // someone else's draft on their behalf.
        if(DRAFT.equals(target.getStatus())) {
            throw new ResourceNotFoundException("Post not found with ID: "+targetId);
        }

        if(target.getUserId().equals(userId) && dto.getContent() == null) {
            throw new BadRequestException("Add a comment when resharing your own post");
        }

        Post repost = new Post();
        repost.setUserId(userId);
        // Content is NOT NULL, and a bare repost carries no commentary.
        repost.setContent(dto.getContent() == null ? "" : dto.getContent().trim());
        repost.setOriginalPostId(target.getId());
        repost.setVisibility(normaliseVisibility(dto.getVisibility()));

        repost = postRepository.save(repost);
        syncHashtags(repost);

        if(!target.getUserId().equals(userId)) {
            // Outbox, in this transaction - the repost and its notification commit together.
            outboxWriter.write("post_reposted_topic", target.getId(), PostReposted.builder()
                    .postId(target.getId())
                    .ownerUserId(target.getUserId())
                    .repostedByUserId(userId)
                    .build());
        }

        return toDto(repost, userId);
    }

    /**
     * True when the viewer is allowed to see the owner's CONNECTIONS-only
     * posts: either it is their own profile, or they are first-degree.
     */
    private boolean canSeeConnectionsOnly(Long ownerId, Long viewerId) {
        if(viewerId == null) return false;
        if(viewerId.equals(ownerId)) return true;

        // Ask "is the viewer connected to the owner" rather than fetching the
        // owner's whole connection list - which lets the list endpoint stay
        // locked to self, closing the "read anyone's connections" hole.
        return connectionsGateway.isConnectedTo(ownerId);
    }

    private String normaliseVisibility(String visibility) {
        return "CONNECTIONS".equalsIgnoreCase(visibility) ? "CONNECTIONS" : "PUBLIC";
    }

    /** Rewrites the tag rows for a post to match its current content. */
    private void syncHashtags(Post post) {
        postHashtagRepository.deleteByPostId(post.getId());

        for (String tag : HashtagExtractor.extract(post.getContent())) {
            PostHashtag row = new PostHashtag();
            row.setPostId(post.getId());
            row.setTag(tag);
            postHashtagRepository.save(row);
        }
    }

    public PageResponse<PostDto> getFeed(int page, int size) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Building feed for user with ID: {}, page: {}", userId, page);

        List<PersonDto> connections = connectionsGateway.getFirstDegreeConnections(userId);

        List<Long> authorIds = new ArrayList<>();
        authorIds.add(userId); // your own posts appear in your feed
        for (PersonDto person : connections) {
            authorIds.add(person.getUserId());
        }

        Page<Post> posts = postRepository.findByUserIdInAndStatusOrderByCreatedAtDesc(authorIds,
                PUBLISHED, pageRequest(page, size));
        return PageResponse.fromContent(posts, toDtos(posts.getContent(), userId));
    }

    @Transactional
    public PostDto updatePost(Long postId, PostUpdateRequestDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} updating the post with ID: {}", userId, postId);

        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found " +
                "with ID: "+postId));

        if(!post.getUserId().equals(userId)) {
            throw new BadRequestException("You cannot edit someone else's post");
        }

        if(dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BadRequestException("Post content cannot be empty");
        }

        post.setContent(dto.getContent().trim());
        syncHashtags(post); // an edit can add or remove tags
        if(dto.isRemoveImage()) {
            // Only the reference is dropped - the file stays in the bucket,
            // since nothing here owns the uploader's storage lifecycle.
            post.setImageUrl(null);
        }

        // saveAndFlush, not save: @UpdateTimestamp is applied by Hibernate at
        // flush time, and inside this transaction the flush would otherwise
        // happen after the DTO is built - returning the pre-edit timestamp.
        post = postRepository.saveAndFlush(post);
        return toDto(post, userId);
    }

    @Transactional
    public void deletePost(Long postId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} deleting the post with ID: {}", userId, postId);

        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found " +
                "with ID: "+postId));

        if(!post.getUserId().equals(userId)) {
            throw new BadRequestException("You cannot delete someone else's post");
        }

        // Likes and comments key off postId with no FK, so nothing cascades -
        // skipping these would leave rows that inflate counts for a future post
        // that reuses the id.
        postLikeRepository.deleteByPostId(postId);

        // Comment likes key off commentId, so they have to go before the
        // comments themselves - otherwise they outlive the rows they point at.
        List<Long> commentIds = commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream().map(Comment::getId).toList();
        if(!commentIds.isEmpty()) {
            commentLikeRepository.deleteByCommentIdIn(commentIds);
        }
        commentRepository.deleteByPostId(postId);
        savedPostRepository.deleteByPostId(postId);
        postHashtagRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    /** Clamped - page size lands straight in a SQL LIMIT. */
    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    /**
     * Single-post convenience, kept so the write paths (create/update/repost)
     * and callers in other services read naturally. It routes through the batch
     * builder, so there is exactly one implementation and no risk of the two
     * drifting apart the way toDto/plainDto used to.
     */
    PostDto toDto(Post post, Long currentUserId) {
        return toDtos(List.of(post), currentUserId).get(0);
    }

    /**
     * Builds a whole page of DTOs with a fixed number of queries regardless of
     * page size - the aggregate counts, the caller's like/save flags, and the
     * hashtags are each loaded once for the whole page, then assembled in
     * memory. This replaces the per-post lookups that made a 10-post page cost
     * 60+ queries.
     */
    List<PostDto> toDtos(List<Post> posts, Long currentUserId) {
        if(posts.isEmpty()) return List.of();

        // Originals of any reposts on the page - fetched once, then folded into
        // the same aggregate load so their counts cost nothing extra.
        Set<Long> originalIds = posts.stream()
                .map(Post::getOriginalPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Post> originals = originalIds.isEmpty()
                ? Map.of()
                : postRepository.findAllById(originalIds).stream()
                        .collect(Collectors.toMap(Post::getId, Function.identity()));

        Set<Long> allIds = new HashSet<>(originals.keySet());
        posts.forEach((post) -> allIds.add(post.getId()));

        Aggregates agg = loadAggregates(allIds, currentUserId);

        List<PostDto> result = new ArrayList<>(posts.size());
        for(Post post : posts) {
            PostDto dto = buildDto(post, agg);
            // One level only: the embedded original never carries its own
            // original, so a repost chain cannot produce an unbounded response.
            if(post.getOriginalPostId() != null) {
                Post original = originals.get(post.getOriginalPostId());
                if(original != null) {
                    PostDto originalDto = buildDto(original, agg);
                    originalDto.setOriginalPost(null);
                    dto.setOriginalPost(originalDto);
                }
            }
            result.add(dto);
        }
        return result;
    }

    private PostDto buildDto(Post post, Aggregates agg) {
        PostDto dto = modelMapper.map(post, PostDto.class);

        Map<String, Long> reactions = agg.reactionBreakdown().getOrDefault(post.getId(), Map.of());
        dto.setReactionCounts(reactions);
        // likeCount stays the total across every reaction type, so existing
        // callers reading it keep seeing the full engagement number.
        dto.setLikeCount(reactions.values().stream().mapToLong(Long::longValue).sum());
        String myReaction = agg.myReactions().get(post.getId());
        dto.setMyReaction(myReaction);
        dto.setLikedByMe(myReaction != null);

        dto.setCommentCount(agg.commentCounts().getOrDefault(post.getId(), 0L));
        dto.setSavedByMe(agg.savedIds().contains(post.getId()));
        dto.setHashtags(agg.hashtags().getOrDefault(post.getId(), List.of()));
        return dto;
    }

    /** Every per-post signal for a set of ids, loaded in one query each. */
    private Aggregates loadAggregates(Set<Long> ids, Long currentUserId) {
        if(ids.isEmpty()) {
            return new Aggregates(Map.of(), Map.of(), Map.of(), Set.of(), Map.of());
        }
        List<Long> idList = new ArrayList<>(ids);

        // Per-type reaction breakdown; likeCount and likedByMe are derived from
        // this in buildDto, so the old count/likedIds queries are not needed.
        Map<Long, Map<String, Long>> reactionBreakdown = new HashMap<>();
        for(Object[] row : postLikeRepository.reactionCountsByPostIdIn(idList)) {
            Long postId = ((Number) row[0]).longValue();
            String type = String.valueOf(row[1]);
            Long count = ((Number) row[2]).longValue();
            reactionBreakdown.computeIfAbsent(postId, (k) -> new HashMap<>()).put(type, count);
        }

        Map<Long, Long> commentCounts = toCountMap(commentRepository.countByPostIdIn(idList));

        // Skip the "mine" queries entirely for an unauthenticated read - matches
        // the old `currentUserId != null && ...` guard.
        Map<Long, String> myReactions = new HashMap<>();
        if(currentUserId != null) {
            for(Object[] row : postLikeRepository.findMyReactions(currentUserId, idList)) {
                myReactions.put(((Number) row[0]).longValue(), String.valueOf(row[1]));
            }
        }
        Set<Long> savedIds = currentUserId == null
                ? Set.of()
                : new HashSet<>(savedPostRepository.findSavedPostIds(currentUserId, idList));

        Map<Long, List<String>> hashtags = new HashMap<>();
        for(PostHashtag row : postHashtagRepository.findByPostIdInOrderByIdAsc(idList)) {
            hashtags.computeIfAbsent(row.getPostId(), (k) -> new ArrayList<>()).add(row.getTag());
        }

        return new Aggregates(reactionBreakdown, myReactions, commentCounts, savedIds, hashtags);
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for(Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }

    private record Aggregates(
            Map<Long, Map<String, Long>> reactionBreakdown,
            Map<Long, String> myReactions,
            Map<Long, Long> commentCounts,
            Set<Long> savedIds,
            Map<Long, List<String>> hashtags) {
    }
}
