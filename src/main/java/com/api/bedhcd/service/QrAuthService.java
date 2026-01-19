package com.api.bedhcd.service;

import com.api.bedhcd.config.JwtUtil;
import com.api.bedhcd.dto.AuthResponse;
import com.api.bedhcd.dto.request.MagicLoginRequest;
import com.api.bedhcd.entity.MagicLinkToken;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.MagicLinkTokenRepository;
import com.api.bedhcd.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrAuthService {

    private final UserRepository userRepository;
    private final MagicLinkTokenRepository magicLinkTokenRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private static final long DEFAULT_EXPIRATION_HOURS = 24;

    @Transactional
    public String generateMagicToken(String userId, LocalDateTime expiresAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(DEFAULT_EXPIRATION_HOURS);
        }

        String token = UUID.randomUUID().toString();

        MagicLinkToken magicLinkToken = MagicLinkToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .isActive(true)
                .build();

        magicLinkTokenRepository.save(magicLinkToken);
        return token;
    }

    @Transactional(readOnly = true)
    public User validateMagicToken(String token) {
        MagicLinkToken magicLinkToken = magicLinkTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired QR token"));

        if (!magicLinkToken.getIsActive()) {
            throw new ResourceNotFoundException("QR token has been revoked");
        }

        if (LocalDateTime.now().isAfter(magicLinkToken.getExpiresAt())) {
            throw new ResourceNotFoundException("QR token has expired");
        }

        return magicLinkToken.getUser();
    }

    @Transactional(readOnly = true)
    public String getLatestMagicToken(String userId) {
        return magicLinkTokenRepository
                .findByUser_IdAndIsActiveTrueAndExpiresAtAfterOrderByExpiresAtDesc(userId, LocalDateTime.now())
                .stream()
                .findFirst()
                .map(MagicLinkToken::getToken)
                .orElse(null);
    }

    @Transactional
    public AuthResponse magicLogin(MagicLoginRequest request) {
        User user = validateMagicToken(request.getToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getCccd());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        String accessToken = jwtUtil.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }
}
