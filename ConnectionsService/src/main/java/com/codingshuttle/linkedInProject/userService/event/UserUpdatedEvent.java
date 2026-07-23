package com.codingshuttle.linkedInProject.userService.event;

import lombok.Data;

/**
 * Consumer-side copy. Deliberately @Data only, with no @Builder - @Builder
 * generates an all-args constructor which suppresses the no-arg one, and
 * Jackson cannot instantiate the event without it.
 */
@Data
public class UserUpdatedEvent {
    private Long userId;
    private String name;
}
