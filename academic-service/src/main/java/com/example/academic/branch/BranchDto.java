package com.example.academic.branch;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record BranchDto(
    Long id,
    String code,
    String name,
    String address,
    String phone,
    Branch.Status status,
    LocalDateTime createdAt) {
  public static BranchDto of(Branch b) {
    return new BranchDto(
        b.getId(),
        b.getCode(),
        b.getName(),
        b.getAddress(),
        b.getPhone(),
        b.getStatus(),
        b.getCreatedAt());
  }

  public record Request(
      @NotBlank @Size(max = 255) String code,
      @NotBlank @Size(max = 255) String name,
      @Size(max = 255) String address,
      @NotBlank @Size(max = 255) String phone) {}
}
