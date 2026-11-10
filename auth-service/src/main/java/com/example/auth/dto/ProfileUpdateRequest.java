package com.example.auth.dto;

import jakarta.validation.constraints.*;

public record ProfileUpdateRequest(
    @Email @Size(max = 100) String email,
    @Size(max = 15) @Pattern(regexp = "^$|[+0-9() .-]{6,15}") String phone) {
  public ProfileUpdateRequest {
    email = email == null || email.isBlank() ? null : email.trim();
    phone = phone == null || phone.isBlank() ? null : phone.trim();
  }
}
