package com.codingshuttle.linkedInProject.userService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "experiences")
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    private String location;

    private LocalDate startDate;

    /** Null means "current role" - that is the whole point of the field. */
    private LocalDate endDate;

    @Column(length = 2000)
    private String description;
}
