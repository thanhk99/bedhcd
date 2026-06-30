package com.api.bedhcd.repository;

import com.api.bedhcd.entity.Role;
import com.api.bedhcd.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // findByUsername removed

    Optional<User> findByEmail(String email);

    Optional<User> findByCccd(String cccd);

    java.util.List<User> findTop10ByCccdContaining(String cccd);

    Optional<User> findByInvestorCode(String investorCode);

    // existsByUsername removed

    Boolean existsByEmail(String email);

    Boolean existsByCccd(String cccd);

    Boolean existsByRolesContaining(Role role);

    long countByRolesContaining(Role role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "LOWER(u.cccd) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.investorCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    org.springframework.data.domain.Page<User> searchUsers(@org.springframework.data.repository.query.Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(u.sharesOwned), 0) FROM User u")
    long sumTotalShares();
}
