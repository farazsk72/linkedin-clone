package com.codingshuttle.linkedInProject.userService.repository;

import com.codingshuttle.linkedInProject.userService.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByUserIdOrderByNameAsc(Long userId);

    Optional<Skill> findByUserIdAndNameIgnoreCase(Long userId, String name);
}
