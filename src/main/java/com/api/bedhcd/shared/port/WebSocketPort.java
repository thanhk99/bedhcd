package com.api.bedhcd.shared.port;

public interface WebSocketPort {
    /**
     * Gửi dữ liệu realtime tới các client đang kết nối
     * @param destination Đường dẫn websocket (endpoint)
     * @param message Nội dung tin nhắn
     */
    void sendToUser(String userId, String destination, Object payload);
    void sendToTopic(String topic, Object payload);
}
