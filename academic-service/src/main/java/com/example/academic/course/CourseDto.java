package com.example.academic.course;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CourseDto(
    Long id,
    String code,
    String name,
    String type,
    String level,
    Integer duration,
    BigDecimal tuitionFee,
    String content,
    String entryRequirement,
    Integer maxStudents,
    Course.Status status,
    Long branchId,
    Integer expectedMinutes,
    long durationRevision) {
  public static CourseDto of(Course c) {
    return new CourseDto(
        c.getId(),
        c.getCode(),
        c.getName(),
        c.getType(),
        c.getLevel(),
        c.getDuration(),
        c.getTuitionFee(),
        c.getContent(),
        c.getEntryRequirement(),
        c.getMaxStudents(),
        c.getStatus(),
        c.getBranchId(),
        c.getExpectedMinutes(),
        c.getDurationRevision());
  }

  public record Request(
      @NotBlank @Size(max = 255) String code,
      @NotBlank @Size(max = 255) String name,
      @Size(max = 255) String type,
      @Size(max = 255) String level,
      @Positive Integer duration,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
          BigDecimal tuitionFee,
      String content,
      String entryRequirement,
      @NotNull @Positive Integer maxStudents,
      Long branchId,
      @Min(1) @Max(com.example.academic.duration.DurationDto.MAX_MINUTES) Integer expectedMinutes) {
    public Request(
        String code,
        String name,
        String type,
        String level,
        Integer duration,
        BigDecimal fee,
        String content,
        String entry,
        Integer maxStudents,
        Long branchId) {
      this(code, name, type, level, duration, fee, content, entry, maxStudents, branchId, null);
    }
  }

  public record StatusRequest(@NotNull Course.Status status) {}
}
