package com.api.bedhcd.modules.meeting.application.service;

import com.api.bedhcd.shared.domain.enums.MeetingStatus;
import com.api.bedhcd.modules.meeting.domain.model.MeetingConfig;
import com.api.bedhcd.modules.meeting.domain.model.MeetingRules;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingConfigApplicationService {

    private final MeetingConfigRepository configRepository;

    /**
     * Lấy danh sách tất cả cấu hình
     * Không cache - List chứa Map lồng nhau phức tạp, dễ lỗi serialization
     */
    @Transactional(readOnly = true)
    public List<MeetingConfig> findAll() {
        return configRepository.findAll();
    }

    /**
     * Lấy chi tiết cấu hình theo ID
     */
    @Cacheable(value = "meeting-configs", key = "#id")
    @Transactional(readOnly = true)
    public MeetingConfig findById(String id) {
        return configRepository.findById(id).orElse(null);
    }

    /**
     * Lưu cấu hình (tạo mới hoặc cập nhật)
     */
    @CacheEvict(value = "meeting-configs", key = "#config.id")
    @Transactional
    public MeetingConfig save(MeetingConfig config) {
        return configRepository.save(config);
    }

    /**
     * Xóa cấu hình
     */
    @CacheEvict(value = "meeting-configs", key = "#id")
    @Transactional
    public void delete(String id) {
        configRepository.delete(id);
    }

    /**
     * Tạo một bản mẫu cấu hình đại hội chuẩn
     */
    @CacheEvict(value = "meeting-configs", key = "#id")
    @Transactional
    public MeetingConfig createStandardTemplate(String id, String name) {
        Map<String, MeetingRules> stateConfigs = new HashMap<>();

        // Cấu hình cho trạng thái Sắp diễn ra
        stateConfigs.put(MeetingStatus.SCHEDULED, MeetingRules.builder()
                .name("Sắp diễn ra")
                .nextState(MeetingStatus.ONGOING)
                .allowEditMeeting(true)
                .allowImportShareholder(true)
                .allowProxyRegistration(true)
                .allowAttendance(true)
                .allowVoting(false)
                .permissions(Map.of("EDIT", List.of("ADMIN"), "IMPORT", List.of("ADMIN", "STAFF")))
                .build());

        // Cấu hình cho trạng thái Đang diễn ra
        stateConfigs.put(MeetingStatus.ONGOING, MeetingRules.builder()
                .name("Đang diễn ra")
                .nextState(MeetingStatus.COMPLETED)
                .allowEditMeeting(false)
                .allowImportShareholder(false)
                .allowProxyRegistration(false)
                .allowAttendance(true)
                .allowVoting(true)
                .permissions(Map.of("VOTE", List.of("USER", "PROXY"), "VIEW", List.of("ALL")))
                .build());

        // Cấu hình cho trạng thái Đã kết thúc
        stateConfigs.put(MeetingStatus.COMPLETED, MeetingRules.builder()
                .name("Đã kết thúc")
                .allowEditMeeting(false)
                .allowAttendance(false)
                .allowVoting(false)
                .build());

        MeetingConfig config = MeetingConfig.builder()
                .id(id)
                .name(name)
                .stateConfigs(stateConfigs)
                .build();

        return configRepository.save(config);
    }
}
