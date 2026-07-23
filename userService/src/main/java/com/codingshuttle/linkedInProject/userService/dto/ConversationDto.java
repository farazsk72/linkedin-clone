package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** One row per thread: the other party, a preview, and my unread count. */
@Data
public class ConversationDto {
    private Long partnerUserId;
    private String partnerName;
    private String partnerAvatarUrl;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private boolean lastMessageMine;
    private long unreadCount;
}
