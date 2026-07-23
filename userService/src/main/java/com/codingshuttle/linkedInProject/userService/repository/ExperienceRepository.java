package com.codingshuttle.linkedInProject.userService.repository;

import com.codingshuttle.linkedInProject.userService.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    /**
     * Current roles first (null endDate), then most recently ended. Postgres
     * sorts NULLs first on DESC by default, which is exactly what is wanted
     * here, so no explicit NULLS clause is needed.
     */
    List<Experience> findByUserIdOrderByEndDateDescStartDateDesc(Long userId);
}
