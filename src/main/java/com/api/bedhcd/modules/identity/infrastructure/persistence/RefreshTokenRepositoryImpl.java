package com.api.bedhcd.modules.identity.infrastructure.persistence;

import com.api.bedhcd.modules.identity.domain.model.RefreshToken;
import com.api.bedhcd.modules.identity.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;

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

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .token(entity.getToken())
                .userId(entity.getUser().getId())
                .expiryDate(entity.getExpiryDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private RefreshTokenEntity toEntity(RefreshToken domain) {
        UserEntity user = userJpaRepository.findById(domain.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + domain.getUserId()));
        
        return RefreshTokenEntity.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .user(user)
                .expiryDate(domain.getExpiryDate())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
