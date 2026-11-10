package com.example.academic.enrollment;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record EnrollmentDto(
    Long id,
    Long studentId,
    String studentName,
    String currentLevel,
    Long classId,
    String classCode,
    String className,
    LocalDateTime enrolledAt,
    Enrollment.StudyStatus studyStatus,
    Long branchId,
    Long billingEnrollmentId,
    Enrollment.TuitionDelivery tuitionDelivery,
    String tuitionDeliveryError,
    Long registeredBy,
    String registeredByName,
    long version,
    java.time.LocalDate reservedSince,
    java.time.LocalDate expectedReturnDate,
    Long completionResultId,
    Long completionResultRevision) {
  public static EnrollmentDto of(Enrollment e) {
    return of(e, e);
  }

  public static EnrollmentDto of(Enrollment e, Enrollment billingRoot) {
    return new EnrollmentDto(
        e.getId(),
        e.getStudent().getId(),
        e.getStudent().getFullName(),
        e.getStudent().getCurrentLevel(),
        e.getSchoolClass().getId(),
        e.getSchoolClass().getCode(),
        e.getSchoolClass().getName(),
        e.getEnrolledAt(),
        e.getStudyStatus(),
        e.getSchoolClass().getBranchId(),
        e.getBillingEnrollmentId(),
        billingRoot.getTuitionDelivery(),
        billingRoot.getTuitionDeliveryError(),
        e.getRegisteredBy(),
        e.getRegisteredByName(),
        e.getVersion(),
        e.getReservedSince(),
        e.getExpectedReturnDate(),
        e.getCompletionResultId(),
        e.getCompletionResultRevision());
  }

  public record Request(
      @NotNull @Positive Long studentId,
      @NotNull @Positive Long classId,
      @Positive Long approvalId) {
    public Request(Long studentId, Long classId) {
      this(studentId, classId, null);
    }
  }

  public record StatusRequest(
      @NotNull Enrollment.StudyStatus studyStatus,
      @NotBlank @Size(min = 3, max = 500) String reason,
      @NotNull @PositiveOrZero Long version,
      java.time.LocalDate expectedReturnDate,
      @Positive Long approvalId,
      Long finalResultId,
      Long finalResultRevision) {
    public StatusRequest(
        Enrollment.StudyStatus status,
        String reason,
        Long version,
        java.time.LocalDate date,
        Long approvalId) {
      this(status, reason, version, date, approvalId, null, null);
    }

    public StatusRequest(
        Enrollment.StudyStatus status,
        String reason,
        Long version,
        java.time.LocalDate expectedReturnDate) {
      this(status, reason, version, expectedReturnDate, null);
    }
  }

  public record TransferRequest(
      @NotNull @Positive Long newClassId,
      @NotBlank @Size(min = 3, max = 500) String reason,
      @NotNull @PositiveOrZero Long version,
      @Positive Long approvalId) {
    public TransferRequest(Long target, String reason, Long version) {
      this(target, reason, version, null);
    }
  }
}
