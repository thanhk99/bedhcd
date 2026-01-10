package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.RepresentativeRequest;
import com.api.bedhcd.dto.response.RepresentativeResponse;
import com.api.bedhcd.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/representatives")
@RequiredArgsConstructor
public class RepresentativeController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RepresentativeResponse> createRepresentative(
            @Valid @RequestBody RepresentativeRequest request) {
        RepresentativeResponse response = userService.createRepresentative(request);
        return ResponseEntity.ok(response);
    }
}
