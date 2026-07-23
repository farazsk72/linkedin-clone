package com.codingshuttle.linkedInProject.userService.controller;

import com.codingshuttle.linkedInProject.userService.dto.ConversationDto;
import com.codingshuttle.linkedInProject.userService.dto.MessageDto;
import com.codingshuttle.linkedInProject.userService.dto.PageResponse;
import com.codingshuttle.linkedInProject.userService.dto.SendMessageRequestDto;
import com.codingshuttle.linkedInProject.userService.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<ConversationDto>> getConversations() {
        return ResponseEntity.ok(messageService.getConversations());
    }

    /** Cheap enough for the navbar to poll - a COUNT across all threads. */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(messageService.getUnreadCount());
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<PageResponse<MessageDto>> getThread(
            @PathVariable Long partnerId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        return ResponseEntity.ok(messageService.getThread(partnerId, page, size));
    }

    @PostMapping
    public ResponseEntity<MessageDto> send(@RequestBody SendMessageRequestDto dto) {
        return new ResponseEntity<>(messageService.send(dto.getRecipientId(), dto.getContent()),
                HttpStatus.CREATED);
    }
}
