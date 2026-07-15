package com.api.bedhcd.modules.admin.domain.repository;

import com.api.bedhcd.modules.admin.domain.model.Admin;

import java.util.List;
import java.util.Optional;

public interface AdminRepository {
    List<Admin> findAll();
    Optional<Admin> findById(String id);
    Optional<Admin> findByUsername(String username);
    Optional<Admin> findByEmail(String email);
    Optional<Admin> findByResetToken(String resetToken);
    Admin save(Admin admin);
    void delete(Admin admin);
}
