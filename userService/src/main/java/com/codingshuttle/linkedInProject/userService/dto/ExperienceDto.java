package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExperienceDto {
    private Long id;
    private Long userId;
    private String title;
    private String company;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
