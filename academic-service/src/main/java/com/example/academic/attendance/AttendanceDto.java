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
      @NotNull @PastOrPresent LocalDate sessionDate,
      @NotNull List<@NotNull @Valid Item> records) {}

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

  public record Sheet(Long classId, String className, LocalDate sessionDate, List<Row> records) {}

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
