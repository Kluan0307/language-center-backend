package com.example.academic.duration;

import jakarta.validation.constraints.*;
import java.util.*;

public final class DurationDto {
  public static final int MAX_MINUTES = 600000;

  public record CourseView(
      Long courseId,
      long revision,
      Integer expectedMinutes,
      Integer legacyDuration,
      boolean verified,
      boolean canManage) {}

  public record CourseRequest(
      @NotNull @PositiveOrZero Long expectedRevision,
      @NotNull @Min(1) @Max(MAX_MINUTES) Integer expectedMinutes,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record ClassView(
      Long classId,
      long revision,
      Integer expectedMinutes,
      boolean verified,
      boolean canManage,
      CourseView course) {}

  public record ClassRequest(
      @NotNull @PositiveOrZero Long expectedRevision,
      @NotNull ClassDurationPlan.Source source,
      @Positive Long expectedCourseRevision,
      @Min(1) @Max(MAX_MINUTES) Integer expectedMinutes,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record Warning(String code, String message) {}

  public record Progress(
      Long classId,
      String classCode,
      long planRevision,
      Integer expectedMinutes,
      Long scheduledMinutes,
      Long taughtMinutes,
      long knownScheduledMinutes,
      long knownTaughtMinutes,
      long scheduledSessions,
      long completedSessions,
      long cancelledSessions,
      long unfinishedSessions,
      long overdueSessions,
      long unknownActiveSessions,
      long unknownCompletedSessions,
      long invalidMakeupSessions,
      String dataState,
      Long scheduledDifferenceMinutes,
      Long remainingTaughtMinutes,
      boolean timeReadyForClosure,
      List<Warning> warnings) {}
}
