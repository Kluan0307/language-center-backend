package com.example.academic.schoolclass;

import com.example.academic.schedule.ScheduleDto;
import com.example.academic.schedule.ScheduleDto.Slot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record SchoolClassDto(
    Long id,
    String code,
    String name,
    Long courseId,
    String courseName,
    Long teacherId,
    String teacherName,
    Long roomId,
    String roomName,
    LocalDate startDate,
    LocalDate endDate,
    Integer maxSize,
    Integer currentSize,
    SchoolClass.Status status,
    boolean statusManuallySet,
    Long branchId) {
  public static SchoolClassDto of(SchoolClass c) {
    return new SchoolClassDto(
        c.getId(),
        c.getCode(),
        c.getName(),
        c.getCourse().getId(),
        c.getCourse().getName(),
        c.getTeacherId(),
        c.getTeacherName(),
        c.getRoom().getId(),
        c.getRoom().getName(),
        c.getStartDate(),
        c.getEndDate(),
        c.getMaxSize(),
        c.getCurrentSize(),
        c.effectiveStatus(),
        c.isStatusManuallySet(),
        c.getBranchId());
  }

  public record Request(
      @NotBlank @Size(max = 255) String code,
      @NotBlank @Size(max = 255) String name,
      @NotNull @Positive Long courseId,
      @NotNull @Positive Long teacherId,
      @NotBlank @Size(max = 255) String teacherName,
      @NotNull @Positive Long roomId,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate,
      @NotNull @Positive Integer maxSize,
      Long branchId,
      List<@NotNull @Valid Slot> schedules) {}

  public record StatusRequest(@NotNull SchoolClass.Status status) {}

  public record Detail(SchoolClassDto schoolClass, List<ScheduleDto> schedules) {}
}
