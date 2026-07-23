package com.api.bedhcd.modules.identity.application.service;

import com.api.bedhcd.config.JwtUtil;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.identity.api.v1.dto.AuthResponse;
import com.api.bedhcd.modules.identity.api.v1.dto.ChangePasswordRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.LoginRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.UserResponse;
import com.api.bedhcd.modules.identity.domain.exception.IdentityException;
import com.api.bedhcd.modules.identity.domain.model.LoginHistory;
import com.api.bedhcd.modules.identity.domain.model.LoginMethod;
import com.api.bedhcd.modules.identity.domain.model.LoginStatus;
import com.api.bedhcd.modules.identity.domain.model.RefreshToken;
import com.api.bedhcd.modules.identity.domain.model.User;
import com.api.bedhcd.modules.identity.domain.repository.LoginHistoryRepository;
import com.api.bedhcd.modules.identity.domain.repository.RefreshTokenRepository;
import com.api.bedhcd.modules.identity.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IdentityApplicationService {

    private final UserRepository userRepository;
    private final ParticipantPort participantPort;
    private final MeetingPort meetingPort;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // Tìm user theo identifier (CCCD hoặc InvestorCode - logic này nên ở Domain
        // Service hoặc Repository)
        User user = userRepository.findByCccd(request.getIdentifier())
                .orElseGet(() -> userRepository.findByUsername(request.getIdentifier())
                        .orElseThrow(() -> IdentityException.invalidCredentials()));

        try {
            // Xác thực với Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
        } catch (org.springframework.security.authentication.DisabledException e) {
            saveLoginHistory(user, null, false, LoginMethod.PASSWORD, httpRequest, "Account is disabled");
            throw IdentityException.unauthorized("Tài khoản đã bị vô hiệu hóa");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            saveLoginHistory(user, null, false, LoginMethod.PASSWORD, httpRequest, "Invalid password");
            throw IdentityException.invalidCredentials();
        } catch (Exception e) {
            saveLoginHistory(user, null, false, LoginMethod.PASSWORD, httpRequest, e.getMessage());
            throw IdentityException.invalidCredentials();
        }

        // Tạo token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Lưu refresh token
        saveRefreshToken(user, refreshToken);

        // Lưu lịch sử đăng nhập
        saveLoginHistory(user, refreshToken, true, LoginMethod.PASSWORD, httpRequest, null);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getUsername()) // Tạm thời dùng username làm email nếu chưa có email field
                .fullName(user.getFullName())
                .roles(user.getRoles())
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> IdentityException.invalidRefreshToken());

        if (storedToken.isExpired()) {
            refreshTokenRepository.deleteByToken(refreshTokenStr);
            throw IdentityException.invalidRefreshToken();
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> IdentityException.userNotFound(storedToken.getUserId()));

        // Tạo access token mới
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenStr)
                .userId(user.getId())
                .fullName(user.getFullName())
                .roles(user.getRoles())
                .build();
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.deleteByToken(refreshTokenStr);
        loginHistoryRepository.findBySessionToken(refreshTokenStr).ifPresent(history -> {
            history.setLogoutTime(LocalDateTime.now());
            loginHistoryRepository.save(history);
        });
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private void saveLoginHistory(User user, String token, boolean success, LoginMethod method,
            HttpServletRequest request, String failureReason) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        LoginHistory history = LoginHistory.builder()
                .userId(user.getId())
                .loginTime(LocalDateTime.now())
                .status(success ? LoginStatus.SUCCESS : LoginStatus.FAILED)
                .sessionToken(token)
                .loginMethod(method)
                .ipAddress(ipAddress)
                .userAgent(request.getHeader("User-Agent"))
                .failureReason(failureReason)
                .build();
        loginHistoryRepository.save(history);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> IdentityException.userNotFound(username));

        // Nếu là cổ đông (không phải admin), kiểm tra cấu hình cuộc họp
        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.name().startsWith("ROLE_ADMIN"));
        if (!isAdmin) {
            String meetingId = participantPort.getLastMeetingId(user.getId());
            if (meetingId != null && !meetingPort.shareholderCanEditAccount(meetingId)) {
                throw IdentityException
                        .invalidState("Cấu hình cuộc họp hiện tại không cho phép cổ đông thay đổi mật khẩu.");
            }
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw IdentityException.invalidCredentials();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> IdentityException.userNotFound(userId));
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        String meetingId = participantPort.getLastMeetingId(user.getId());
        long attendingShares = 0L;
        long receivedProxyShares = 0L;
        long delegatedShares = 0L;
        LocalDateTime checkedInAt = null;

        if (meetingId != null) {
            attendingShares = participantPort.getAttendingShares(meetingId, user.getId());
            receivedProxyShares = participantPort.getReceivedProxyShares(meetingId, user.getId());
            delegatedShares = participantPort.getDelegatedShares(meetingId, user.getId());
            checkedInAt = participantPort.getCheckedInAt(meetingId, user.getId());
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .meetingId(meetingId)
                .cccd(user.getCccd())
                .investorCode(user.getInvestorCode())
                .sharesOwned(user.getSharesOwned())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .attendingShares(attendingShares)
                .receivedProxyShares(receivedProxyShares)
                .delegatedShares(delegatedShares)
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .checkedInAt(checkedInAt)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
