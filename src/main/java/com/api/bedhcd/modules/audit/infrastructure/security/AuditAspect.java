package com.api.bedhcd.modules.audit.infrastructure.security;

import com.api.bedhcd.modules.audit.application.service.AuditLogApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogApplicationService auditLogApplicationService;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(auditActivity)", returning = "result")
    public void logActivity(JoinPoint joinPoint, AuditActivity auditActivity, Object result) {
        try {
            // Get actor ID
            String actorId = null;
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                actorId = SecurityContextHolder.getContext().getAuthentication().getName(); // username as ID or
                                                                                            // depending on JWT
            }

            // In our system, the 'userId' is often stored in the JWT claims and extracted.
            // If the name is username, we might need to find adminId, but let's assume it's
            // username for now
            // or we can just use it directly.

            // Get IP
            String ipAddress = null;
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty()) {
                    ipAddress = request.getRemoteAddr();
                }
            }

            // Get payload (convert args to JSON)
            Object[] args = joinPoint.getArgs();
            String payload = "";
            if (args != null && args.length > 0) {
                Object payloadObj = null;
                for (Object arg : args) {
                    if (arg != null && !(arg instanceof String) && !(arg instanceof HttpServletRequest)
                            && !(arg.getClass().getName().contains("HttpServletResponse"))) {
                        payloadObj = arg;
                        break;
                    }
                }
                if (payloadObj != null) {
                    payload = objectMapper.writeValueAsString(payloadObj);
                } else {
                    payload = objectMapper.writeValueAsString(args);
                }
            }

            // Extract targetId if possible
            String targetId = null;
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object pathVarsObj = request
                        .getAttribute(org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if (pathVarsObj instanceof Map) {
                    Map<String, String> pathVariables = (Map<String, String>) pathVarsObj;
                    if (pathVariables.containsKey("adminId")) {
                        targetId = pathVariables.get("adminId");
                    } else if (pathVariables.containsKey("id")) {
                        targetId = pathVariables.get("id");
                    }
                }
            }

            if (targetId == null && result != null) {
                try {
                    java.lang.reflect.Method getDataMethod = result.getClass().getMethod("getData");
                    Object data = getDataMethod.invoke(result);
                    if (data != null) {
                        java.lang.reflect.Method getIdMethod = data.getClass().getMethod("getId");
                        Object idObj = getIdMethod.invoke(data);
                        if (idObj != null) {
                            targetId = idObj.toString();
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            auditLogApplicationService.logActionAsync(
                    actorId != null ? actorId : "SYSTEM",
                    auditActivity.action(),
                    auditActivity.resource(),
                    targetId,
                    payload,
                    ipAddress);

        } catch (Exception e) {
            // Log error but don't interrupt the business flow
            System.err.println("Failed to save audit log: " + e.getMessage());
        }
    }
}
