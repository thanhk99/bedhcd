package com.api.bedhcd.modules.participant.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bản ghi đối soát giữa số lượng cổ phần tham dự dự kiến (KSNB import)
 * và số lượng cổ phần tham dự thực tế trong hệ thống (meeting_participants).
 * Hiển thị song song cặp dữ liệu Import vs Thực tế.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationItemResponse {
    // --- Thông tin cổ đông chính ---
    private String cccd;
    private String fullName;
    private String shareholderStatus;
    private Long importDirectShares;
    private Long actualDirectShares;

    // --- Thông tin người nhận uỷ quyền theo File ---
    private String importProxyCccd;
    private String importProxyName;
    private Long importProxyShares;

    // --- Thông tin người THỰC SỰ nhận uỷ quyền trong hệ thống ---
    private String actualProxyCccd;
    private String actualProxyName;
    private String actualProxyStatus;
    private Long actualProxyShares;

    // --- Kết quả đối soát ---
    private String actualScenario;
    private String status; // KHOP | LECH
    private String reason;
    private Long difference;
}
