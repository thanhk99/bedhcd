package com.api.bedhcd.modules.admin.application.service;

import com.api.bedhcd.modules.admin.domain.model.Admin;
import com.api.bedhcd.modules.admin.domain.repository.AdminRepository;
import com.api.bedhcd.modules.identity.domain.exception.IdentityException;
import com.api.bedhcd.modules.notification.application.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminPasswordResetService {

    private final AdminRepository adminRepository;
    private final EmailNotificationService emailNotificationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void forgotPassword(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> IdentityException.userNotFound("Không tìm thấy tài khoản với email này."));

        if (!admin.isActive()) {
            throw IdentityException.userNotFound("Tài khoản đã bị vô hiệu hóa.");
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        admin.setResetToken(token, expiry);
        adminRepository.save(admin);

        emailNotificationService.sendPasswordResetEmail(admin, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Admin admin = adminRepository.findByResetToken(token)
                .orElseThrow(() -> IdentityException.userNotFound("Token không hợp lệ hoặc đã hết hạn."));

        if (!admin.isResetTokenValid(token)) {
            throw IdentityException.userNotFound("Token không hợp lệ hoặc đã hết hạn.");
        }

        admin.updatePassword(passwordEncoder.encode(newPassword));
        admin.clearResetToken();
        adminRepository.save(admin);
    }
}
