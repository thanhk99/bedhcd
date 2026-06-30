package com.api.bedhcd.modules.identity.infrastructure.persistence;

import com.api.bedhcd.modules.identity.domain.model.LoginHistory;
import com.api.bedhcd.modules.identity.domain.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LoginHistoryRepositoryImpl implements LoginHistoryRepository {

    private final LoginHistoryJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public void save(LoginHistory domain) {
        LoginHistoryEntity entity = toEntity(domain);
        jpaRepository.save(entity);
    }

    @Override
    public List<LoginHistory> findByUserId(String userId) {
        return jpaRepository.findByUserIdOrderByLoginTimeDesc(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LoginHistory> findBySessionToken(String sessionToken) {
        return jpaRepository.findBySessionToken(sessionToken).map(this::toDomain);
    }

    private LoginHistory toDomain(LoginHistoryEntity entity) {
        return LoginHistory.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .loginTime(entity.getLoginTime())
                .logoutTime(entity.getLogoutTime())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .location(entity.getLocation())
                .status(entity.getStatus())
                .failureReason(entity.getFailureReason())
                .sessionToken(entity.getSessionToken())
                .loginMethod(entity.getLoginMethod())
                .build();
    }

    private LoginHistoryEntity toEntity(LoginHistory domain) {
        UserEntity user = userJpaRepository.findById(domain.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + domain.getUserId()));

        return LoginHistoryEntity.builder()
                .id(domain.getId())
                .user(user)
                .loginTime(domain.getLoginTime())
                .logoutTime(domain.getLogoutTime())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .location(domain.getLocation())
                .status(domain.getStatus())
                .failureReason(domain.getFailureReason())
                .sessionToken(domain.getSessionToken())
                .loginMethod(domain.getLoginMethod())
                .build();
    }
}
