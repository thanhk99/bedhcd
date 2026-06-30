package com.api.bedhcd.shared.infrastructure.adapter;

import com.api.bedhcd.shared.port.WebSocketPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPortImpl implements WebSocketPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToUser(String userId, String destination, Object payload) {
        log.debug("Sending WebSocket message to user {}: {}", userId, destination);
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
    }

    @Override
    public void sendToTopic(String topic, Object payload) {
        log.debug("Sending WebSocket message to topic {}: {}", topic, payload);
        messagingTemplate.convertAndSend(topic, payload);
    }
}
