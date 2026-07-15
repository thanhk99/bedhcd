package com.api.bedhcd.modules.notification.application.service;

import com.api.bedhcd.modules.admin.domain.model.Admin;
import com.api.bedhcd.modules.notification.domain.model.EmailJob;
import com.api.bedhcd.modules.notification.domain.repository.EmailJobRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender javaMailSender;
    private final EmailJobRepository emailJobRepository;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.base-url:http://10.16.7.73:3000}")
    private String baseUrl;

    @Async
    public void sendAdminCreatedEmail(Admin admin, String rawPassword) {
        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            log.warn("Cannot send email: Admin {} does not have an email address.", admin.getUsername());
            return;
        }

        String subject = "Thông tin tài khoản quản trị hệ thống ĐHCĐ";
        String body = buildAdminCreatedHtml(admin, rawPassword);

        sendHtmlEmailAsync(admin.getEmail(), subject, body);
    }

    @Async
    public void sendPasswordResetEmail(Admin admin, String resetToken) {
        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            log.warn("Cannot send email: Admin {} does not have an email address.", admin.getUsername());
            return;
        }

        String subject = "Yêu cầu khôi phục mật khẩu hệ thống ĐHCĐ";
        String body = buildPasswordResetHtml(admin, resetToken);

        sendHtmlEmailAsync(admin.getEmail(), subject, body);
    }

    private void sendHtmlEmailAsync(String toEmail, String subject, String body) {
        EmailJob job = EmailJob.createNew(toEmail, subject, body);
        job = emailJobRepository.save(job);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            javaMailSender.send(message);

            job.markSent();
            emailJobRepository.save(job);
            log.info("Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            job.markFailed(e.getMessage());
            emailJobRepository.save(job);
        }
    }

    private String buildAdminCreatedHtml(Admin admin, String rawPassword) {
        return "<html><body>" +
                "<h2>Xin chào " + admin.getFullName() + ",</h2>" +
                "<p>Tài khoản quản trị của bạn đã được tạo thành công trên hệ thống Đại Hội Cổ Đông.</p>" +
                "<p><b>Thông tin đăng nhập:</b></p>" +
                "<ul>" +
                "<li><b>Tên đăng nhập:</b> " + admin.getUsername() + "</li>" +
                "<li><b>Mật khẩu tạm thời:</b> " + rawPassword + "</li>" +
                "</ul>" +
                "<p>Vui lòng đăng nhập và đổi mật khẩu trong lần đầu tiên sử dụng.</p>" +
                "<p><a href='" + baseUrl + "'>Truy cập hệ thống</a></p>" +
                "<br><p>Trân trọng,<br>Ban Quản Trị Hệ Thống ĐHCĐ</p>" +
                "</body></html>";
    }

    private String buildPasswordResetHtml(Admin admin, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        return "<html><body>" +
                "<h2>Xin chào " + admin.getFullName() + ",</h2>" +
                "<p>Bạn vừa yêu cầu khôi phục mật khẩu cho tài khoản quản trị hệ thống Đại Hội Cổ Đông.</p>" +
                "<p>Vui lòng click vào nút bên dưới để đặt lại mật khẩu của bạn. Link này sẽ hết hạn sau 30 phút.</p>" +
                "<p><a href='" + resetLink
                + "' style='display:inline-block;padding:10px 20px;color:#007bff;text-decoration:none;border-radius:5px;'>Đặt lại mật khẩu</a></p>"
                +
                "<p>Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>" +
                "<br><p>Trân trọng,<br>Ban Quản Trị Hệ Thống ĐHCĐ</p>" +
                "</body></html>";
    }
}
