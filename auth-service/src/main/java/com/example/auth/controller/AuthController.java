package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/accounts")
    public ResponseEntity<UserResponse> createAccount(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.createAccount(request));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<UserResponse>> getAllAccounts() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PatchMapping("/accounts/{id}/toggle-status")
    public ResponseEntity<UserResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(authService.toggleStatus(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(authService.getByUsername(username));
    }
}