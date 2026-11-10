package com.example.academic.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDto {
  private AttendanceDto() {}

  public record Item(
      @NotNull @Positive Long enrollmentId,
      @NotNull Attendance.Status status,
      Boolean hasPermission) {}

  public record Bulk(
      @NotNull @Positive Long classId,
      @NotNull LocalDate sessionDate,
      @NotNull List<@NotNull @Valid Item> records,
      @Positive Long sessionId,
      @PositiveOrZero Long version,
      @Size(max = 500) String reason) {
    public Bulk(Long classId, LocalDate sessionDate, List<Item> records) {
      this(classId, sessionDate, records, null, null, null);
    }
  }

  public record Row(
      Long enrollmentId,
      Long studentId,
      String studentName,
      String currentLevel,
      Attendance.Status status,
      boolean hasPermission,
      boolean saved,
      Long recordedBy,
      String recordedByName) {}

  public record Sheet(
      Long classId,
      String className,
      LocalDate sessionDate,
      List<Row> records,
      Long sessionId,
      long version,
      com.example.academic.session.ClassSession.Status sessionStatus,
      java.time.LocalTime startTime,
      java.time.LocalTime endTime,
      String teacherName,
      boolean canEdit,
      boolean correctionRequired,
      boolean canViewClassHistory) {}

  public record Change(
      Long id,
      String studentName,
      Attendance.Status oldStatus,
      Attendance.Status newStatus,
      Boolean oldPermission,
      boolean newPermission,
      String previousRecordedByName,
      String changedByName,
      java.time.LocalDateTime changedAt,
      String reason) {
    public static Change of(AttendanceChange a) {
      return new Change(
          a.getId(),
          a.getStudentName(),
          a.getOldStatus(),
          a.getNewStatus(),
          a.getOldPermission(),
          a.isNewPermission(),
          a.getPreviousRecordedByName(),
          a.getChangedByName(),
          a.getChangedAt(),
          a.getReason());
    }
  }

  public record History(LocalDate sessionDate, long present, long absent, long late) {}

  public record Summary(
      Long enrollmentId,
      Long studentId,
      String studentName,
      long present,
      long absent,
      long late,
      long totalSessions,
      BigDecimal participationRate) {}
}
