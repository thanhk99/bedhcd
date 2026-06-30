package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import com.api.bedhcd.modules.meeting.domain.model.MeetingConfig;
import com.api.bedhcd.modules.meeting.domain.model.MeetingRules;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MeetingConfigRepositoryImpl implements MeetingConfigRepository {

    private final MeetingConfigJpaRepository jpaRepository;

    @Override
    public List<MeetingConfig> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<MeetingConfig> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public MeetingConfig save(MeetingConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(java.util.UUID.randomUUID().toString());
            MeetingConfigEntity entity = toEntity(config);
            return toDomain(jpaRepository.save(entity));
        }

        // Trường hợp update: Tìm thực thể hiện có để cập nhật
        return jpaRepository.findById(config.getId())
                .map(existingEntity -> {
                    existingEntity.setName(config.getName());
                    existingEntity.setDescription(config.getDescription());
                    existingEntity.setStateConfigs(config.getStateConfigs());
                    existingEntity.setUpdatedAt(java.time.LocalDateTime.now());
                    return toDomain(jpaRepository.save(existingEntity));
                })
                .orElseGet(() -> {
                    // Nếu không tìm thấy (ID được cung cấp nhưng chưa có trong DB), tạo mới
                    MeetingConfigEntity entity = toEntity(config);
                    return toDomain(jpaRepository.save(entity));
                });
    }

    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }

    private MeetingConfig toDomain(MeetingConfigEntity entity) {
        Map<String, MeetingRules> stateConfigs = new java.util.HashMap<>();
        if (entity.getStateConfigs() != null) {
            stateConfigs.putAll(entity.getStateConfigs());
        }

        return MeetingConfig.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .stateConfigs(stateConfigs)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MeetingConfigEntity toEntity(MeetingConfig domain) {
        Map<String, MeetingRules> stateConfigs = new java.util.HashMap<>();
        if (domain.getStateConfigs() != null) {
            stateConfigs.putAll(domain.getStateConfigs());
        }

        return MeetingConfigEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .stateConfigs(stateConfigs)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
