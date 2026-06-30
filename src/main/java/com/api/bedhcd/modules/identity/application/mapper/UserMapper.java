package com.api.bedhcd.modules.identity.application.mapper;

import com.api.bedhcd.modules.identity.domain.model.User;
import com.api.bedhcd.modules.identity.infrastructure.persistence.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .address(entity.getAddress())
                .cccd(entity.getCccd())
                .investorCode(entity.getInvestorCode())
                .sharesOwned(entity.getSharesOwned())
                .roles(entity.getRoles())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return UserEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .password(domain.getPassword())
                .fullName(domain.getFullName())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .address(domain.getAddress())
                .cccd(domain.getCccd())
                .investorCode(domain.getInvestorCode())
                .sharesOwned(domain.getSharesOwned())
                .roles(domain.getRoles())
                .enabled(domain.isEnabled())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
