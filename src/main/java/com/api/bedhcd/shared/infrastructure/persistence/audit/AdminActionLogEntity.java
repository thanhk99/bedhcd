package com.api.bedhcd.shared.infrastructure.persistence.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_action_logs_new")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionLogEntity {
    @Id
    private String id;
    
    private String username;
    private String action;
    private String resourceType;
    private String resourceId;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    private String ipAddress;
    private String status;
    private LocalDateTime timestamp;
}
