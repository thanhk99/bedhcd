package com.api.bedhcd.service;

import com.api.bedhcd.config.CookieUtil;
import com.api.bedhcd.config.JwtUtil;
import com.api.bedhcd.dto.AuthResponse;
import com.api.bedhcd.dto.LoginRequest;
import com.api.bedhcd.dto.RegisterRequest;
import com.api.bedhcd.entity.RefreshToken;
import com.api.bedhcd.entity.Role;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.exception.BadRequestException;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.exception.UnauthorizedException;
import com.api.bedhcd.repository.RefreshTokenRepository;
import com.api.bedhcd.repository.UserRepository;
import com.api.bedhcd.repository.MeetingRepository;
import com.api.bedhcd.repository.MeetingParticipantRepository;
import com.api.bedhcd.repository.LoginHistoryRepository;
import com.api.bedhcd.entity.Meeting;
import com.api.bedhcd.entity.MeetingParticipant;
import com.api.bedhcd.entity.LoginHistory;
import com.api.bedhcd.entity.enums.LoginStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.api.bedhcd.util.RandomUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthService {

        private final UserRepository userRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final CookieUtil cookieUtil;
        private final AuthenticationManager authenticationManager;
        private final UserDetailsService userDetailsService;
        private final MeetingRepository meetingRepository;
        private final MeetingParticipantRepository meetingParticipantRepository;
        private final LoginHistoryRepository loginHistoryRepository;

        @Value("${jwt.refresh-token-expiration}")
        private Long refreshTokenExpiration;

        @SuppressWarnings("null")
        @Transactional
        public AuthResponse register(RegisterRequest request) {
                // Check if email already exists

                // Check if email already exists
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new BadRequestException("Email is already registered");
                }

                // Create new user
                Set<Role> roles = new HashSet<>();
                roles.add(Role.SHAREHOLDER);

                User user = User.builder()
                                .id(RandomUtil.generate6DigitId(userRepository::existsById))
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .fullName(request.getFullName())
                                .phoneNumber(request.getPhoneNumber())
                                .investorCode(request.getInvestorCode())
                                .cccd(request.getCccd())
                                .dateOfIssue(request.getDateOfIssue())
                                .address(request.getAddress())
                                .sharesOwned(request.getSharesOwned() != null ? request.getSharesOwned() : 0L)
                                .roles(roles)
                                .enabled(true)
                                .build();

                user = userRepository.save(user);

                // Link user to meeting
                Meeting meeting = meetingRepository.findById(request.getMeetingId())
                                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

                MeetingParticipant participant = MeetingParticipant.builder()
                                .meeting(meeting)
                                .user(user)
                                .sharesOwned(user.getSharesOwned() != null ? user.getSharesOwned() : 0L)
                                .receivedProxyShares(0L)
                                .delegatedShares(0L)
                                .participationType(com.api.bedhcd.entity.enums.ParticipationType.DIRECT)
                                .status(com.api.bedhcd.entity.enums.ParticipantStatus.PENDING)
                                .build();
                meetingParticipantRepository.save(participant);

                // Generate tokens
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getCccd());
                String accessToken = jwtUtil.generateAccessToken(userDetails);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .roles(user.getRoles())
                                .build();
        }

        @SuppressWarnings("null")
        @Transactional
        public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
                // Get user details first
                User user = userRepository.findByCccd(request.getIdentifier())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                try {
                        // Authenticate user
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.getIdentifier(),
                                                        request.getPassword()));
                } catch (Exception e) {
                        // We do NOT log failures anymore as per requirement
                        throw e;
                }

                // Generate tokens
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getCccd());
                String accessToken = jwtUtil.generateAccessToken(userDetails);
                String refreshToken = jwtUtil.generateRefreshToken(userDetails);

                // Save refresh token to database
                saveRefreshToken(user, refreshToken);

                // Add refresh token to cookie
                cookieUtil.addRefreshTokenCookie(response, refreshToken);

                // Save login history
                saveLoginHistory(user, refreshToken, true, com.api.bedhcd.entity.enums.LoginMethod.PASSWORD,
                                httpRequest);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .roles(user.getRoles())
                                .build();
        }

        private void saveLoginHistory(User user, String token, boolean success,
                        com.api.bedhcd.entity.enums.LoginMethod method, HttpServletRequest request) {
                try {
                        String ipAddress = request.getHeader("X-Forwarded-For");
                        if (ipAddress == null || ipAddress.isEmpty()) {
                                ipAddress = request.getRemoteAddr();
                        }

                        String userAgent = request.getHeader("User-Agent");

                        LoginHistory history = LoginHistory.builder()
                                        .user(user)
                                        .loginTime(LocalDateTime.now())
                                        .status(success ? LoginStatus.SUCCESS : LoginStatus.FAILED)
                                        .sessionToken(token)
                                        .loginMethod(method)
                                        .ipAddress(ipAddress)
                                        .userAgent(userAgent)
                                        .build();
                        loginHistoryRepository.save(history);
                } catch (Exception e) {
                        // Log error but don't fail authentication
                        System.err.println("Failed to save login history: " + e.getMessage());
                }
        }

        // ... existing refreshAccessToken and logout methods ...

        @Transactional
        public AuthResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
                // Get refresh token from cookie
                String refreshToken = cookieUtil.getRefreshTokenFromCookie(request)
                                .orElseThrow(() -> new UnauthorizedException("Refresh token not found in cookie"));

                // Validate refresh token
                RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

                if (storedToken.isExpired()) {
                        refreshTokenRepository.delete(storedToken);
                        throw new UnauthorizedException("Refresh token has expired");
                }

                // Generate new access token
                UserDetails userDetails = userDetailsService.loadUserByUsername(storedToken.getUser().getCccd());
                String newAccessToken = jwtUtil.generateAccessToken(userDetails);

                User user = storedToken.getUser();

                return AuthResponse.builder()
                                .accessToken(newAccessToken)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .roles(user.getRoles())
                                .build();
        }

        @Transactional
        public void logout(HttpServletRequest request, HttpServletResponse response) {
                // Get refresh token from cookie
                cookieUtil.getRefreshTokenFromCookie(request).ifPresent(token -> {
                        // Delete refresh token from database
                        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
                });

                // Delete refresh token cookie
                cookieUtil.deleteRefreshTokenCookie(response);

                // Update logout time in history if possible
                cookieUtil.getRefreshTokenFromCookie(request).ifPresent(token -> {
                        loginHistoryRepository.findBySessionToken(token).ifPresent(history -> {
                                history.setLogoutTime(LocalDateTime.now());
                                loginHistoryRepository.save(history);
                        });
                });
        }

        @SuppressWarnings("null")
        private void saveRefreshToken(User user, String token) {
                // Create new refresh token
                RefreshToken refreshToken = RefreshToken.builder()
                                .token(token)
                                .user(user)
                                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                                .createdAt(LocalDateTime.now())
                                .build();

                refreshTokenRepository.save(refreshToken);
        }

        @Transactional
        public AuthResponse loginWithoutPassword(User user, HttpServletRequest request, HttpServletResponse response) {
                // Generate tokens
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getCccd());
                String accessToken = jwtUtil.generateAccessToken(userDetails);
                String refreshToken = jwtUtil.generateRefreshToken(userDetails);

                // Save refresh token to database
                saveRefreshToken(user, refreshToken);

                // Add refresh token to cookie
                cookieUtil.addRefreshTokenCookie(response, refreshToken);

                // Save login history
                saveLoginHistory(user, refreshToken, true, com.api.bedhcd.entity.enums.LoginMethod.QR_MAGIC_LINK,
                                request);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .roles(user.getRoles())
                                .build();
        }
}
