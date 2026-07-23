package com.codingshuttle.linkedInProject.ConnectionsService.controller;

import com.codingshuttle.linkedInProject.ConnectionsService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.ConnectionsService.dto.ConnectionStatusDto;
import com.codingshuttle.linkedInProject.ConnectionsService.dto.ConnectionSuggestionDto;
import com.codingshuttle.linkedInProject.ConnectionsService.entity.Person;
import com.codingshuttle.linkedInProject.ConnectionsService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.ConnectionsService.service.ConnectionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
@Slf4j
public class ConnectionsController {

    private final ConnectionsService connectionsService;

    @GetMapping("/requests")
    public ResponseEntity<List<Person>> getPendingRequests() {
        return ResponseEntity.ok(connectionsService.getPendingRequests());
    }

    // Scoped to the caller's own token - suggestions are personal, so there is
    // deliberately no way to ask for someone else's.
    @GetMapping("/suggestions")
    public ResponseEntity<List<ConnectionSuggestionDto>> getConnectionSuggestions(
            @RequestParam(value = "limit", defaultValue = "10") long limit) {
        return ResponseEntity.ok(connectionsService.getConnectionSuggestions(limit));
    }

    // Following is one-directional and needs no acceptance, unlike a connection.
    @PostMapping("/follow/{userId}")
    public ResponseEntity<Void> follow(@PathVariable Long userId) {
        connectionsService.follow(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/follow/{userId}")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId) {
        connectionsService.unfollow(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<List<Person>> getFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(connectionsService.getFollowing(userId));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<Person>> getFollowers(@PathVariable Long userId) {
        return ResponseEntity.ok(connectionsService.getFollowers(userId));
    }

    /** Just the ids, for the posts service feed fan-out. */
    @GetMapping("/following-ids")
    public ResponseEntity<List<Long>> getFollowingIds() {
        return ResponseEntity.ok(connectionsService.getFollowingIds());
    }

    /** Relationship between the caller and someone else, from the caller's side. */
    @GetMapping("/status/{userId}")
    public ResponseEntity<ConnectionStatusDto> getConnectionStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(connectionsService.getConnectionStatus(userId));
    }

    // Locked to the caller's own id. Previously any authenticated user could
    // read anyone's connection list by passing a different userId; the internal
    // callers (feed fan-out, notifications) always pass their own id, and the
    // visibility check now uses /status instead of pulling a whole list.
    @GetMapping("/{userId}/first-degree")
    public ResponseEntity<List<Person>> getFirstDegreeConnections(@PathVariable Long userId) {
        Long callerId = AuthContextHolder.getCurrentUserId();
        if(!callerId.equals(userId)) {
            throw new BadRequestException("You can only view your own connections");
        }
        return ResponseEntity.ok(connectionsService.getFirstDegreeConnectionsOfUser(userId));
    }

    @PostMapping("/request/{userId}")
    public ResponseEntity<Void> sendConnectionRequest(@PathVariable Long userId) {
        connectionsService.sendConnectionRequest(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accept/{userId}")
    public ResponseEntity<Void> acceptConnectionRequest(@PathVariable Long userId) {
        connectionsService.acceptConnectionRequest(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reject/{userId}")
    public ResponseEntity<Void> rejectConnectionRequest(@PathVariable Long userId) {
        connectionsService.rejectConnectionRequest(userId);
        return ResponseEntity.noContent().build();
    }

    /** Take back an invitation you sent, before it is answered. */
    @DeleteMapping("/request/{userId}")
    public ResponseEntity<Void> withdrawConnectionRequest(@PathVariable Long userId) {
        connectionsService.withdrawConnectionRequest(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeConnection(@PathVariable Long userId) {
        connectionsService.removeConnection(userId);
        return ResponseEntity.noContent().build();
    }
}
