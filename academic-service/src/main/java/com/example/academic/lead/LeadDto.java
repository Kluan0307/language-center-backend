package com.example.academic.lead;

import com.example.academic.student.StudentDto;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record LeadDto(
    Long id,
    String fullName,
    String phone,
    String email,
    String source,
    Long interestedCourseId,
    String interestedCourseName,
    String desiredLevel,
    Lead.Status status,
    String note,
    Long assignedTo,
    String assignedToName,
    StudentDto convertedStudent,
    LocalDateTime createdAt,
    LocalDateTime lastContactedAt,
    Long branchId) {
  public static LeadDto of(Lead l) {
    return new LeadDto(
        l.getId(),
        l.getFullName(),
        l.getPhone(),
        l.getEmail(),
        l.getSource(),
        l.getInterestedCourse() == null ? null : l.getInterestedCourse().getId(),
        l.getInterestedCourse() == null ? null : l.getInterestedCourse().getName(),
        l.getDesiredLevel(),
        l.getStatus(),
        l.getNote(),
        l.getAssignedTo(),
        l.getAssignedToName(),
        l.getConvertedStudent() == null ? null : StudentDto.of(l.getConvertedStudent()),
        l.getCreatedAt(),
        l.getLastContactedAt(),
        l.getBranchId());
  }

  public record Request(
      @NotBlank @Size(max = 255) String fullName,
      @NotBlank @Size(max = 255) String phone,
      @Email @Size(max = 255) String email,
      @Size(max = 255) String source,
      Long interestedCourseId,
      @Size(max = 255) String desiredLevel,
      String note,
      Long branchId) {}

  public record StatusRequest(@NotNull Lead.Status status, String note) {}
}
