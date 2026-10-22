
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
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/accounts")
    public ResponseEntity<UserResponse> createAccount(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                authService.createAccount(request, authentication.getName())
        );
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<UserResponse>> getAllAccounts(
            Authentication authentication) {
        return ResponseEntity.ok(
                authService.getAllUsers(authentication.getName())
        );
    }

    @PatchMapping("/accounts/{id}/toggle-status")
    public ResponseEntity<UserResponse> toggleStatus(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                authService.toggleStatus(id, authentication.getName())
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication) {
        return ResponseEntity.ok(
                authService.getByUsername(authentication.getName())
        );
    }
}
