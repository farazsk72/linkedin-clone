package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;

/**
 * Every field is optional. A null means "leave this alone", which lets the
 * frontend PATCH-style a single field without having to resend the whole
 * profile. Clearing a field is done by sending an empty string.
 */
@Data
public class UpdateProfileRequestDto {
    private String name;
    private String headline;
    private String about;
    private String avatarUrl;
    private String location;
    private String currentCompany;
}
