package com.codingshuttle.linkedInProject.userService.controller;

import com.codingshuttle.linkedInProject.userService.dto.AuthResponseDto;
import com.codingshuttle.linkedInProject.userService.dto.LoginRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.RefreshRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.SignupRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.UserDto;
import com.codingshuttle.linkedInProject.userService.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signUp(@RequestBody SignupRequestDto signupRequestDto) {
        UserDto userDto = authService.signUp(signupRequestDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    /** Returns a short-lived access token plus a revocable refresh token. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    /** Rotates the refresh token, so a captured one is single-use. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestBody RefreshRequestDto dto) {
        return ResponseEntity.ok(authService.refresh(dto.getRefreshToken()));
    }

    // Under /auth on purpose: logging out has to work once the access token has
    // already expired, which is exactly when a user is likely to press it.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequestDto dto) {
        authService.logout(dto.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
