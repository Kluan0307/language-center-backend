package com.example.auth.dto;

import com.example.auth.entity.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Password không được để trống")
    private String password;

    @NotNull(message = "Vai trò không được để trống")
    private Role role; // ADMIN (chỉ Super Admin tạo), STAFF hoặc TEACHER

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;

    // Các trường riêng cho Teacher (role = TEACHER)
    private String specialization;
    private String qualification;
    private String experience;
    private String certificate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    private String workStatus;

    private Long branchId;   // bắt buộc khi Super Admin tạo; Admin tạo thì bị bỏ qua (ép theo chi nhánh của Admin)
    private String position; // chỉ dùng cho STAFF
}