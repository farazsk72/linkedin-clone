package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

@Data
public class SkillDto {
    private Long id;
    private Long userId;
    private String name;
    private long endorsementCount;
    private boolean endorsedByMe;
}
