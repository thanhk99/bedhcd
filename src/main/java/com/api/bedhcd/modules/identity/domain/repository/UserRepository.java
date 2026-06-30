package com.api.bedhcd.modules.identity.domain.repository;

import com.api.bedhcd.modules.identity.domain.model.User;
import java.util.Optional;
import java.util.List;

public interface UserRepository {
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    Optional<User> findByCccd(String cccd);
    User save(User user);
    boolean existsByCccd(String cccd);
    List<User> searchByKeyword(String keyword);
    List<User> findAll(int page, int size);
    long count();
}
