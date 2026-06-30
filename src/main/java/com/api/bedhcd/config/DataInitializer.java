package com.api.bedhcd.config;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.shared.domain.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminJpaRepository adminJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(@org.springframework.context.annotation.Lazy AdminJpaRepository adminJpaRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.adminJpaRepository = adminJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // Tự động migration cho cột enabled nếu Hibernate update không chạy
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE");
            System.out.println("Database migration: Column 'enabled' checked/added to 'users' table.");
        } catch (Exception e) {
            System.err.println("Database migration warning: " + e.getMessage());
        }

        // Cập nhật check constraint cho cột roles trong bảng user_roles để cho phép
        // SUPER_ADMIN
        try {
            jdbcTemplate.execute("ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS user_roles_roles_check");
            jdbcTemplate.execute(
                    "ALTER TABLE user_roles ADD CONSTRAINT user_roles_roles_check " +
                            "CHECK (roles IN ('SUPER_ADMIN', 'ADMIN', 'SHAREHOLDER', 'REPRESENTATIVE'))");
            System.out.println("Database migration: Constraint 'user_roles_roles_check' updated with SUPER_ADMIN.");
        } catch (Exception e) {
            System.err.println("Database migration warning (roles constraint): " + e.getMessage());
        }

        // Create SuperAdmin in admins table if not exists
        if (!adminJpaRepository.existsByRole(Role.SUPER_ADMIN)) {
            AdminEntity admin = AdminEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Super Administrator")
                    .email("admin@example.com")
                    .role(Role.SUPER_ADMIN)
                    .isActive(true)
                    .build();

            adminJpaRepository.save(admin);
            System.out.println("Default SuperAdmin account created in admins table: admin / admin123");
        }
    }
}
