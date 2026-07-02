package com.api.bedhcd.shared.infrastructure.monitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import com.api.bedhcd.shared.domain.UuidFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter ghi lại mọi HTTP request: method, URI, user, IP, status, duration.
 * Dùng StatusCapturingWrapper nhẹ (không buffer body) thay
 * ContentCachingResponseWrapper.
 * Bỏ qua actuator, swagger, ws, và /api/v1/monitor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RequestLogStore requestLogStore;
    private final MeterRegistry meterRegistry;

    // Pattern trích xuất tên module từ URI, ví dụ: /api/v1/admin/... -> admin
    private static final Pattern MODULE_PATTERN = Pattern.compile("^/api/v\\d+/([^/]+)");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/ws")
                || uri.startsWith("/api/v1/monitor");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Wrapper nhẹ: chỉ capture status code, KHÔNG buffer body
        StatusCapturingWrapper wrapper = new StatusCapturingWrapper(response);

        String errorMessage = null;
        try {
            filterChain.doFilter(request, wrapper);
        } catch (Exception ex) {
            errorMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = wrapper.getStatus();

            String username = resolveUsername();
            String ip = resolveIp(request);
            String module = resolveModule(request.getRequestURI());

            RequestLogEntry entry = RequestLogEntry.builder()
                    .id(UuidFactory.generate())
                    .timestamp(LocalDateTime.now())
                    .method(request.getMethod())
                    .uri(request.getRequestURI())
                    .username(username)
                    .ipAddress(ip)
                    .statusCode(status)
                    .durationMs(duration)
                    .errorMessage(errorMessage)
                    .module(module)
                    .build();

            requestLogStore.add(entry);

            // --- Micrometer metrics (Prometheus scrape) ---
            // Counter: http_requests_total{module, method, status}
            Counter.builder("http_requests_total")
                    .tag("module", module)
                    .tag("method", request.getMethod())
                    .tag("status", String.valueOf(status))
                    .description("Tổng số HTTP request theo module/method/status")
                    .register(meterRegistry)
                    .increment();

            // Timer: http_request_duration_ms{module, method}
            Timer.builder("http_request_duration_ms")
                    .tag("module", module)
                    .tag("method", request.getMethod())
                    .description("Độ trễ xử lý request (ms)")
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);

            if (status >= 400) {
                log.warn("[{}] {} {} | status={} | {}ms | user={} | ip={} | err={}",
                        module.toUpperCase(), request.getMethod(), request.getRequestURI(),
                        status, duration, username, ip, errorMessage);
            } else {
                log.info("[{}] {} {} | status={} | {}ms | user={}",
                        module.toUpperCase(), request.getMethod(), request.getRequestURI(),
                        status, duration, username);
            }
        }
    }

    private String resolveUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return "anonymous";
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank())
            ip = request.getRemoteAddr();
        if (ip != null && ip.contains(","))
            ip = ip.split(",")[0].trim();
        return ip;
    }

    private String resolveModule(String uri) {
        Matcher matcher = MODULE_PATTERN.matcher(uri);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    /**
     * Wrapper nhẹ: chỉ ghi nhận status code.
     * KHÔNG buffer response body vào memory → không ảnh hưởng hiệu xuất.
     */
    private static class StatusCapturingWrapper extends HttpServletResponseWrapper {
        private int status = 200;

        public StatusCapturingWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        @Override
        public int getStatus() {
            return this.status;
        }
    }
}
