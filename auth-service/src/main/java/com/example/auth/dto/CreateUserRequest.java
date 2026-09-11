package com.example.auth.dto;

import com.example.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Password không được để trống")
    private String password;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;

    @NotNull(message = "Vai trò không được để trống")
    private Role role; // chỉ nhận STAFF hoặc TEACHER — chặn tạo ADMIN ở tầng service
}