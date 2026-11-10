package com.example.auth.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public record TeacherProfileDto(
    Long profileId,
    Long accountId,
    String username,
    boolean accountActive,
    List<Long> branchIds,
    boolean canManage,
    long version,
    String fullName,
    String email,
    String phone,
    String specialization,
    String qualification,
    String experience,
    String certificate,
    LocalDate hireDate,
    String workStatus) {
  public record Update(
      @NotNull @PositiveOrZero Long version,
      @NotBlank @Size(max = 100) String fullName,
      @Email @Size(max = 100) String email,
      @Size(max = 15) @Pattern(regexp = "^$|[+0-9() .-]{6,15}") String phone,
      @Size(max = 100) String specialization,
      @Size(max = 100) String qualification,
      @Size(max = 50) String experience,
      @Size(max = 255) String certificate,
      LocalDate hireDate) {}

  public record WorkStatus(
      @NotNull @PositiveOrZero Long version,
      @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String workStatus,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record Event(
      Long id,
      String action,
      String beforeState,
      String afterState,
      String reason,
      Long actorId,
      String actorName,
      long teacherVersion,
      LocalDateTime occurredAt) {}

  public record Responsibility(
      String kind,
      Long id,
      Long branchId,
      Long classId,
      String classCode,
      String label,
      LocalDate date,
      String status,
      Long version) {}
}
