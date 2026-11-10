package com.example.auth.dto;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
    @NotBlank @Size(min = 8, max = 72) String temporaryPassword,
    @NotBlank @Size(max = 72) String confirmPassword,
    @NotBlank @Size(min = 3, max = 255) String reason) {
  @Override
  public String toString() {
    return "ResetPasswordRequest[REDACTED]";
  }
}
