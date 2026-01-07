package com.api.bedhcd.service;

import com.api.bedhcd.dto.UserResponse;
import com.api.bedhcd.entity.Role;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        return mapToUserResponse(user);
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
                .roles(roles)
                .enabled(true)
                .build();

        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(String id, com.api.bedhcd.dto.RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Note: For admin update simplifying validation slightly or adding checks
        // conditionally
        // Updating fields if provided
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

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .sharesOwned(user.getSharesOwned())
                .receivedProxyShares(user.getReceivedProxyShares())
                .delegatedShares(user.getDelegatedShares())
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
}
