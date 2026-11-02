package com.example.academic.student;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public record StudentDto(
    Long id,
    String fullName,
    LocalDate dateOfBirth,
    Student.Gender gender,
    String email,
    String phone,
    String address,
    String contactPerson,
    String currentLevel,
    LocalDate registeredAt,
    Student.Status status,
    Long branchId) {
  public static StudentDto of(Student s) {
    return new StudentDto(
        s.getId(),
        s.getFullName(),
        s.getDateOfBirth(),
        s.getGender(),
        s.getEmail(),
        s.getPhone(),
        s.getAddress(),
        s.getContactPerson(),
        s.getCurrentLevel(),
        s.getRegisteredAt(),
        s.getStatus(),
        s.getBranchId());
  }

  public record Request(
      @NotBlank @Size(max = 255) String fullName,
      @PastOrPresent LocalDate dateOfBirth,
      Student.Gender gender,
      @Email @Size(max = 255) String email,
      @NotBlank @Size(max = 255) String phone,
      @Size(max = 255) String address,
      @Size(max = 255) String contactPerson,
      @Size(max = 255) String currentLevel,
      Long branchId) {}

  public record StatusRequest(@NotNull Student.Status status) {}

  public record Registration(
      Long id,
      Long classId,
      String classCode,
      String className,
      LocalDateTime enrolledAt,
      String studyStatus) {}

  public record Detail(StudentDto student, List<Registration> enrollments) {}
}
