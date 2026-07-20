package com.api.bedhcd.modules.identity.infrastructure.persistence;

import com.api.bedhcd.modules.identity.domain.model.RefreshToken;
import com.api.bedhcd.modules.identity.domain.repository.RefreshTokenRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final AdminJpaRepository adminJpaRepository;

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public void save(RefreshToken domain) {
        RefreshTokenEntity entity = toEntity(domain);
        jpaRepository.save(entity);
    }

    @Override
    public void deleteByToken(String token) {
        jpaRepository.deleteByToken(token);
    }

    @Override
    public void saveAdminToken(String adminId, String token, java.time.LocalDateTime expiryDate) {
        com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity admin = adminJpaRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminId));
        
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .token(token)
                .admin(admin)
                .expiryDate(expiryDate)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        jpaRepository.save(entity);
    }

    @Override
    public void deleteByAdminId(String adminId) {
        jpaRepository.deleteByAdmin_Id(adminId);
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .token(entity.getToken())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .adminId(entity.getAdmin() != null ? entity.getAdmin().getId() : null)
                .expiryDate(entity.getExpiryDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private RefreshTokenEntity toEntity(RefreshToken domain) {
        UserEntity user = null;
        if (domain.getUserId() != null) {
            user = userJpaRepository.findById(domain.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + domain.getUserId()));
        }
        
        com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity admin = null;
        if (domain.getAdminId() != null) {
            admin = adminJpaRepository.findById(domain.getAdminId())
                    .orElseThrow(() -> new RuntimeException("Admin not found: " + domain.getAdminId()));
        }
        
        return RefreshTokenEntity.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .user(user)
                .admin(admin)
                .expiryDate(domain.getExpiryDate())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
