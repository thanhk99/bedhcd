package com.api.bedhcd.modules.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.bedhcd.shared.domain.enums.Role;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
        Optional<UserEntity> findByUsername(String username);

        Optional<UserEntity> findByCccd(String cccd);

        List<UserEntity> findByEmail(String email);

        List<UserEntity> findAllByCccdIn(List<String> cccds);

        boolean existsByRolesContaining(Role role);

        @org.springframework.data.jpa.repository.Query("SELECT u FROM UserEntity u WHERE " +
                        "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword)) OR " +
                        "LOWER(u.cccd) LIKE LOWER(CONCAT('%', :keyword)) OR " +
                        "LOWER(u.investorCode) LIKE LOWER(CONCAT('%', :keyword))")
        List<UserEntity> searchByKeyword(
                        @org.springframework.data.repository.query.Param("keyword") String keyword);

        @org.springframework.data.jpa.repository.Query("SELECT u FROM UserEntity u WHERE " +
                        "LOWER(u.fullName) LIKE LOWER(CONCAT(:keyword, '%')) OR " +
                        "LOWER(u.cccd) LIKE LOWER(CONCAT(:keyword, '%')) OR " +
                        "LOWER(u.investorCode) LIKE LOWER(CONCAT(:keyword, '%'))")
        List<UserEntity> searchTopByKeyword(
                        @org.springframework.data.repository.query.Param("keyword") String keyword,
                        org.springframework.data.domain.Pageable pageable);

        long countBySplitAccountFalse();
}
