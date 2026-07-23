package com.codingshuttle.linkedInProject.userService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_recipient", columnList = "recipientId,senderId"),
        @Index(name = "idx_message_sender", columnList = "senderId,recipientId")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = 4000)
    private String content;

    /** Read state belongs to the recipient - the sender's copy is always "read". */
    @Column(nullable = false)
    private boolean read = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
