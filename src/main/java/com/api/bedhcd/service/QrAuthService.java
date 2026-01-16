package com.api.bedhcd.service;

import com.api.bedhcd.entity.MagicLinkToken;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.MagicLinkTokenRepository;
import com.api.bedhcd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrAuthService {

    private final UserRepository userRepository;
    private final MagicLinkTokenRepository magicLinkTokenRepository;

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
}
