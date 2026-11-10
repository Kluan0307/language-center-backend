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
    Long branchId,
    long version,
    LocalDateTime nextContactAt,
    boolean overdue) {
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
        l.getBranchId(),
        l.getVersion(),
        l.getNextContactAt(),
        l.active()
            && l.getNextContactAt() != null
            && l.getNextContactAt()
                .isBefore(LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE)));
  }

  public record Request(
      @NotBlank @Size(max = 255) String fullName,
      @NotBlank @Size(max = 255) String phone,
      @Email @Size(max = 255) String email,
      @Size(max = 255) String source,
      Long interestedCourseId,
      @Size(max = 255) String desiredLevel,
      @Size(max = 2000) String note,
      Long branchId,
      @PositiveOrZero Long version) {
    public Request(
        String fullName,
        String phone,
        String email,
        String source,
        Long interestedCourseId,
        String desiredLevel,
        String note,
        Long branchId) {
      this(fullName, phone, email, source, interestedCourseId, desiredLevel, note, branchId, null);
    }
  }

  public record StatusRequest(
      @NotNull Lead.Status status,
      @NotBlank @Size(min = 3, max = 2000) String note,
      @NotNull @PositiveOrZero Long version) {}

  public enum Channel {
    CALL,
    EMAIL,
    MESSAGE,
    IN_PERSON,
    NOTE
  }

  public record Interaction(
      @NotNull Channel channel,
      @NotBlank @Size(min = 3, max = 2000) String note,
      @NotNull Lead.Status status,
      LocalDateTime nextContactAt,
      @NotNull @PositiveOrZero Long version) {}

  public record Assignment(
      @NotNull @Positive Long accountId,
      @NotBlank @Size(min = 3, max = 2000) String reason,
      @NotNull @PositiveOrZero Long version) {}

  public record Conversion(@NotNull @PositiveOrZero Long version) {}
}
