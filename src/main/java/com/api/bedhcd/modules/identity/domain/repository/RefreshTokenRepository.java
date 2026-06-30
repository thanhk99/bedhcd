package com.api.bedhcd.modules.identity.domain.repository;

import com.api.bedhcd.modules.identity.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByToken(String token);
    void save(RefreshToken refreshToken);
    void deleteByToken(String token);
}
