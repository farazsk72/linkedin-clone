package com.codingshuttle.linkedInProject.userService.service;

import com.codingshuttle.linkedInProject.userService.dto.AuthResponseDto;
import com.codingshuttle.linkedInProject.userService.dto.LoginRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.SignupRequestDto;
import com.codingshuttle.linkedInProject.userService.dto.UserDto;
import com.codingshuttle.linkedInProject.userService.entity.RefreshToken;
import com.codingshuttle.linkedInProject.userService.entity.User;
import com.codingshuttle.linkedInProject.userService.event.UserCreatedEvent;
import com.codingshuttle.linkedInProject.userService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.userService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.userService.repository.RefreshTokenRepository;
import com.codingshuttle.linkedInProject.userService.repository.UserRepository;
import com.codingshuttle.linkedInProject.userService.utils.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final KafkaTemplate<Long, UserCreatedEvent> userCreatedEventKafkaTemplate;

    public UserDto signUp(SignupRequestDto signupRequestDto) {
        log.info("Signup a user with email: {}", signupRequestDto.getEmail());

        boolean exists = userRepository.existsByEmail(signupRequestDto.getEmail());
        if(exists) {
            throw new BadRequestException("User already exists");
        }

        User user = modelMapper.map(signupRequestDto, User.class);
        user.setPassword(BCrypt.hash(signupRequestDto.getPassword()));

        user = userRepository.save(user);

        UserCreatedEvent userCreatedEvent = UserCreatedEvent.builder()
                .userId(user.getId())
                .name(user.getName())
                .build();

        userCreatedEventKafkaTemplate.send("user_created_topic", userCreatedEvent);

        return modelMapper.map(user, UserDto.class);
    }

    public AuthResponseDto login(LoginRequestDto loginRequestDto) {
        log.info("Login request for user with email: {}", loginRequestDto.getEmail());

        User user = userRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(() -> new BadRequestException(
                "Incorrect email or password"));

        boolean isPasswordMatch = BCrypt.match(loginRequestDto.getPassword(), user.getPassword());

        if(!isPasswordMatch) {
            throw new BadRequestException("Incorrect email or password");
        }

        return new AuthResponseDto(jwtService.generateAccessToken(user), issueRefreshToken(user.getId()));
    }

    /**
     * Exchanges a refresh token for a new pair, rotating the refresh token so a
     * captured one is single-use. A revoked or expired token is rejected with
     * the same generic message, to avoid confirming which it was.
     */
    @Transactional
    public AuthResponseDto refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if(stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid refresh token");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        log.info("Refreshing session for user with ID: {}", user.getId());
        return new AuthResponseDto(jwtService.generateAccessToken(user), issueRefreshToken(user.getId()));
    }

    /**
     * Revoking is deliberately idempotent and silent - logout should never fail
     * or leak whether the token was real.
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent((stored) -> {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            log.info("Logged out user with ID: {}", stored.getUserId());
        });
    }

    private String issueRefreshToken(Long userId) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setUserId(userId);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));

        return refreshTokenRepository.save(token).getToken();
    }
}
