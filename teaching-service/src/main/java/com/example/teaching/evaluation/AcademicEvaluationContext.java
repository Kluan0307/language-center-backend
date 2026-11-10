package com.example.teaching.evaluation;

import java.time.*;
import java.util.*;

public record AcademicEvaluationContext(
    Long classId,
    Long branchId,
    LocalDateTime observedAt,
    List<Learner> enrollments,
    List<Session> sessions,
    List<Mark> attendance,
    Duration duration) {
  public record Period(LocalDate activeFrom, LocalDate activeUntil) {
    public boolean includes(LocalDate date) {
      return activeFrom != null
          && !date.isBefore(activeFrom)
          && (activeUntil == null || date.isBefore(activeUntil));
    }
  }

  public record Learner(
      Long enrollmentId,
      Long studentId,
      String studentName,
      String studyStatus,
      List<Period> periods) {}

  public record Session(
      Long id, LocalDate sessionDate, LocalTime startTime, LocalTime endTime, String status) {}

  public record Mark(
      Long enrollmentId,
      Long sessionId,
      LocalDate sessionDate,
      String status,
      boolean hasPermission) {}

  public record Duration(
      Integer expectedMinutes,
      long planRevision,
      Long taughtMinutes,
      Long scheduledMinutes,
      long unknownActiveSessions,
      long unfinishedSessions,
      boolean timeReadyForClosure) {}
}
