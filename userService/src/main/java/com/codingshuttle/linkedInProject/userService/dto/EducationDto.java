package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

@Data
public class EducationDto {
    private Long id;
    private Long userId;
    private String school;
    private String degree;
    private String fieldOfStudy;
    private Integer startYear;
    private Integer endYear;
}
