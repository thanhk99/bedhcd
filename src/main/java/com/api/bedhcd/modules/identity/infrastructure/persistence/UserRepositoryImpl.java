package com.api.bedhcd.modules.identity.infrastructure.persistence;

import com.api.bedhcd.modules.identity.application.mapper.UserMapper;
import com.api.bedhcd.modules.identity.domain.model.User;
import com.api.bedhcd.modules.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(String id) {
        return jpaRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByCccd(String cccd) {
        return jpaRepository.findByCccd(cccd).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByCccd(String cccd) {
        return jpaRepository.findByCccd(cccd).isPresent();
    }

    @Override
    public User save(User domain) {
        UserEntity entity = userMapper.toEntity(domain);
        return userMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public java.util.List<User> searchByKeyword(String keyword) {
        return jpaRepository.searchByKeyword(keyword).stream()
                .map(userMapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.List<User> findAll(int page, int size) {
        return jpaRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size)).getContent().stream()
                .map(userMapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}
