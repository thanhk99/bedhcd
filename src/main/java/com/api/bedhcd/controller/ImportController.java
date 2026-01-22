package com.api.bedhcd.controller;

import com.api.bedhcd.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/{meetingId}/shareholders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importShareholders(
            @PathVariable String meetingId,
            @RequestParam("file") MultipartFile file) {
        importService.importShareholders(meetingId, file);
        return ResponseEntity.ok("Import shareholders successfully");
    }

    @PostMapping("/{meetingId}/proxies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importProxies(
            @PathVariable String meetingId,
            @RequestParam("file") MultipartFile file) {
        importService.importProxies(meetingId, file);
        return ResponseEntity.ok("Import proxies successfully");
    }
}
