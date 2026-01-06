package com.api.bedhcd.service;

import com.api.bedhcd.entity.NotificationLog;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.entity.enums.NotificationStatus;
import com.api.bedhcd.entity.enums.NotificationType;
import com.api.bedhcd.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void sendNotification(User user, String subject, String content, NotificationType type) {
        NotificationLog log = NotificationLog.builder()
                .user(user)
                .subject(subject)
                .content(content)
                .notificationType(type)
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        notificationLogRepository.save(log);
        // Logic gửi mail/SMS thực tế sẽ ở đây
    }
}
