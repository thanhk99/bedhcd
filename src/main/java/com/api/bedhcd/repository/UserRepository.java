package com.api.bedhcd.repository;

import com.api.bedhcd.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByCccd(String cccd);

    Optional<User> findByInvestorCode(String investorCode);

    Optional<User> findByCccdOrInvestorCode(String cccd, String investorCode);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
