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
        name = "endorsements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"skillId", "endorserUserId"})
)
public class Endorsement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long skillId;

    @Column(nullable = false)
    private Long endorserUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
