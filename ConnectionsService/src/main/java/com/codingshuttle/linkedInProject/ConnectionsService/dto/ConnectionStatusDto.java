package com.codingshuttle.linkedInProject.ConnectionsService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Lets a client render the right button without guessing. The two PENDING
 * cases are distinct because they need opposite actions: withdraw yours, or
 * accept/reject theirs.
 */
@Data
@AllArgsConstructor
public class ConnectionStatusDto {

    public enum Status {
        SELF,
        CONNECTED,
        PENDING_OUTGOING,
        PENDING_INCOMING,
        NOT_CONNECTED
    }

    private Status status;
}
