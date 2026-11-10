package com.example.academic.session;

import jakarta.validation.constraints.*;
import java.time.*;

public record SessionDto(
    Long id,
    long version,
    Long classId,
    String classCode,
    String className,
    Long branchId,
    LocalDate sessionDate,
    LocalTime startTime,
    LocalTime endTime,
    Long roomId,
    String roomName,
    Long plannedTeacherId,
    String plannedTeacherName,
    Long teacherId,
    String teacherName,
    ClassSession.Status status,
    ClassSession.Kind kind,
    LocalDate sourceDate,
    Long makeupForId,
    boolean customized,
    String reason,
    Long makeupSessionId,
    boolean classClosed,
    boolean canAttend) {
  public static SessionDto of(ClassSession s, boolean canAttend) {
    var c = s.getSchoolClass();
    return new SessionDto(
        s.getId(),
        s.getVersion(),
        c.getId(),
        c.getCode(),
        c.getName(),
        c.getBranchId(),
        s.getSessionDate(),
        s.getStartTime(),
        s.getEndTime(),
        s.getRoomId(),
        s.getRoomName(),
        s.getPlannedTeacherId(),
        s.getPlannedTeacherName(),
        s.getTeacherId(),
        s.getTeacherName(),
        s.getStatus(),
        s.getKind(),
        s.getSourceDate(),
        s.getMakeupForId(),
        s.isCustomized(),
        s.getReason(),
        s.getMakeupSessionId(),
        c.getStatus() == com.example.academic.schoolclass.SchoolClass.Status.FINISHED,
        canAttend);
  }

  public record Generate(@NotNull @Positive Long classId, LocalDate from, LocalDate to) {}

  public record Generated(int created, int changed) {}

  public record Edit(
      @NotNull LocalDate sessionDate,
      @NotNull LocalTime startTime,
      @NotNull LocalTime endTime,
      @NotNull @Positive Long roomId,
      @NotNull @Positive Long teacherId,
      @NotBlank @Size(min = 3, max = 500) String reason,
      @NotNull @PositiveOrZero Long version) {}

  public record TimeCorrection(@NotNull LocalTime startTime,@NotNull LocalTime endTime,@NotNull @PositiveOrZero Long version,@NotBlank @Size(min=3,max=500) String reason){}
  public record Action(
      @NotBlank @Size(min = 3, max = 500) String reason, @NotNull @PositiveOrZero Long version) {}

  public record Event(
      Long id,
      String action,
      String reason,
      String beforeState,
      String afterState,
      Long actorId,
      String actorName,
      LocalDateTime occurredAt) {
    public static Event of(SessionEvent e) {
      return new Event(
          e.getId(),
          e.getAction(),
          e.getReason(),
          e.getBeforeState(),
          e.getAfterState(),
          e.getActorId(),
          e.getActorName(),
          e.getOccurredAt());
    }
  }
}
