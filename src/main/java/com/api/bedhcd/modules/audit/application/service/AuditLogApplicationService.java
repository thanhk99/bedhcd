package com.api.bedhcd.modules.audit.application.service;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.modules.audit.api.v1.dto.AuditLogResponse;
import com.api.bedhcd.modules.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.api.bedhcd.modules.audit.infrastructure.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.api.bedhcd.shared.domain.UuidFactory;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogApplicationService {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final AdminJpaRepository adminJpaRepository;

    @Async
    @Transactional
    public void logActionAsync(String actorUsername, String action, String resource, String targetId, String payload,
            String ipAddress) {
        String finalActorId = actorUsername;
        String finalActorName = "Unknown Admin";

        if (actorUsername != null && !actorUsername.equals("SYSTEM")) {
            var adminOpt = adminJpaRepository.findByUsername(actorUsername);
            if (adminOpt.isPresent()) {
                finalActorId = adminOpt.get().getId();
                finalActorName = adminOpt.get().getFullName();
            }
        }

        AuditLogEntity log = AuditLogEntity.builder()
                .id(UuidFactory.generate())
                .actorId(finalActorId)
                .actorName(finalActorName)
                .action(action)
                .resource(resource)
                .targetId(targetId)
                .payload(payload)
                .ipAddress(ipAddress)
                .build();

        auditLogJpaRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllLogs() {
        return auditLogJpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getMyLogs(String actorUsername) {
        String actorId = adminJpaRepository.findByUsername(actorUsername)
                .map(AdminEntity::getId)
                .orElse(actorUsername);

        return auditLogJpaRepository.findByActorIdOrderByCreatedAtDesc(actorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse mapToResponse(AuditLogEntity entity) {
        AuditLogResponse res = new AuditLogResponse();
        res.setId(entity.getId());
        res.setActorId(entity.getActorId());
        res.setActorName(entity.getActorName());
        res.setAction(entity.getAction());
        res.setResource(entity.getResource());
        res.setTargetId(entity.getTargetId());
        res.setPayload(entity.getPayload());
        res.setIpAddress(entity.getIpAddress());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }
}
