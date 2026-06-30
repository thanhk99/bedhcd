package com.api.bedhcd.modules.meeting.api.v1;

import com.api.bedhcd.modules.meeting.application.service.MeetingConfigApplicationService;
import com.api.bedhcd.modules.meeting.domain.model.MeetingConfig;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meeting/configs")
@RequiredArgsConstructor
public class MeetingConfigController {

    private final MeetingConfigApplicationService configService;

    @GetMapping
    public ApiResponse<List<MeetingConfig>> getAll() {
        return ApiResponse.success(configService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<MeetingConfig> getById(@PathVariable String id) {
        return ApiResponse.success(configService.findById(id));
    }

    @PostMapping
    public ApiResponse<MeetingConfig> create(@RequestBody MeetingConfig config) {
        return ApiResponse.success(configService.save(config));
    }

    @PutMapping("/{id}")
    public ApiResponse<MeetingConfig> update(@PathVariable String id, @RequestBody MeetingConfig config) {
        config.setId(id);
        return ApiResponse.success(configService.save(config));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        configService.delete(id);
        return ApiResponse.success(null);
    }
}
