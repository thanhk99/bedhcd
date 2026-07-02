package com.api.bedhcd.util;

import com.api.bedhcd.shared.domain.UuidFactory;
import com.api.bedhcd.shared.infrastructure.persistence.audit.AdminActionLogEntity;
import com.api.bedhcd.shared.infrastructure.persistence.audit.AdminActionLogJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class AdminActionAspect {

    private final AdminActionLogJpaRepository adminActionLogRepository;

    public AdminActionAspect(
            @org.springframework.context.annotation.Lazy AdminActionLogJpaRepository adminActionLogRepository) {
        this.adminActionLogRepository = adminActionLogRepository;
    }

    @Around("@annotation(logAdminAction)")
    public Object logAdminAction(ProceedingJoinPoint joinPoint, LogAdminAction logAdminAction) throws Throwable {
        String username = getCurrentUsername();
        String ipAddress = getClientIp();
        String action = logAdminAction.action();
        String resourceType = logAdminAction.resourceType();
        String resourceId = extractResourceId(joinPoint);
        String details = extractDetails(joinPoint);

        Object result;
        try {
            result = joinPoint.proceed();
            saveLog(username, action, resourceType, resourceId, details, ipAddress, "SUCCESS");
            return result;
        } catch (Throwable throwable) {
            saveLog(username, action, resourceType, resourceId, details + " | Error: " + throwable.getMessage(),
                    ipAddress, "FAILED");
            throw throwable;
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }
            return ipAddress;
        }
        return "UNKNOWN";
    }

    private String extractResourceId(ProceedingJoinPoint joinPoint) {
        // Thử tìm tham số có tên id, resolutionId, meetingId, v.v.
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String name = parameterNames[i].toLowerCase();
                if (name.contains("id")) {
                    return String.valueOf(args[i]);
                }
            }
        }
        return null;
    }

    private String extractDetails(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0)
            return "";

        return Arrays.stream(args)
                .map(arg -> arg == null ? "null" : arg.toString())
                .collect(Collectors.joining(", "));
    }

    private void saveLog(String username, String action, String resourceType, String resourceId, String details,
            String ipAddress, String status) {
        AdminActionLogEntity log = AdminActionLogEntity.builder()
                .id(UuidFactory.generate())
                .username(username)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details.length() > 2000 ? details.substring(0, 2000) : details)
                .ipAddress(ipAddress)
                .status(status)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        adminActionLogRepository.save(log);
    }
}
