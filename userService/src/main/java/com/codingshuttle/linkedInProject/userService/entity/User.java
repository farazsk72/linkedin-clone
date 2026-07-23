package com.codingshuttle.linkedInProject.userService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // Profile fields. All optional - existing accounts and fresh signups both
    // start with them null.
    private String headline;

    @Column(length = 2000)
    private String about;

    private String avatarUrl;

    private String location;

    private String currentCompany;
}
