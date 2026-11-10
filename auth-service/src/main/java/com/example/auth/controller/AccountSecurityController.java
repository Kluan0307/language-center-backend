package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AccountSecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AccountSecurityController {
  private final AccountSecurityService service;

  @PostMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(
      @Valid @RequestBody ChangePasswordRequest request, Authentication auth) {
    service.changePassword(auth.getName(), request);
  }

  @PostMapping("/me/logout-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logoutAll(Authentication auth) {
    service.logoutAll(auth.getName());
  }

  @PostMapping("/accounts/{id}/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(
      @PathVariable Long id,
      @Valid @RequestBody ResetPasswordRequest request,
      Authentication auth) {
    service.resetPassword(auth.getName(), id, request);
  }

  @PatchMapping("/me/profile")
  public UserResponse updateProfile(
      @Valid @RequestBody ProfileUpdateRequest request, Authentication auth) {
    return service.updateProfile(auth.getName(), request);
  }
}
