package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String name, email;
    private String headline, about, avatarUrl, location, currentCompany;
}
