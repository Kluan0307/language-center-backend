package com.example.academic.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.*;

public record ScheduleDto(
    Long id,
    Long classId,
    String classCode,
    String className,
    Long roomId,
    String roomName,
    Long teacherId,
    String teacherName,
    Long branchId,
    DayOfWeek dayOfWeek,
    @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @JsonFormat(pattern = "HH:mm") LocalTime endTime) {
  public static ScheduleDto of(Schedule s) {
    var c = s.getSchoolClass();
    return new ScheduleDto(
        s.getId(),
        c.getId(),
        c.getCode(),
        c.getName(),
        c.getRoom().getId(),
        c.getRoom().getName(),
        c.getTeacherId(),
        c.getTeacherName(),
        c.getBranchId(),
        s.getDayOfWeek(),
        s.getStartTime(),
        s.getEndTime());
  }

  public record Slot(
      @NotNull DayOfWeek dayOfWeek,
      @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
      @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime) {}

  public record Request(
      @NotNull @Positive Long classId,
      @NotNull DayOfWeek dayOfWeek,
      @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
      @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime) {}

  public record ConflictRequest(
      Long classId,
      Long excludeScheduleId,
      @NotNull DayOfWeek dayOfWeek,
      @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
      @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
      Long branchId,
      Long teacherId,
      Long roomId,
      LocalDate startDate,
      LocalDate endDate) {}

  public record Conflict(
      Long scheduleId,
      Long classId,
      String classCode,
      String className,
      String reason,
      DayOfWeek dayOfWeek,
      @JsonFormat(pattern = "HH:mm") LocalTime startTime,
      @JsonFormat(pattern = "HH:mm") LocalTime endTime) {}
}
