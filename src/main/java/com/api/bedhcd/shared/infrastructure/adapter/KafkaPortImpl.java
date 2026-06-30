package com.api.bedhcd.shared.infrastructure.adapter;

import com.api.bedhcd.shared.port.KafkaPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaPortImpl implements KafkaPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void send(String topic, String key, Object message) {
        try {
            log.info("Sending message to Kafka topic {}: key={}", topic, key);
            kafkaTemplate.send(topic, key, message);
        } catch (Exception e) {
            log.error("Failed to send message to Kafka topic {}: {}", topic, e.getMessage());
            // Có thể thêm cơ chế retry hoặc fallback ở đây nếu cần
        }
    }
}
