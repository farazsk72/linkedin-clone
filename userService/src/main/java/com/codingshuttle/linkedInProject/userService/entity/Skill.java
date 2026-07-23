package com.codingshuttle.linkedInProject.userService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
        name = "skills",
        // One entry per skill per person - listing "Java" twice is a data bug,
        // not a stronger claim to knowing Java.
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "name"})
)
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;
}
