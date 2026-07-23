package com.api.bedhcd.modules.meeting.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingRules {
    private String name;
    private String nextState; // Trạng thái tiếp theo (dạng Linked List A -> B -> C)

    private AdminRules adminRules;
    private ShareholderRules shareholderRules;

    /**
     * Map phân quyền theo Action và danh sách Role được phép
     * Ví dụ: "EDIT" -> ["ADMIN"], "VOTE" -> ["USER", "PROXY"]
     */
    private Map<String, List<String>> permissions;

    /**
     * Kiểm tra xem một Role có quyền thực hiện Action nhất định không
     */
    public boolean hasPermission(String action, String role) {
        if (permissions == null || !permissions.containsKey(action))
            return false;
        return permissions.get(action).contains(role);
    }

    public static MeetingRules createEmpty(String name, String nextState) {
        return MeetingRules.builder()
                .name(name)
                .nextState(nextState)
                .adminRules(AdminRules.createEmpty())
                .shareholderRules(ShareholderRules.createEmpty())
                .build();
    }
}

