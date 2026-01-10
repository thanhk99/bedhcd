package com.api.bedhcd.controller;

import com.api.bedhcd.dto.AuthResponse;
import com.api.bedhcd.dto.request.AdminQrGenerateRequest;
import com.api.bedhcd.dto.request.MagicLoginRequest;
import com.api.bedhcd.dto.response.QrGenerateResponse;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.service.AuthService;
import com.api.bedhcd.service.QrAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/qr")
@RequiredArgsConstructor
public class QrAuthController {

    private final QrAuthService qrAuthService;
    private final AuthService authService;

    // TODO: Configure this via application.properties or environment variable
    private static final String FRONTEND_URL = "http://localhost:3000/login/";

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QrGenerateResponse> generateQrForUser(@RequestBody AdminQrGenerateRequest request) {
        String token = qrAuthService.generateMagicToken(request.getUserId(), request.getExpiresAt());
        String qrContent = FRONTEND_URL + "?token=" + token;

        return ResponseEntity.ok(QrGenerateResponse.builder()
                .token(token)
                .qrContent(qrContent)
                .build());
    }

    @PostMapping("/magic-login")
    public ResponseEntity<AuthResponse> magicLogin(@RequestBody MagicLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        User user = qrAuthService.validateMagicToken(request.getToken());
        AuthResponse authResponse = authService.loginWithoutPassword(user, httpRequest, response);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/token/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QrGenerateResponse> getQrTokenForUser(@PathVariable String userId) {
        String token = qrAuthService.getLatestMagicToken(userId);
        if (token == null) {
            return ResponseEntity.notFound().build();
        }
        String qrContent = FRONTEND_URL + "?token=" + token;

        return ResponseEntity.ok(QrGenerateResponse.builder()
                .token(token)
                .qrContent(qrContent)
                .build());
    }
}
