package com.api.bedhcd.controller;

import com.api.bedhcd.dto.AuthResponse;
import com.api.bedhcd.dto.LoginRequest;
import com.api.bedhcd.dto.RegisterRequest;
import com.api.bedhcd.dto.request.AdminQrGenerateRequest;
import com.api.bedhcd.dto.request.MagicLoginRequest;
import com.api.bedhcd.service.AuthService;
import com.api.bedhcd.service.QrAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final QrAuthService qrAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request, httpRequest, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.refreshAccessToken(request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Logged out successfully");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/qr/generate")
    public ResponseEntity<Map<String, String>> generateQrCode(@RequestBody AdminQrGenerateRequest request) {
        Map<String, String> result = new HashMap<>();
        result.put("token", qrAuthService.generateMagicToken(request.getUserId(), request.getExpiresAt()));
        result.put("qrContent", "http://dhcd.vix.local:3000/login?token=" + result.get("token"));
        return ResponseEntity.ok(result);
    }

    @GetMapping("qr/token/{userId}")
    public ResponseEntity<Map<String, String>> getLatestQrToken(@PathVariable String userId) {
        Map<String, String> result = new HashMap<>();
        result.put("token", qrAuthService.getLatestMagicToken(userId));
        result.put("qrContent", "http://dhcd.vix.local:3000/login?token=" + result.get("token"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/qr/magic-login")
    public ResponseEntity<AuthResponse> magicLogin(
            @Valid @RequestBody MagicLoginRequest request) {
        AuthResponse authResponse = qrAuthService.magicLogin(request);
        return ResponseEntity.ok(authResponse);
    }
}
