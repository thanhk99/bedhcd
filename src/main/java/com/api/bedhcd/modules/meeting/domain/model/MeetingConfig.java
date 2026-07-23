package com.api.bedhcd.modules.meeting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingConfig {
    private String id;
    private String name;
    private String description;

    /**
     * Cấu hình chi tiết cho từng trạng thái cuộc họp
     */
    private Map<String, MeetingRules> stateConfigs;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method để tạo cấu hình mặc định trống (không có sẵn cấu hình tự động, hoặc nếu cần thì tạo một map rỗng)
     */
    public static MeetingConfig createDefault() {
        return MeetingConfig.builder()
                .stateConfigs(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Lấy quy tắc cho một trạng thái cụ thể
     */
    public MeetingRules getRulesForStatus(String status) {
        if (stateConfigs == null)
            return null;
        return stateConfigs.get(status);
    }
}

