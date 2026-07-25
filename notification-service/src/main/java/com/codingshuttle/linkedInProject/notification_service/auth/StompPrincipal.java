package com.codingshuttle.linkedInProject.notification_service.auth;

import java.security.Principal;

/**
 * The authenticated identity attached to a STOMP session. Its name is the user
 * id as a string, which is what {@code convertAndSendToUser(userId, ...)} matches
 * a session against when routing a message to a specific user's queue.
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
