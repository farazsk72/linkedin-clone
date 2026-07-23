package com.codingshuttle.linkedInProject.userService.repository;

import com.codingshuttle.linkedInProject.userService.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {

    List<Education> findByUserIdOrderByEndYearDescStartYearDesc(Long userId);
}
