package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
  public ResponseEntity<UserResponse> createAccount(
      @Valid @RequestBody CreateUserRequest request, Authentication authentication) {
    return ResponseEntity.ok(authService.createAccount(request, authentication.getName()));
  }

  @GetMapping("/accounts")
  public ResponseEntity<com.example.auth.common.PageResponse<UserResponse>> getAllAccounts(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) com.example.auth.entity.Role role,
      @RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(
        authService.accounts(authentication.getName(), page, size, branchId, role, keyword));
  }

  @PatchMapping("/accounts/{id}/toggle-status")
  public ResponseEntity<UserResponse> toggleStatus(
      @PathVariable Long id, Authentication authentication) {
    return ResponseEntity.ok(authService.toggleStatus(id, authentication.getName()));
  }

  @GetMapping("/accounts/{id}/branches")
  public List<BranchAssignmentResponse> assignments(
      @PathVariable Long id, Authentication authentication) {
    return authService.assignments(id, authentication.getName());
  }

  @GetMapping("/accounts/{id}/branches/history")
  public List<AuthService.AssignmentEvent> assignmentHistory(
      @PathVariable Long id, Authentication authentication) {
    return authService.assignmentHistory(id, authentication.getName());
  }

  @PutMapping("/accounts/{id}/branches")
  public UserResponse assignBranches(
      @PathVariable Long id,
      @Valid @RequestBody BranchAssignmentsRequest request,
      Authentication authentication) {
    return authService.assignBranches(id, request, authentication.getName());
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
    return ResponseEntity.ok(authService.getByUsername(authentication.getName()));
  }

  @GetMapping("/teachers")
  public List<TeacherResponse> teachers(
      Authentication authentication, @RequestParam(required = false) Long branchId) {
    return authService.teachers(authentication.getName(), branchId);
  }

  @GetMapping("/teachers/{id}")
  public TeacherResponse teacher(
      Authentication authentication,
      @PathVariable Long id,
      @RequestParam(required = false) Long branchId) {
    return authService.teacher(authentication.getName(), id, branchId);
  }
}
