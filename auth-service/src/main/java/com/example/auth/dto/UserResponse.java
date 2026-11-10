package com.example.auth.dto;

import com.example.auth.entity.Role;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
  private boolean mustChangePassword;
  private LocalDateTime passwordChangedAt;
  private Long accountId;
  private Long profileId;
  private String username;
  private Role role;
  private Boolean status;
  private Long branchId;
  private java.util.List<Long> branchIds;
  private LocalDateTime createdAt;
  private LocalDateTime lastLogin;

  private String fullName;
  private String email;
  private String phone;

  // Các trường riêng cho Staff
  private String position;

  // Các trường riêng cho Teacher nếu role = TEACHER
  private String specialization;
  private String qualification;
  private String experience;
  private String certificate;
  private LocalDate hireDate;
  private String workStatus;
}
