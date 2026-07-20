package com.api.bedhcd.modules.identity.infrastructure.port;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.identity.infrastructure.persistence.UserEntity;
import com.api.bedhcd.modules.identity.infrastructure.persistence.UserJpaRepository;
import com.api.bedhcd.shared.domain.UuidFactory;
import com.api.bedhcd.shared.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class IdentityPortImpl implements IdentityPort {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        return userRepository.findByUsername(username)
                .map(UserEntity::getId)
                .orElse(null);
    }

    @Override
    public Set<Role> getUserRoles(String userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getRoles)
                .orElse(Collections.emptySet());
    }

    @Override
    public boolean hasRole(Role role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String targetAuthority = "ROLE_" + role.name();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(targetAuthority));
    }

    @Override
    public java.util.Optional<String> getUserIdByCccd(String cccd) {
        return userRepository.findByCccd(cccd).map(UserEntity::getId);
    }

    @Override
    public com.api.bedhcd.shared.dto.UserDTO getUserInfo(String userId) {
        return userRepository.findById(userId).map(entity -> com.api.bedhcd.shared.dto.UserDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .cccd(entity.getCccd())
                .investorCode(entity.getInvestorCode())
                .phoneNumber(entity.getPhoneNumber())
                .sharesOwned(entity.getSharesOwned())
                .roles(entity.getRoles())
                .enabled(entity.isEnabled())
                .splitAccount(entity.isSplitAccount())
                .build()).orElse(null);
    }

    @Override
    public long countUsers() {
        return userRepository.countBySplitAccountFalse();
    }

    @Override
    public com.api.bedhcd.shared.dto.UserDTO createOrUpdateUser(com.api.bedhcd.shared.dto.UserDTO dto) {
        boolean isExisting = userRepository.findByCccd(dto.getCccd()).isPresent();

        UserEntity entity = userRepository.findByCccd(dto.getCccd())
                .orElseGet(() -> UserEntity.builder()
                        .id(com.api.bedhcd.shared.domain.UuidFactory.generate())
                        .cccd(dto.getCccd())
                        .username(dto.getCccd()) // Dùng CCCD làm username
                        .password(passwordEncoder.encode(dto.getCccd())) // Dùng CCCD làm password mặc định
                        .roles(Set.of(Role.SHAREHOLDER))
                        .enabled(true) // Mặc định kích hoạt cho tài khoản mới
                        .splitAccount(dto.isSplitAccount())
                        .build());

        entity.setPassword(passwordEncoder.encode(dto.getCccd()));

        // Chỉ ghi đè thông tin cơ bản khi có giá trị
        if (dto.getFullName() != null)
            entity.setFullName(dto.getFullName());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            entity.setEmail(dto.getEmail());
        }
        if (dto.getInvestorCode() != null)
            entity.setInvestorCode(dto.getInvestorCode());
        if (dto.getPhoneNumber() != null)
            entity.setPhoneNumber(dto.getPhoneNumber());

        // Chỉ ghi đè sharesOwned nếu DTO truyền vào giá trị thực sự (không null).
        // Tránh trường hợp DTO được build với sharesOwned=0 làm mất cổ phần của cổ đông
        // hiện có.
        if (dto.getSharesOwned() != null) {
            entity.setSharesOwned(dto.getSharesOwned());
        }

        // Chỉ cập nhật enabled nếu tài khoản là mới.

        if (!isExisting) {
            entity.setEnabled(true);
            entity.setSplitAccount(dto.isSplitAccount());
        }

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            entity.setRoles(dto.getRoles());
        }

        UserEntity saved = userRepository.save(entity);
        return getUserInfo(saved.getId());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public java.util.List<com.api.bedhcd.shared.dto.UserDTO> createOrUpdateUserBatch(
            java.util.List<com.api.bedhcd.shared.dto.UserDTO> users) {
        if (users == null || users.isEmpty())
            return java.util.Collections.emptyList();

        // 1. Tải trước tất cả user hiện có bằng CCCD để tối ưu
        java.util.List<String> cccds = users.stream().map(com.api.bedhcd.shared.dto.UserDTO::getCccd).toList();
        java.util.List<UserEntity> existingUsers = userRepository.findAllByCccdIn(cccds);
        java.util.Map<String, UserEntity> existingMap = existingUsers.stream()
                .collect(java.util.stream.Collectors.toMap(UserEntity::getCccd, u -> u));

        java.util.List<UserEntity> entitiesToSave = users.parallelStream().map(dto -> {
            UserEntity entity = existingMap.get(dto.getCccd());
            boolean isNew = (entity == null);

            if (isNew) {
                // Chỉ encode password khi tạo mới - không encode lại cho user hiện có
                // Chạy parallelStream giúp mã hóa nhiều password cùng lúc, tận dụng đa nhân CPU
                entity = UserEntity.builder()
                        .id(UuidFactory.generate())
                        .cccd(dto.getCccd())
                        .username(dto.getCccd())
                        .password(passwordEncoder.encode(dto.getCccd()))
                        .roles(Set.of(Role.SHAREHOLDER))
                        .enabled(true)
                        .splitAccount(dto.isSplitAccount())
                        .build();
            }

            // KHÔNG gọi setPassword cho user cũ (tránh bcrypt N lần)
            if (dto.getFullName() != null)
                entity.setFullName(dto.getFullName());
            if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                entity.setEmail(dto.getEmail());
            }
            if (dto.getInvestorCode() != null)
                entity.setInvestorCode(dto.getInvestorCode());
            if (dto.getPhoneNumber() != null)
                entity.setPhoneNumber(dto.getPhoneNumber());

            if (dto.getSharesOwned() != null) {
                entity.setSharesOwned(dto.getSharesOwned());
            }

            if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
                entity.setRoles(dto.getRoles());
            }

            return entity;
        }).collect(java.util.stream.Collectors.toList());

        java.util.List<UserEntity> savedEntities = userRepository.saveAll(entitiesToSave);
        return savedEntities.stream()
                .map(entity -> com.api.bedhcd.shared.dto.UserDTO.builder()
                        .id(entity.getId())
                        .username(entity.getUsername())
                        .fullName(entity.getFullName())
                        .email(entity.getEmail())
                        .cccd(entity.getCccd())
                        .investorCode(entity.getInvestorCode())
                        .phoneNumber(entity.getPhoneNumber())
                        .sharesOwned(entity.getSharesOwned())
                        .roles(entity.getRoles())
                        .enabled(entity.isEnabled())
                        .splitAccount(entity.isSplitAccount())
                        .build())
                .toList();
    }
}
