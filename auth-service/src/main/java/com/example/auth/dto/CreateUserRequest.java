package com.example.auth.dto;

import com.example.auth.entity.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

  @NotBlank(message = "Username không được để trống")
  @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_.@-]{3,50}")
  private String username;

  @NotBlank(message = "Password không được để trống")
  @jakarta.validation.constraints.Size(min = 8, max = 72)
  @lombok.ToString.Exclude
  private String password;

  @NotNull(message = "Vai trò không được để trống")
  private Role role; // ADMIN (chỉ Super Admin tạo), STAFF hoặc TEACHER

  @NotBlank(message = "Họ tên không được để trống")
  @jakarta.validation.constraints.Size(max = 100)
  private String fullName;

  @Email(message = "Email không hợp lệ")
  @jakarta.validation.constraints.Size(max = 100)
  private String email;

  @jakarta.validation.constraints.Size(max = 15)
  private String phone;

  // Các trường riêng cho Teacher (role = TEACHER)
  @jakarta.validation.constraints.Size(max = 100)
  private String specialization;

  @jakarta.validation.constraints.Size(max = 100)
  private String qualification;

  @jakarta.validation.constraints.Size(max = 50)
  private String experience;

  @jakarta.validation.constraints.Size(max = 255)
  private String certificate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate hireDate;

  @jakarta.validation.constraints.Pattern(regexp = "ACTIVE|INACTIVE")
  private String workStatus;

  private Long branchId; // Selected assigned branch; default is the current/primary branch.

  @jakarta.validation.constraints.Size(max = 100)
  private String position; // chỉ dùng cho STAFF
}
