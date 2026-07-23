package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;

/** Mirrors ConnectionsService's status response: {"status":"CONNECTED"|...}. */
@Data
public class ConnectionStatusDto {
    private String status;
}
