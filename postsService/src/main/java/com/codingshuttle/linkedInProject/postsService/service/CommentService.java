package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.postsService.dto.CommentCreateRequestDto;
import com.codingshuttle.linkedInProject.postsService.dto.CommentDto;
import com.codingshuttle.linkedInProject.postsService.entity.Comment;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.event.PostCommented;
import com.codingshuttle.linkedInProject.postsService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.postsService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.postsService.entity.CommentLike;
import com.codingshuttle.linkedInProject.postsService.repository.CommentLikeRepository;
import com.codingshuttle.linkedInProject.postsService.repository.CommentRepository;
import com.codingshuttle.linkedInProject.postsService.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    private final OutboxWriter outboxWriter;

    @Transactional
    public CommentDto addComment(Long postId, CommentCreateRequestDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} commenting on the post with ID: {}", userId, postId);

        if(dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BadRequestException("Comment cannot be empty");
        }

        Post post = postRepository.findById(postId).orElseThrow(()
                -> new ResourceNotFoundException("Post not found with ID: "+postId));

        // One level deep: replying to a reply attaches to that reply's parent,
        // so threads stay flat rather than becoming an unbounded tree.
        Long parentId = null;
        if(dto.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(dto.getParentCommentId()).orElseThrow(()
                    -> new ResourceNotFoundException("Comment not found with ID: "+dto.getParentCommentId()));

            if(!parent.getPostId().equals(postId)) {
                throw new BadRequestException("That comment belongs to a different post");
            }
            parentId = parent.getParentCommentId() != null ? parent.getParentCommentId() : parent.getId();
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentCommentId(parentId);
        comment.setContent(dto.getContent().trim());
        comment = commentRepository.save(comment);

        // Commenting on your own post should not notify you. Queued via the
        // outbox in this transaction, so the comment and its notification are
        // atomic.
        if(!post.getUserId().equals(userId)) {
            outboxWriter.write("post_commented_topic", postId, PostCommented.builder()
                    .postId(postId)
                    .ownerUserId(post.getUserId())
                    .commentedByUserId(userId)
                    .content(comment.getContent())
                    .build());
        }

        return toDto(comment, userId);
    }

    @Transactional
    public CommentDto updateComment(Long commentId, CommentCreateRequestDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} editing the comment with ID: {}", userId, commentId);

        Comment comment = commentRepository.findById(commentId).orElseThrow(()
                -> new ResourceNotFoundException("Comment not found with ID: "+commentId));

        // Only the author may edit. The post owner can delete a comment on
        // their post but must not be able to put words in someone's mouth.
        if(!comment.getUserId().equals(userId)) {
            throw new BadRequestException("You cannot edit someone else's comment");
        }
        if(dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BadRequestException("Comment cannot be empty");
        }

        comment.setContent(dto.getContent().trim());
        // saveAndFlush so @UpdateTimestamp has been applied before the DTO is
        // built - otherwise the response carries the pre-edit timestamp.
        comment = commentRepository.saveAndFlush(comment);

        return toDto(comment, userId);
    }

    @Transactional
    public CommentDto likeComment(Long commentId) {
        Long userId = AuthContextHolder.getCurrentUserId();

        Comment comment = commentRepository.findById(commentId).orElseThrow(()
                -> new ResourceNotFoundException("Comment not found with ID: "+commentId));

        // Liking twice is a no-op rather than an error - the intent is already
        // satisfied, unlike post likes which report it.
        if(!commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            commentLikeRepository.save(like);
        }
        return toDto(comment, userId);
    }

    @Transactional
    public CommentDto unlikeComment(Long commentId) {
        Long userId = AuthContextHolder.getCurrentUserId();

        Comment comment = commentRepository.findById(commentId).orElseThrow(()
                -> new ResourceNotFoundException("Comment not found with ID: "+commentId));

        commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
        return toDto(comment, userId);
    }

    private CommentDto toDto(Comment comment, Long currentUserId) {
        CommentDto dto = modelMapper.map(comment, CommentDto.class);
        dto.setLikeCount(commentLikeRepository.countByCommentId(comment.getId()));
        dto.setLikedByMe(currentUserId != null
                && commentLikeRepository.existsByUserIdAndCommentId(currentUserId, comment.getId()));
        return dto;
    }

    public List<CommentDto> getComments(Long postId) {
        log.info("Getting comments of the post with ID: {}", postId);

        postRepository.findById(postId).orElseThrow(()
                -> new ResourceNotFoundException("Post not found with ID: "+postId));

        Long currentUserId = AuthContextHolder.getCurrentUserId();

        // One query returns every comment for the post, replies included, so the
        // reply grouping is done in memory rather than a query per parent. Like
        // counts and my-likes are then two batch queries for the whole thread -
        // this used to be ~3 queries per comment.
        List<Comment> all = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        if(all.isEmpty()) return List.of();

        List<Long> ids = all.stream().map(Comment::getId).toList();
        Map<Long, Long> likeCounts = new HashMap<>();
        for(Object[] row : commentLikeRepository.countByCommentIdIn(ids)) {
            likeCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        Set<Long> likedIds = currentUserId == null
                ? Set.of()
                : new HashSet<>(commentLikeRepository.findLikedCommentIds(currentUserId, ids));

        Map<Long, List<Comment>> repliesByParent = all.stream()
                .filter((comment) -> comment.getParentCommentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentCommentId));

        // Returned as top-level comments each carrying their replies (in
        // created-at order, preserved from the single fetch).
        return all.stream()
                .filter((comment) -> comment.getParentCommentId() == null)
                .map((comment) -> {
                    CommentDto dto = toDto(comment, likeCounts, likedIds);
                    dto.setReplies(repliesByParent.getOrDefault(comment.getId(), List.of())
                            .stream()
                            .map((reply) -> toDto(reply, likeCounts, likedIds))
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private CommentDto toDto(Comment comment, Map<Long, Long> likeCounts, Set<Long> likedIds) {
        CommentDto dto = modelMapper.map(comment, CommentDto.class);
        dto.setLikeCount(likeCounts.getOrDefault(comment.getId(), 0L));
        dto.setLikedByMe(likedIds.contains(comment.getId()));
        return dto;
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} deleting the comment with ID: {}", userId, commentId);

        Comment comment = commentRepository.findById(commentId).orElseThrow(()
                -> new ResourceNotFoundException("Comment not found with ID: "+commentId));

        // The comment's author can remove it, and so can the owner of the post
        // it sits under - otherwise you cannot moderate your own post.
        boolean isAuthor = comment.getUserId().equals(userId);
        boolean isPostOwner = postRepository.findById(comment.getPostId())
                .map((post) -> post.getUserId().equals(userId))
                .orElse(false);

        if(!isAuthor && !isPostOwner) {
            throw new BadRequestException("You cannot delete someone else's comment");
        }

        // Replies have no meaning without the comment they answer, and nothing
        // cascades on its own since parentCommentId carries no FK. Their likes
        // go too, or they would linger pointing at rows that no longer exist.
        List<Long> replyIds = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId())
                .stream().map(Comment::getId).toList();
        if(!replyIds.isEmpty()) {
            commentLikeRepository.deleteByCommentIdIn(replyIds);
        }
        commentLikeRepository.deleteByCommentId(comment.getId());
        commentRepository.deleteByParentCommentId(comment.getId());
        commentRepository.delete(comment);
    }
}
