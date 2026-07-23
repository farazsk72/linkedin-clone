package com.codingshuttle.linkedInProject.userService.service;

import com.codingshuttle.linkedInProject.userService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.userService.dto.SkillDto;
import com.codingshuttle.linkedInProject.userService.entity.Endorsement;
import com.codingshuttle.linkedInProject.userService.entity.Skill;
import com.codingshuttle.linkedInProject.userService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.userService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.userService.repository.EndorsementRepository;
import com.codingshuttle.linkedInProject.userService.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {

    private final SkillRepository skillRepository;
    private final EndorsementRepository endorsementRepository;

    /** Most-endorsed first, which is the ordering that carries information. */
    public List<SkillDto> getSkills(Long userId) {
        Long currentUserId = AuthContextHolder.getCurrentUserId();

        return skillRepository.findByUserIdOrderByNameAsc(userId)
                .stream()
                .map((skill) -> toDto(skill, currentUserId))
                .sorted(Comparator.comparingLong(SkillDto::getEndorsementCount).reversed()
                        .thenComparing(SkillDto::getName))
                .toList();
    }

    @Transactional
    public SkillDto addSkill(String name) {
        Long userId = AuthContextHolder.getCurrentUserId();

        if(name == null || name.isBlank()) {
            throw new BadRequestException("Skill name is required");
        }
        String trimmed = name.trim();

        // Adding a skill you already list is a no-op rather than an error, and
        // returns the existing row so its endorsements are not lost.
        Skill existing = skillRepository.findByUserIdAndNameIgnoreCase(userId, trimmed).orElse(null);
        if(existing != null) {
            return toDto(existing, userId);
        }

        Skill skill = new Skill();
        skill.setUserId(userId);
        skill.setName(trimmed);

        log.info("Adding skill '{}' for user with ID: {}", trimmed, userId);
        return toDto(skillRepository.save(skill), userId);
    }

    @Transactional
    public void deleteSkill(Long skillId) {
        Long userId = AuthContextHolder.getCurrentUserId();

        Skill skill = ownedSkill(skillId, userId);

        // Endorsements key off skillId with no FK, so nothing cascades.
        endorsementRepository.deleteBySkillId(skillId);
        skillRepository.delete(skill);
    }

    @Transactional
    public SkillDto endorse(Long skillId) {
        Long userId = AuthContextHolder.getCurrentUserId();

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: "+skillId));

        // Self-endorsement would make the count meaningless.
        if(skill.getUserId().equals(userId)) {
            throw new BadRequestException("You cannot endorse your own skill");
        }

        if(!endorsementRepository.existsBySkillIdAndEndorserUserId(skillId, userId)) {
            Endorsement endorsement = new Endorsement();
            endorsement.setSkillId(skillId);
            endorsement.setEndorserUserId(userId);
            endorsementRepository.save(endorsement);
            log.info("User with ID: {} endorsed skill with ID: {}", userId, skillId);
        }

        return toDto(skill, userId);
    }

    @Transactional
    public SkillDto withdrawEndorsement(Long skillId) {
        Long userId = AuthContextHolder.getCurrentUserId();

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: "+skillId));

        endorsementRepository.deleteBySkillIdAndEndorserUserId(skillId, userId);
        return toDto(skill, userId);
    }

    private Skill ownedSkill(Long skillId, Long userId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: "+skillId));

        // Not-found rather than forbidden, matching the other profile sections.
        if(!skill.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Skill not found with ID: "+skillId);
        }
        return skill;
    }

    private SkillDto toDto(Skill skill, Long currentUserId) {
        SkillDto dto = new SkillDto();
        dto.setId(skill.getId());
        dto.setUserId(skill.getUserId());
        dto.setName(skill.getName());
        dto.setEndorsementCount(endorsementRepository.countBySkillId(skill.getId()));
        dto.setEndorsedByMe(currentUserId != null
                && endorsementRepository.existsBySkillIdAndEndorserUserId(skill.getId(), currentUserId));
        return dto;
    }
}
