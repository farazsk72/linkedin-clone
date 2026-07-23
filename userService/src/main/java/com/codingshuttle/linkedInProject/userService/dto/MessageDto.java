package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDto {
    private Long id;
    private Long senderId;
    private Long recipientId;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;
}
