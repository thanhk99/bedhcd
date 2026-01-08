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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserService {

    private final UserRepository userRepository;
    private final ProxyDelegationRepository proxyDelegationRepository;
    private final PasswordEncoder passwordEncoder;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        UserResponse response = mapToUserResponse(user);

        response.setDelegationsMade(proxyDelegationRepository.findByDelegator_Id(id).stream()
                .map(this::mapToProxyResponse)
                .collect(Collectors.toList()));

        response.setDelegationsReceived(proxyDelegationRepository.findByProxy_Id(id).stream()
                .map(this::mapToProxyResponse)
                .collect(Collectors.toList()));

        return response;
    }

    @Transactional
    public UserResponse updateProfile(String fullName, String email) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
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
        User user = userRepository.findByUsername(username)
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

    @Transactional
    public UserResponse createUser(com.api.bedhcd.dto.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new com.api.bedhcd.exception.BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.api.bedhcd.exception.BadRequestException("Email is already registered");
        }

        java.util.Set<Role> roles = new java.util.HashSet<>();
        roles.add(Role.SHAREHOLDER);

        User user = User.builder()
                .id(com.api.bedhcd.util.RandomUtil.generate6DigitId(userRepository::existsById))
                .username(request.getUsername())
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

        return mapToUserResponse(user);
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
            if (request.getSharesOwned() != null)
                user.setSharesOwned(request.getSharesOwned());

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

        if (ongoingMeeting.isPresent()) {
            return meetingParticipantRepository.findByMeeting_IdAndUser_Id(ongoingMeeting.get().getId(), user.getId())
                    .map(this::mapParticipantToResponse)
                    .orElse(mapBaseUserToResponse(user));
        }

        return mapBaseUserToResponse(user);
    }

    private UserResponse mapBaseUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .sharesOwned(user.getSharesOwned())
                .receivedProxyShares(0L)
                .delegatedShares(0L)
                .totalShares(user.getSharesOwned() != null ? user.getSharesOwned() : 0L)
                .phoneNumber(user.getPhoneNumber())
                .investorCode(user.getInvestorCode())
                .cccd(user.getCccd())
                .dateOfIssue(user.getDateOfIssue())
                .address(user.getAddress())
                .roles(user.getRoles())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserResponse mapParticipantToResponse(com.api.bedhcd.entity.MeetingParticipant participant) {
        User user = participant.getUser();
        UserResponse response = mapBaseUserToResponse(user);
        response.setSharesOwned(participant.getSharesOwned());
        response.setReceivedProxyShares(participant.getReceivedProxyShares());
        response.setDelegatedShares(participant.getDelegatedShares());
        response.setTotalShares((participant.getSharesOwned() != null ? participant.getSharesOwned() : 0) +
                (participant.getReceivedProxyShares() != null ? participant.getReceivedProxyShares() : 0));
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
