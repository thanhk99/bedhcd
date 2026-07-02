package com.api.bedhcd.modules.identity.application.service;

import com.api.bedhcd.config.JwtUtil;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.identity.api.v1.dto.AuthResponse;
import com.api.bedhcd.modules.identity.api.v1.dto.ChangePasswordRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.CreateAdminRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.LoginRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.UpdateUserRequest;
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
import com.api.bedhcd.shared.domain.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.api.bedhcd.shared.domain.UuidFactory;
import java.util.stream.Collectors;

import com.api.bedhcd.shared.dto.PageResponse;

@Service
@RequiredArgsConstructor
public class IdentityApplicationService {

    private final UserRepository userRepository;
    private final ParticipantPort participantPort;
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

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String keyword) {
        return userRepository.searchTop10ByKeyword(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(int page, int size) {
        return getUsers(page, size, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(int page, int size, String keyword) {
        List<UserResponse> allUsers;
        if (keyword != null && !keyword.isBlank()) {
            allUsers = userRepository.searchByKeyword(keyword).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } else {
            allUsers = userRepository.findAll(page, size).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        long total = (keyword != null && !keyword.isBlank()) ? allUsers.size() : userRepository.count();
        int fromIndex = (keyword != null && !keyword.isBlank()) ? Math.min(page * size, allUsers.size()) : 0;
        int toIndex = (keyword != null && !keyword.isBlank()) ? Math.min(fromIndex + size, allUsers.size())
                : allUsers.size();
        List<UserResponse> pageItems = (keyword != null && !keyword.isBlank())
                ? allUsers.subList(fromIndex, toIndex)
                : allUsers;
        return com.api.bedhcd.shared.dto.PageResponse.of(pageItems, total, page, size);
    }

    @Transactional
    public UserResponse updateRoles(String userId, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> IdentityException.userNotFound(userId));

        // Lấy tập Role của người đang thực hiện thao tác từ Security Context
        Set<Role> assignerRoles = getCurrentUserRoles();

        // Xóa toàn bộ roles hiện tại và cấp lại từng role
        // (với sự kiểm duyệt của Domain Model)
        user.setRoles(new HashSet<>());
        for (Role role : roles) {
            user.assignRole(role, assignerRoles);
        }
        return mapToResponse(userRepository.save(user));
    }

    /**
     * Tạo tài khoản Sub-Admin (ADMIN): Chỉ SUPER_ADMIN mới được phép gọi.
     * Nghị vụ kiểm tra thẩm quyền được ủy thác cho Domain Model (User.assignRole).
     */
    @Transactional
    public UserResponse createSubAdmin(CreateAdminRequest request) {
        // Kiểm tra username tồn tại
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw IdentityException.usernameAlreadyExists(request.getUsername());
        }

        // Lấy tập Role của người đang thực hiện thao tác
        Set<Role> assignerRoles = getCurrentUserRoles();

        // Tạo User mới với trạng thái rỗng
        User newAdmin = User.builder()
                .id(UuidFactory.generate())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .roles(new java.util.HashSet<>())
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Ủy thác việc kiểm tra thẩm quyền cho Domain Model
        // Nếu người gọi không phải SUPER_ADMIN, Domain sẽ tự động ném lỗi AccessDenied
        newAdmin.assignRole(Role.ADMIN, assignerRoles);

        return mapToResponse(userRepository.save(newAdmin));
    }

    /**
     * Helper: Lấy tập Role của User đang đăng nhập từ Spring Security Context.
     */
    private Set<Role> getCurrentUserRoles() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities()
                .stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(role -> {
                    try {
                        return Role.valueOf(role);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Transactional
    public UserResponse updateStatus(String userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> IdentityException.userNotFound(userId));

        user.setEnabled(enabled);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> IdentityException.userNotFound(userId));

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null)
            user.setPhoneNumber(request.getPhoneNumber());
        if (request.getEnabled() != null)
            user.setEnabled(request.getEnabled());

        return mapToResponse(userRepository.save(user));
    }

    private UserResponse mapToResponse(User user) {
        String meetingId = participantPort.getLastMeetingId(user.getId());
        long attendingShares = 0L;
        long receivedProxyShares = 0L;
        long delegatedShares = 0L;
        java.time.LocalDateTime checkedInAt = null;

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
