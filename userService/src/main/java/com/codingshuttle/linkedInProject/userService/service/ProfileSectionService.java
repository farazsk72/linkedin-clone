package com.codingshuttle.linkedInProject.userService.service;

import com.codingshuttle.linkedInProject.userService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.userService.dto.EducationDto;
import com.codingshuttle.linkedInProject.userService.dto.ExperienceDto;
import com.codingshuttle.linkedInProject.userService.entity.Education;
import com.codingshuttle.linkedInProject.userService.entity.Experience;
import com.codingshuttle.linkedInProject.userService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.userService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.userService.repository.EducationRepository;
import com.codingshuttle.linkedInProject.userService.repository.ExperienceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The repeating parts of a profile - roles held and schools attended. Reads are
 * public (any authenticated user can view a profile); writes always target the
 * caller's own id, never a path variable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileSectionService {

    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final ModelMapper modelMapper;

    public List<ExperienceDto> getExperience(Long userId) {
        return experienceRepository.findByUserIdOrderByEndDateDescStartDateDesc(userId)
                .stream()
                .map((element) -> modelMapper.map(element, ExperienceDto.class))
                .toList();
    }

    @Transactional
    public ExperienceDto addExperience(ExperienceDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Adding experience for user with ID: {}", userId);

        if(isBlank(dto.getTitle()) || isBlank(dto.getCompany())) {
            throw new BadRequestException("Title and company are required");
        }
        if(dto.getStartDate() != null && dto.getEndDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("End date cannot be before the start date");
        }

        Experience experience = modelMapper.map(dto, Experience.class);
        experience.setId(null); // a client-supplied id must never overwrite a row
        experience.setUserId(userId);

        return modelMapper.map(experienceRepository.save(experience), ExperienceDto.class);
    }

    @Transactional
    public ExperienceDto updateExperience(Long experienceId, ExperienceDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Updating experience with ID: {} for user with ID: {}", experienceId, userId);

        Experience experience = ownedExperience(experienceId, userId);

        if(isBlank(dto.getTitle()) || isBlank(dto.getCompany())) {
            throw new BadRequestException("Title and company are required");
        }
        if(dto.getStartDate() != null && dto.getEndDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("End date cannot be before the start date");
        }

        experience.setTitle(dto.getTitle().trim());
        experience.setCompany(dto.getCompany().trim());
        experience.setLocation(dto.getLocation());
        experience.setStartDate(dto.getStartDate());
        experience.setEndDate(dto.getEndDate());
        experience.setDescription(dto.getDescription());

        return modelMapper.map(experienceRepository.save(experience), ExperienceDto.class);
    }

    @Transactional
    public void deleteExperience(Long experienceId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Deleting experience with ID: {} for user with ID: {}", experienceId, userId);

        experienceRepository.delete(ownedExperience(experienceId, userId));
    }

    /**
     * Loads a row only if it belongs to the caller. Missing and not-yours are
     * both reported as not-found, so this cannot be used to probe for the
     * existence of other people's rows.
     */
    private Experience ownedExperience(Long experienceId, Long userId) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found with ID: "+experienceId));

        if(!experience.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Experience not found with ID: "+experienceId);
        }
        return experience;
    }

    public List<EducationDto> getEducation(Long userId) {
        return educationRepository.findByUserIdOrderByEndYearDescStartYearDesc(userId)
                .stream()
                .map((element) -> modelMapper.map(element, EducationDto.class))
                .toList();
    }

    @Transactional
    public EducationDto addEducation(EducationDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Adding education for user with ID: {}", userId);

        if(isBlank(dto.getSchool())) {
            throw new BadRequestException("School is required");
        }
        if(dto.getStartYear() != null && dto.getEndYear() != null
                && dto.getEndYear() < dto.getStartYear()) {
            throw new BadRequestException("End year cannot be before the start year");
        }

        Education education = modelMapper.map(dto, Education.class);
        education.setId(null);
        education.setUserId(userId);

        return modelMapper.map(educationRepository.save(education), EducationDto.class);
    }

    @Transactional
    public EducationDto updateEducation(Long educationId, EducationDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Updating education with ID: {} for user with ID: {}", educationId, userId);

        Education education = ownedEducation(educationId, userId);

        if(isBlank(dto.getSchool())) {
            throw new BadRequestException("School is required");
        }
        if(dto.getStartYear() != null && dto.getEndYear() != null
                && dto.getEndYear() < dto.getStartYear()) {
            throw new BadRequestException("End year cannot be before the start year");
        }

        education.setSchool(dto.getSchool().trim());
        education.setDegree(dto.getDegree());
        education.setFieldOfStudy(dto.getFieldOfStudy());
        education.setStartYear(dto.getStartYear());
        education.setEndYear(dto.getEndYear());

        return modelMapper.map(educationRepository.save(education), EducationDto.class);
    }

    @Transactional
    public void deleteEducation(Long educationId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Deleting education with ID: {} for user with ID: {}", educationId, userId);

        educationRepository.delete(ownedEducation(educationId, userId));
    }

    private Education ownedEducation(Long educationId, Long userId) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new ResourceNotFoundException("Education not found with ID: "+educationId));

        if(!education.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Education not found with ID: "+educationId);
        }
        return education;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
