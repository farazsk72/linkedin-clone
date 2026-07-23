package com.codingshuttle.linkedInProject.userService.repository;

import com.codingshuttle.linkedInProject.userService.entity.Endorsement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EndorsementRepository extends JpaRepository<Endorsement, Long> {

    long countBySkillId(Long skillId);

    boolean existsBySkillIdAndEndorserUserId(Long skillId, Long endorserUserId);

    void deleteBySkillIdAndEndorserUserId(Long skillId, Long endorserUserId);

    void deleteBySkillId(Long skillId);
}
