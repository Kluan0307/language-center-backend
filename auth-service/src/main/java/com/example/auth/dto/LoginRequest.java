package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

  @NotBlank(message = "Username không được để trống")
  @jakarta.validation.constraints.Size(max = 50)
  private String username;

  @NotBlank(message = "Password không được để trống")
  @jakarta.validation.constraints.Size(max = 72)
  @lombok.ToString.Exclude
  private String password;
}
