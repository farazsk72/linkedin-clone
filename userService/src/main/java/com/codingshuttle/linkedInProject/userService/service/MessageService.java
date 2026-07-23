package com.codingshuttle.linkedInProject.userService.service;

import com.codingshuttle.linkedInProject.userService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.userService.dto.ConversationDto;
import com.codingshuttle.linkedInProject.userService.dto.MessageDto;
import com.codingshuttle.linkedInProject.userService.dto.PageResponse;
import com.codingshuttle.linkedInProject.userService.entity.Message;
import com.codingshuttle.linkedInProject.userService.entity.User;
import com.codingshuttle.linkedInProject.userService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.userService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.userService.repository.BlockRepository;
import com.codingshuttle.linkedInProject.userService.repository.MessageRepository;
import com.codingshuttle.linkedInProject.userService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public MessageDto send(Long recipientId, String content) {
        Long userId = AuthContextHolder.getCurrentUserId();

        if(userId.equals(recipientId)) {
            throw new BadRequestException("You cannot message yourself");
        }
        if(content == null || content.isBlank()) {
            throw new BadRequestException("Message cannot be empty");
        }
        userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("No such user"));

        // A block in either direction stops the message. Reported as a generic
        // failure rather than "you are blocked", which would leak the block.
        if(blockRepository.blockExistsBetween(userId, recipientId)) {
            throw new BadRequestException("You cannot message this user");
        }

        Message message = new Message();
        message.setSenderId(userId);
        message.setRecipientId(recipientId);
        message.setContent(content.trim());
        message = messageRepository.save(message);

        log.info("User with ID: {} messaged user with ID: {}", userId, recipientId);
        return modelMapper.map(message, MessageDto.class);
    }

    /** Newest-first page of one thread. Opening a thread marks it read. */
    @Transactional
    public PageResponse<MessageDto> getThread(Long partnerId, int page, int size) {
        Long userId = AuthContextHolder.getCurrentUserId();

        messageRepository.markThreadRead(userId, partnerId);

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return PageResponse.from(messageRepository.findThread(userId, partnerId, pageRequest),
                (message) -> modelMapper.map(message, MessageDto.class));
    }

    public List<ConversationDto> getConversations() {
        Long userId = AuthContextHolder.getCurrentUserId();

        return messageRepository.findPartnerIds(userId).stream()
                .map((partnerId) -> toConversation(userId, partnerId))
                .sorted(Comparator.comparing(ConversationDto::getLastMessageAt).reversed())
                .toList();
    }

    public long getUnreadCount() {
        Long userId = AuthContextHolder.getCurrentUserId();
        return messageRepository.countByRecipientIdAndReadFalse(userId);
    }

    private ConversationDto toConversation(Long userId, Long partnerId) {
        Message last = messageRepository.findLatestBetween(userId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Empty conversation"));

        ConversationDto dto = new ConversationDto();
        dto.setPartnerUserId(partnerId);
        dto.setLastMessage(last.getContent());
        dto.setLastMessageAt(last.getCreatedAt());
        dto.setLastMessageMine(last.getSenderId().equals(userId));
        dto.setUnreadCount(messageRepository.countByRecipientIdAndSenderIdAndReadFalse(userId, partnerId));

        User partner = userRepository.findById(partnerId).orElse(null);
        if(partner != null) {
            dto.setPartnerName(partner.getName());
            dto.setPartnerAvatarUrl(partner.getAvatarUrl());
        }
        return dto;
    }
}
