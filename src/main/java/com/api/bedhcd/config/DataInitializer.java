package com.api.bedhcd.config;

import com.api.bedhcd.entity.Role;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create Admin if not exists
        if (!userRepository.existsByRolesContaining(Role.ADMIN)) {
            Set<Role> roles = new HashSet<>();
            roles.add(Role.ADMIN);
            roles.add(Role.SHAREHOLDER);

            User admin = User.builder()
                    .id("ADMIN1")
                    .username("admin")
                    .email("admin@dhcd.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Administrator")
                    .phoneNumber("0000000000")
                    .investorCode("ADMIN")
                    .cccd("000000000000")
                    .dateOfIssue("2020-01-01")
                    .address("System")
                    .roles(roles)
                    .enabled(true)
                    .build();

            userRepository.save(admin);
            System.out.println("Default Admin account created: admin / admin123");
        }
    }
}
