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

    Optional<User> findByInvestorCode(String investorCode);

    Optional<User> findByCccdOrInvestorCode(String cccd, String investorCode);

    // existsByUsername removed

    Boolean existsByEmail(String email);

    Boolean existsByRolesContaining(Role role);

    long countByRolesContaining(Role role);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(u.sharesOwned), 0) FROM User u")
    long sumTotalShares();
}
