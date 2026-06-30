package com.api.bedhcd.shared.infrastructure.monitor;

import com.api.bedhcd.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
@Tag(name = "Monitor", description = "API theo dõi request log và thống kê hệ thống")
public class MonitorController {

    private final RequestLogStore requestLogStore;

    @Operation(summary = "Lấy danh sách request log gần nhất")
    @GetMapping("/requests")
    public ApiResponse<List<RequestLogEntry>> getRecentRequests(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String status // "ok" | "error"
    ) {
        List<RequestLogEntry> result = requestLogStore.getRecent(500);

        if (method != null && !method.isBlank()) {
            result = result.stream()
                    .filter(e -> method.equalsIgnoreCase(e.getMethod()))
                    .collect(Collectors.toList());
        }
        if (module != null && !module.isBlank()) {
            result = result.stream()
                    .filter(e -> module.equalsIgnoreCase(e.getModule()))
                    .collect(Collectors.toList());
        }
        if ("error".equalsIgnoreCase(status)) {
            result = result.stream()
                    .filter(e -> e.getStatusCode() != null && e.getStatusCode() >= 400)
                    .collect(Collectors.toList());
        } else if ("ok".equalsIgnoreCase(status)) {
            result = result.stream()
                    .filter(e -> e.getStatusCode() != null && e.getStatusCode() < 400)
                    .collect(Collectors.toList());
        }

        result = result.stream().limit(limit).collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @Operation(summary = "Thống kê tổng quan: tổng request, lỗi, phân theo module, top URI")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        List<RequestLogEntry> all = requestLogStore.getAll();

        long total = all.size();
        long errors = all.stream().filter(e -> e.getStatusCode() != null && e.getStatusCode() >= 400).count();
        long ok = total - errors;

        // Phân theo module
        Map<String, Long> byModule = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getModule() != null ? e.getModule() : "unknown",
                        Collectors.counting()
                ));

        // Phân theo status code
        Map<String, Long> byStatus = all.stream()
                .filter(e -> e.getStatusCode() != null)
                .collect(Collectors.groupingBy(
                        e -> String.valueOf(e.getStatusCode()),
                        Collectors.counting()
                ));

        // Top 10 URI hay được gọi nhất
        Map<String, Long> byUri = all.stream()
                .collect(Collectors.groupingBy(RequestLogEntry::getUri, Collectors.counting()));
        List<Map<String, Object>> topUris = byUri.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("uri", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        // Avg duration
        double avgDuration = all.stream()
                .filter(e -> e.getDurationMs() != null)
                .mapToLong(RequestLogEntry::getDurationMs)
                .average()
                .orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("ok", ok);
        stats.put("errors", errors);
        stats.put("errorRate", total > 0 ? Math.round((errors * 100.0 / total) * 10.0) / 10.0 : 0.0);
        stats.put("avgDurationMs", Math.round(avgDuration));
        stats.put("byModule", byModule);
        stats.put("byStatus", byStatus);
        stats.put("topUris", topUris);

        return ApiResponse.success(stats);
    }
}
