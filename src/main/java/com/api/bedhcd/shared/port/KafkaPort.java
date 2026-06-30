package com.api.bedhcd.shared.port;

public interface KafkaPort {
    /**
     * Gửi event ra ngoài hệ thống (ví dụ: cập nhật dashboard realtime)
     * @param topic Topic cần gửi
     * @param payload Dữ liệu (Sẽ được serialize sang JSON)
     */
    void send(String topic, String key, Object payload);
}
