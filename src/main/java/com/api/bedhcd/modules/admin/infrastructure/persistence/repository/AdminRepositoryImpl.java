package com.api.bedhcd.modules.admin.infrastructure.persistence.repository;

import com.api.bedhcd.modules.admin.domain.model.Admin;
import com.api.bedhcd.modules.admin.domain.repository.AdminRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AdminRepositoryImpl implements AdminRepository {

    private final AdminJpaRepository jpaRepository;

    @Override
    public List<Admin> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Admin> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Admin> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Admin save(Admin domain) {
        AdminEntity entity = toEntity(domain);
        AdminEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void delete(Admin domain) {
        jpaRepository.deleteById(domain.getId());
    }

    private Admin toDomain(AdminEntity entity) {
        return Admin.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .isActive(entity.isActive())
                .department(entity.getDepartment())
                .jobTitle(entity.getJobTitle())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AdminEntity toEntity(Admin domain) {
        return AdminEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .password(domain.getPassword())
                .fullName(domain.getFullName())
                .email(domain.getEmail())
                .role(domain.getRole())
                .isActive(domain.isActive())
                .department(domain.getDepartment())
                .jobTitle(domain.getJobTitle())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
