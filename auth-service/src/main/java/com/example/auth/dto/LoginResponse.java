package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
  private boolean mustChangePassword;
  @lombok.ToString.Exclude private String token;
  private Long accountId;
  private Long profileId;
  private String username;
  private String role;
  private String fullName;
  private Long branchId;
  private java.util.List<Long> branchIds;
}
