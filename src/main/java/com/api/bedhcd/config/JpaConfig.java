package com.api.bedhcd.config;

import com.api.bedhcd.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.of("SYSTEM");
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                return Optional.of(((User) principal).getId());
            }

            if (principal instanceof org.springframework.security.core.userdetails.User) {
                return Optional.of(((org.springframework.security.core.userdetails.User) principal).getUsername());
            }

            if (principal instanceof String) {
                return Optional.of((String) principal);
            }

            return Optional.of("SYSTEM");
        };
    }
}
