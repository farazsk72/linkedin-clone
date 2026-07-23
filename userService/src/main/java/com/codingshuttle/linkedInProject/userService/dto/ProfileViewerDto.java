package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfileViewerDto {
    private Long userId;
    private String name;
    private String headline;
    private String avatarUrl;
    private LocalDateTime lastViewedAt;
}
