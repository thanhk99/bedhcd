package com.api.bedhcd.controller;

import com.api.bedhcd.dto.RegisterRequest;
import com.api.bedhcd.dto.UserResponse;
import com.api.bedhcd.entity.Role;
import com.api.bedhcd.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.api.bedhcd.service.VotingService votingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me/votes")
    public ResponseEntity<java.util.List<com.api.bedhcd.dto.response.VoteHistoryResponse>> getUserVoteHistory() {
        // Need to get current user ID
        String userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(votingService.getUserVoteHistory(userId));
    }

    @GetMapping("/me/login-history")
    public ResponseEntity<java.util.List<com.api.bedhcd.dto.response.LoginHistoryResponse>> getUserLoginHistory() {
        String userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(userService.getUserLoginHistory(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email) {
        UserResponse user = userService.updateProfile(fullName, email);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.changePassword(oldPassword, newPassword);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable String id, @RequestBody Set<Role> roles) {
        return ResponseEntity.ok(userService.updateRoles(id, roles));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String id, @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable String id) {
        String newPassword = userService.resetPassword(id);
        Map<String, String> response = new HashMap<>();
        response.put("newPassword", newPassword);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/votes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.api.bedhcd.dto.response.VoteHistoryResponse>> getUserVoteHistoryByAdmin(
            @PathVariable String id) {
        return ResponseEntity.ok(votingService.getUserVoteHistory(id));
    }

    @GetMapping("/{id}/login-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.api.bedhcd.dto.response.LoginHistoryResponse>> getUserLoginHistoryByAdmin(
            @PathVariable String id) {
        return ResponseEntity.ok(userService.getUserLoginHistory(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
