package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

@Data
public class SendMessageRequestDto {
    private Long recipientId;
    private String content;
}
