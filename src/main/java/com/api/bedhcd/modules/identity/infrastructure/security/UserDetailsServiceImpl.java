package com.api.bedhcd.modules.identity.infrastructure.security;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.modules.identity.infrastructure.persistence.UserEntity;
import com.api.bedhcd.modules.identity.infrastructure.persistence.UserJpaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserJpaRepository userRepository;
    private final AdminJpaRepository adminJpaRepository;

    public UserDetailsServiceImpl(
            @org.springframework.context.annotation.Lazy UserJpaRepository userRepository,
            @org.springframework.context.annotation.Lazy AdminJpaRepository adminJpaRepository) {
        this.userRepository = userRepository;
        this.adminJpaRepository = adminJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Kiểm tra trong bảng admin trước
        Optional<AdminEntity> adminOpt = adminJpaRepository.findByUsername(username);
        if (adminOpt.isPresent()) {
            AdminEntity admin = adminOpt.get();
            return new User(
                    admin.getUsername(),
                    admin.getPassword(),
                    admin.isActive(),
                    true,
                    true,
                    true,
                    java.util.List.of(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()))
            );
        }

        // Nếu không có, tìm trong bảng users
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList())
        );
    }
}
