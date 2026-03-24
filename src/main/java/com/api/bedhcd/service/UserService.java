package com.api.bedhcd.service;

import com.api.bedhcd.dto.UserResponse;
import com.api.bedhcd.entity.Role;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.UserRepository;
import com.api.bedhcd.dto.response.ProxyDelegationResponse;
import com.api.bedhcd.repository.ProxyDelegationRepository;
import com.api.bedhcd.repository.MeetingParticipantRepository;
import com.api.bedhcd.repository.MeetingRepository;
import com.api.bedhcd.entity.MeetingParticipant;
import com.api.bedhcd.entity.Meeting;
import com.api.bedhcd.dto.request.RepresentativeRequest;
import com.api.bedhcd.dto.response.RepresentativeResponse;
import com.api.bedhcd.entity.ProxyDelegation;
import com.api.bedhcd.entity.enums.DelegationStatus;
import com.api.bedhcd.entity.enums.ParticipantStatus;
import com.api.bedhcd.entity.enums.ParticipationType;
import com.api.bedhcd.exception.BadRequestException;
import com.api.bedhcd.util.RandomUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.bedhcd.repository.LoginHistoryRepository;
import com.api.bedhcd.dto.response.LoginHistoryResponse;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserService {

    private final UserRepository userRepository;
    private final ProxyDelegationRepository proxyDelegationRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginHistoryRepository loginHistoryRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;

    public java.util.List<LoginHistoryResponse> getUserLoginHistory(String userId) {
        return loginHistoryRepository.findByUser_IdOrderByLoginTimeDesc(userId)
                .stream()
                .map(history -> LoginHistoryResponse.builder()
                        .id(history.getId())
                        .loginTime(history.getLoginTime())
                        .logoutTime(history.getLogoutTime())
                        .ipAddress(history.getIpAddress())
                        .userAgent(history.getUserAgent())
                        .location(history.getLocation())
                        .status(history.getStatus())
                        .failureReason(history.getFailureReason())
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByCccd(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String fullName, String email) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByCccd(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (fullName != null && !fullName.isEmpty()) {
            user.setFullName(fullName);
        }

        if (email != null && !email.isEmpty()) {
            user.setEmail(email);
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByCccd(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateRoles(String id, java.util.Set<Role> roles) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setRoles(roles);
        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.List<UserResponse> searchUsersByCccd(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return userRepository.findTop10ByCccdContaining(keyword).stream()
                .map(this::mapToUserResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public UserResponse createUser(com.api.bedhcd.dto.RegisterRequest request) {
        if (userRepository.existsByCccd(request.getCccd())) {
            throw new com.api.bedhcd.exception.BadRequestException("CCCD is already registered");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new com.api.bedhcd.exception.BadRequestException("Password must be at least 6 characters");
        }

        java.util.Set<Role> roles = new java.util.HashSet<>();
        roles.add(Role.SHAREHOLDER);

        User user = User.builder()
                .id(com.api.bedhcd.util.RandomUtil.generate6DigitId(userRepository::existsById))
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .investorCode(request.getInvestorCode())
                .cccd(request.getCccd())
                .dateOfIssue(request.getDateOfIssue())
                .placeOfIssue(request.getPlaceOfIssue())
                .address(request.getAddress())
                .nation(request.getNation())
                .sharesOwned(request.getSharesOwned() != null ? request.getSharesOwned() : 0L)
                .roles(roles)
                .enabled(true)
                .build();

        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional
    public RepresentativeResponse createRepresentative(RepresentativeRequest request) {
        // 1. Kiểm tra cổ đông uỷ quyền (delegator)
        User delegatorUser = userRepository.findByCccd(request.getDelegatorCccd())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cổ đông uỷ quyền không tồn tại với CCCD: " + request.getDelegatorCccd()));

        Meeting meeting = meetingRepository.findById(request.getMeetingId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuộc họp không tồn tại"));

        MeetingParticipant delegatorParticipant = meetingParticipantRepository
                .findByMeeting_IdAndUser_Id(meeting.getId(), delegatorUser.getId())
                .orElseThrow(() -> new BadRequestException("Cổ đông chưa đăng ký tham gia cuộc họp này"));

        Long sharesToDelegate = request.getSharesDelegated() != null ? request.getSharesDelegated() : 0L;
        if (sharesToDelegate > (delegatorParticipant.getSharesOwned() != null ? delegatorParticipant.getSharesOwned()
                : 0L)) {
            throw new BadRequestException("Số cổ phần uỷ quyền vượt quá số cổ phần hiện có. Khả dụng: "
                    + (delegatorParticipant.getSharesOwned() != null ? delegatorParticipant.getSharesOwned() : 0L));
        }

        // 2. Tìm hoặc tạo người đại diện (proxy)
        String rawPassword = String.valueOf(10000000 + (int) (Math.random() * 90000000)); // Sinh password 8 chữ số
        User proxyUser = userRepository.findByCccd(request.getCccd()).orElse(null);

        if (proxyUser == null) {
            Set<Role> roles = new HashSet<>();
            roles.add(Role.REPRESENTATIVE);
            proxyUser = User.builder()
                    .id(RandomUtil.generate6DigitId(userRepository::existsById))
                    .cccd(request.getCccd())
                    .fullName(request.getFullName())
                    .address(request.getAddress())
                    .placeOfIssue(request.getPlaceOfIssue())
                    .dateOfIssue(request.getDateOfIssue() != null ? request.getDateOfIssue() : "N/A")
                    .password(passwordEncoder.encode(rawPassword))
                    .roles(roles)
                    .email(request.getEmail() != null ? request.getEmail() : request.getCccd() + "@example.com")
                    .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : request.getCccd())
                    .investorCode("REP-" + request.getCccd())
                    .sharesOwned(0L)
                    .enabled(true)
                    .build();
            proxyUser = userRepository.save(proxyUser);
        } else {
            // Nếu đã có User (là cổ đông hoặc người uỷ quyền cũ), cập nhật thông tin và đảm
            // bảo có Role REPRESENTATIVE
            proxyUser.getRoles().add(Role.REPRESENTATIVE);
            if (request.getFullName() != null)
                proxyUser.setFullName(request.getFullName());
            if (request.getPlaceOfIssue() != null)
                proxyUser.setPlaceOfIssue(request.getPlaceOfIssue());
            if (request.getPhoneNumber() != null)
                proxyUser.setPhoneNumber(request.getPhoneNumber());
            if (request.getAddress() != null)
                proxyUser.setAddress(request.getAddress());
            userRepository.save(proxyUser);
        }

        final User finalProxyUser = proxyUser;
        // 3. Đăng ký người đại diện vào cuộc họp nếu chưa có
        MeetingParticipant proxyParticipant = meetingParticipantRepository
                .findByMeeting_IdAndUser_Id(meeting.getId(), proxyUser.getId())
                .orElseGet(() -> {
                    MeetingParticipant p = MeetingParticipant.builder()
                            .meeting(meeting)
                            .user(finalProxyUser)
                            .sharesOwned(0L)
                            .receivedProxyShares(0L)
                            .delegatedShares(0L)
                            .participationType(ParticipationType.PROXY)
                            .status(ParticipantStatus.PENDING)
                            .build();
                    return meetingParticipantRepository.save(p);
                });

        // 4. Tạo hoặc cập nhật bản ghi uỷ quyền (ProxyDelegation)
        ProxyDelegation delegation = proxyDelegationRepository
                .findByMeeting_IdAndDelegator_IdAndProxy_IdAndStatus(meeting.getId(), delegatorUser.getId(),
                        proxyUser.getId(), DelegationStatus.ACTIVE)
                .orElse(null);

        if (delegation == null) {
            delegation = ProxyDelegation.builder()
                    .meeting(meeting)
                    .delegator(delegatorUser)
                    .proxy(proxyUser)
                    .sharesDelegated(sharesToDelegate)
                    .authorizationDocument(request.getNote())
                    .status(DelegationStatus.ACTIVE)
                    .build();
        } else {
            // Nếu đã tồn tại uỷ quyền giữa 2 người này, cộng dồn số cổ phần
            long currentShares = delegation.getSharesDelegated() != null ? delegation.getSharesDelegated() : 0L;
            delegation.setSharesDelegated(currentShares + sharesToDelegate);
            if (request.getNote() != null) {
                delegation.setAuthorizationDocument(request.getNote());
            }
        }
        proxyDelegationRepository.save(delegation);

        // 5. Cập nhật số dư cổ phần trong cuộc họp
        delegatorParticipant.setSharesOwned(delegatorUser.getSharesOwned());
        proxyParticipant.setSharesOwned(proxyUser.getSharesOwned());

        long currentDelegatedCount = delegatorParticipant.getDelegatedShares() != null
                ? delegatorParticipant.getDelegatedShares()
                : 0L;
        long currentProxyReceived = proxyParticipant.getReceivedProxyShares() != null
                ? proxyParticipant.getReceivedProxyShares()
                : 0L;

        delegatorParticipant.setDelegatedShares(currentDelegatedCount + sharesToDelegate);
        proxyParticipant.setReceivedProxyShares(currentProxyReceived + sharesToDelegate);

        meetingParticipantRepository.save(delegatorParticipant);
        meetingParticipantRepository.save(proxyParticipant);

        userRepository.save(delegatorUser);
        userRepository.save(proxyUser);

        return RepresentativeResponse.builder()
                .id(proxyUser.getId())
                .fullName(proxyUser.getFullName())
                .cccd(proxyUser.getCccd())
                .generatedPassword(rawPassword)
                .meetingId(meeting.getId())
                .sharesDelegated(request.getSharesDelegated())
                .build();
    }

    @Transactional
    public String resetPassword(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String rawPassword = String.valueOf(10000000 + (int) (Math.random() * 90000000));
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);

        return rawPassword;
    }

    @Transactional
    public UserResponse updateUser(String id, com.api.bedhcd.dto.RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null)
            user.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null)
            user.setAddress(request.getAddress());
        if (request.getInvestorCode() != null)
            user.setInvestorCode(request.getInvestorCode());
        if (request.getCccd() != null)
            user.setCccd(request.getCccd());
        if (request.getDateOfIssue() != null)
            user.setDateOfIssue(request.getDateOfIssue());
        if (request.getPlaceOfIssue() != null)
            user.setPlaceOfIssue(request.getPlaceOfIssue());
        if (request.getSharesOwned() != null)
            user.setSharesOwned(request.getSharesOwned());
        if (request.getNation() != null)
            user.setNation(request.getNation());

        // Password update only if provided and not empty
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserResponse> getShareholdersByMeeting(String meetingId) {
        return meetingParticipantRepository.findByMeeting_Id(meetingId).stream()
                .map(this::mapParticipantToResponse)
                .collect(Collectors.toList());
    }

    private UserResponse mapToUserResponse(User user) {
        // Try to find if there is an ongoing meeting to get proxy shares context
        java.util.Optional<com.api.bedhcd.entity.Meeting> ongoingMeeting = meetingRepository
                .findFirstByStatus(com.api.bedhcd.entity.enums.MeetingStatus.ONGOING);

        UserResponse response;
        if (ongoingMeeting.isPresent()) {
            String meetingId = ongoingMeeting.get().getId();
            response = meetingParticipantRepository.findByMeeting_IdAndUser_Id(meetingId, user.getId())
                    .map(this::mapParticipantToResponse)
                    .orElse(mapBaseUserToResponse(user));

            // Fetch detailed delegations for the ongoing meeting
            response.setDelegationsMade(
                    proxyDelegationRepository.findByMeeting_IdAndDelegator_Id(meetingId, user.getId()).stream()
                            .map(this::mapToProxyResponse)
                            .collect(Collectors.toList()));

            response.setDelegationsReceived(
                    proxyDelegationRepository.findByMeeting_IdAndProxy_Id(meetingId, user.getId()).stream()
                            .map(this::mapToProxyResponse)
                            .collect(Collectors.toList()));
        } else {
            response = mapBaseUserToResponse(user);

            // If no ongoing meeting, fetch all historical delegations
            response.setDelegationsMade(proxyDelegationRepository.findByDelegator_Id(user.getId()).stream()
                    .map(this::mapToProxyResponse)
                    .collect(Collectors.toList()));

            response.setDelegationsReceived(proxyDelegationRepository.findByProxy_Id(user.getId()).stream()
                    .map(this::mapToProxyResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private UserResponse mapBaseUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .sharesOwned(user.getSharesOwned() != null ? user.getSharesOwned() : 0L)
                .receivedProxyShares(0L)
                .delegatedShares(0L)
                .phoneNumber(user.getPhoneNumber())
                .investorCode(user.getInvestorCode())
                .cccd(user.getCccd())
                .dateOfIssue(user.getDateOfIssue())
                .placeOfIssue(user.getPlaceOfIssue())
                .address(user.getAddress())
                .nation(user.getNation())
                .roles(user.getRoles())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserResponse mapParticipantToResponse(com.api.bedhcd.entity.MeetingParticipant participant) {
        User user = participant.getUser();
        UserResponse response = mapBaseUserToResponse(user);
        
        // Lấy số liệu thực tế từ bảng ủy quyền để đảm bảo hiển thị chính xác
        long receivedProxyShares = proxyDelegationRepository.sumReceivedProxyShares(
                participant.getMeeting().getId(), user.getId());
        
        response.setAttendingShares(participant.getAttendingShares() != null ? participant.getAttendingShares() : 0L);
        response.setReceivedProxyShares(receivedProxyShares);
        response.setDelegatedShares(participant.getDelegatedShares() != null ? participant.getDelegatedShares() : 0L);

        return response;
    }

    private ProxyDelegationResponse mapToProxyResponse(com.api.bedhcd.entity.ProxyDelegation delegation) {
        return ProxyDelegationResponse.builder()
                .id(delegation.getId())
                .delegatorId(delegation.getDelegator().getId())
                .delegatorName(delegation.getDelegator().getFullName())
                .proxyId(delegation.getProxy().getId())
                .proxyName(delegation.getProxy().getFullName())
                .sharesDelegated(delegation.getSharesDelegated())
                .authorizationDocument(delegation.getAuthorizationDocument())
                .status(delegation.getStatus())
                .createdAt(delegation.getCreatedAt())
                .revokedAt(delegation.getRevokedAt())
                .build();
    }
}
