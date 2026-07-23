package com.codingshuttle.linkedInProject.userService.controller;

import com.codingshuttle.linkedInProject.userService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.userService.dto.ChangePasswordRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.EducationDto;
import com.codingshuttle.linkedInProject.userService.dto.ExperienceDto;
import com.codingshuttle.linkedInProject.userService.dto.PageResponse;
import com.codingshuttle.linkedInProject.userService.dto.UpdateProfileRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.UserDto;
import com.codingshuttle.linkedInProject.userService.dto.ProfileViewerDto;
import com.codingshuttle.linkedInProject.userService.dto.SkillDto;
import com.codingshuttle.linkedInProject.userService.service.BlockService;
import com.codingshuttle.linkedInProject.userService.service.SkillService;
import com.codingshuttle.linkedInProject.userService.service.ProfileSectionService;
import com.codingshuttle.linkedInProject.userService.service.ProfileViewService;
import com.codingshuttle.linkedInProject.userService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final ProfileSectionService profileSectionService;
    private final ProfileViewService profileViewService;
    private final SkillService skillService;
    private final BlockService blockService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Long userId = AuthContextHolder.getCurrentUserId();
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    // Deliberately scoped to the caller's own id from the token rather than a
    // path variable - there is no way to address someone else's profile here.
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(@RequestBody UpdateProfileRequestDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, dto));
    }

    // Lives under /profile rather than /auth because it must be authenticated -
    // the gateway leaves /auth/** open for signup and login.
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequestDto dto) {
        userService.changePassword(dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<UserDto>> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.searchUsers(query, page, size));
    }

    // Experience and education. Reads take a userId because any profile can be
    // viewed; writes take none, because they always act on the caller.
    @GetMapping("/{userId}/experience")
    public ResponseEntity<List<ExperienceDto>> getExperience(@PathVariable Long userId) {
        return ResponseEntity.ok(profileSectionService.getExperience(userId));
    }

    @PostMapping("/experience")
    public ResponseEntity<ExperienceDto> addExperience(@RequestBody ExperienceDto dto) {
        return new ResponseEntity<>(profileSectionService.addExperience(dto), HttpStatus.CREATED);
    }

    @PutMapping("/experience/{experienceId}")
    public ResponseEntity<ExperienceDto> updateExperience(@PathVariable Long experienceId,
                                                          @RequestBody ExperienceDto dto) {
        return ResponseEntity.ok(profileSectionService.updateExperience(experienceId, dto));
    }

    @DeleteMapping("/experience/{experienceId}")
    public ResponseEntity<Void> deleteExperience(@PathVariable Long experienceId) {
        profileSectionService.deleteExperience(experienceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/education")
    public ResponseEntity<List<EducationDto>> getEducation(@PathVariable Long userId) {
        return ResponseEntity.ok(profileSectionService.getEducation(userId));
    }

    @PostMapping("/education")
    public ResponseEntity<EducationDto> addEducation(@RequestBody EducationDto dto) {
        return new ResponseEntity<>(profileSectionService.addEducation(dto), HttpStatus.CREATED);
    }

    @PutMapping("/education/{educationId}")
    public ResponseEntity<EducationDto> updateEducation(@PathVariable Long educationId,
                                                        @RequestBody EducationDto dto) {
        return ResponseEntity.ok(profileSectionService.updateEducation(educationId, dto));
    }

    @DeleteMapping("/education/{educationId}")
    public ResponseEntity<Void> deleteEducation(@PathVariable Long educationId) {
        profileSectionService.deleteEducation(educationId);
        return ResponseEntity.noContent().build();
    }

    // Skills. Reads take a userId; adding and removing always act on the
    // caller, and endorsing is keyed by skill id since it targets someone else.
    @GetMapping("/{userId}/skills")
    public ResponseEntity<List<SkillDto>> getSkills(@PathVariable Long userId) {
        return ResponseEntity.ok(skillService.getSkills(userId));
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillDto> addSkill(@RequestBody SkillDto dto) {
        return new ResponseEntity<>(skillService.addSkill(dto.getName()), HttpStatus.CREATED);
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long skillId) {
        skillService.deleteSkill(skillId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/skills/{skillId}/endorse")
    public ResponseEntity<SkillDto> endorse(@PathVariable Long skillId) {
        return ResponseEntity.ok(skillService.endorse(skillId));
    }

    @DeleteMapping("/skills/{skillId}/endorse")
    public ResponseEntity<SkillDto> withdrawEndorsement(@PathVariable Long skillId) {
        return ResponseEntity.ok(skillService.withdrawEndorsement(skillId));
    }

    // Blocking. Lives under /profile so it rides the existing gateway route.
    @PostMapping("/block/{userId}")
    public ResponseEntity<Void> block(@PathVariable Long userId) {
        blockService.block(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/block/{userId}")
    public ResponseEntity<Void> unblock(@PathVariable Long userId) {
        blockService.unblock(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<UserDto>> getBlockedUsers() {
        return ResponseEntity.ok(blockService.getBlockedUsers());
    }

    @GetMapping("/viewers")
    public ResponseEntity<PageResponse<ProfileViewerDto>> getMyViewers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(profileViewService.getMyViewers(page, size));
    }

    @GetMapping("/viewers/count")
    public ResponseEntity<Long> getRecentViewerCount() {
        return ResponseEntity.ok(profileViewService.getRecentViewerCount());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        UserDto user = userService.getUserById(userId);
        // Recorded as a side effect of the lookup, so every path that renders a
        // profile counts without each caller having to remember to log it.
        profileViewService.record(userId, AuthContextHolder.getCurrentUserId());
        return ResponseEntity.ok(user);
    }
}
