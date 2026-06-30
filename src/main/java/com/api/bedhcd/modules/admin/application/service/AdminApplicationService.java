package com.api.bedhcd.modules.admin.application.service;

import com.api.bedhcd.config.JwtUtil;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.modules.identity.api.v1.dto.AuthResponse;
import com.api.bedhcd.modules.identity.api.v1.dto.LoginRequest;
import com.api.bedhcd.modules.identity.domain.exception.IdentityException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminApplicationService {

    private final AdminJpaRepository adminJpaRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AdminEntity admin = adminJpaRepository.findByUsername(request.getIdentifier())
                .orElseThrow(IdentityException::invalidCredentials);

        if (!admin.isActive()) {
            throw IdentityException.unauthorized("Tài khoản quản trị đã bị khóa.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(admin.getUsername(), request.getPassword()));
        } catch (Exception e) {
            throw IdentityException.invalidCredentials();
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(admin.getUsername());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(admin.getId())
                .email(admin.getEmail())
                .roles(Set.of(admin.getRole()))
                .build();
    }
}
