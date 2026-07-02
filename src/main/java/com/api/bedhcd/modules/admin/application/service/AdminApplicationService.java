package com.api.bedhcd.modules.admin.application.service;

import com.api.bedhcd.config.JwtUtil;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.modules.identity.api.v1.dto.AuthResponse;
import com.api.bedhcd.modules.identity.api.v1.dto.ChangePasswordRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.LoginRequest;
import com.api.bedhcd.modules.identity.domain.exception.IdentityException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminApplicationService {

    private final AdminJpaRepository adminJpaRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final com.api.bedhcd.modules.audit.application.service.AuditLogApplicationService auditLogApplicationService;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
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

        // Record admin login audit log
        String ipAddress = getClientIp(httpRequest);
        String userAgentHeader = httpRequest.getHeader("User-Agent");
        String browserOs = parseUserAgent(userAgentHeader);
        String payload = String.format("Admin đăng nhập thành công. IP: %s, Thiết bị/Trình duyệt: %s", ipAddress,
                browserOs);

        auditLogApplicationService.logActionAsync(
                admin.getUsername(),
                "ADMIN_LOGIN",
                "AUTH",
                admin.getId(),
                payload,
                ipAddress);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .roles(Set.of(admin.getRole()))
                .build();
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }

    private String parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Không xác định";
        }

        String browser = "Trình duyệt khác";
        String os = "Hệ điều hành khác";

        if (userAgent.contains("Edg") || userAgent.contains("Edge")) {
            browser = "Edge";
        } else if (userAgent.contains("Chrome")) {
            browser = "Chrome";
        } else if (userAgent.contains("Firefox")) {
            browser = "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            browser = "Safari";
        } else if (userAgent.contains("Opera") || userAgent.contains("OPR")) {
            browser = "Opera";
        }

        if (userAgent.contains("Windows")) {
            os = "Windows";
        } else if (userAgent.contains("Mac OS X")) {
            os = "MacOS";
        } else if (userAgent.contains("Linux")) {
            os = "Linux";
        } else if (userAgent.contains("Android")) {
            os = "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            os = "iOS";
        }

        return browser + " trên " + os;
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        AdminEntity admin = adminJpaRepository.findByUsername(username)
                .orElseThrow(() -> IdentityException.userNotFound(username));

        if (!passwordEncoder.matches(request.getOldPassword(), admin.getPassword())) {
            throw IdentityException.invalidCredentials();
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminJpaRepository.save(admin);

        auditLogApplicationService.logActionAsync(
                admin.getUsername(),
                "CHANGE_PASSWORD",
                "AUTH",
                admin.getId(),
                "Admin đã thay đổi mật khẩu thành công.",
                null);
    }
}
