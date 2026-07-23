package com.codingshuttle.linkedInProject.userService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "blocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blockerUserId", "blockedUserId"})
)
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long blockerUserId;

    @Column(nullable = false)
    private Long blockedUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
