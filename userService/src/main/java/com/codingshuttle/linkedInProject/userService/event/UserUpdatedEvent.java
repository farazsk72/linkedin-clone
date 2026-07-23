package com.codingshuttle.linkedInProject.userService.event;

import lombok.Builder;
import lombok.Data;

/**
 * Published when a profile edit changes the display name. ConnectionsService
 * copies the name onto its Neo4j Person nodes, so without this the network and
 * search results keep showing the name the user had at signup.
 */
@Data
@Builder
public class UserUpdatedEvent {
    private Long userId;
    private String name;
}
