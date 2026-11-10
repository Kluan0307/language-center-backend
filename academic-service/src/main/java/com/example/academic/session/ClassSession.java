package com.example.academic.session;

import com.example.academic.common.BaseEntity;
import com.example.academic.schoolclass.SchoolClass;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "class_sessions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_session_source",
          columnNames = {"class_id", "source_date"}),
      @UniqueConstraint(name = "uk_session_makeup", columnNames = "makeup_for_id")
    },
    indexes = {
      @Index(name = "idx_session_date_teacher", columnList = "session_date,teacher_id,status"),
      @Index(name = "idx_session_date_room", columnList = "session_date,room_id,status")
    })
@Getter
@Setter
public class ClassSession extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_id", nullable = false)
  private SchoolClass schoolClass;

  @Column(nullable = false)
  private LocalDate sessionDate;

  private LocalTime startTime;
  private LocalTime endTime;
  private Long roomId;
  private String roomName;
  private Long plannedTeacherId;
  private String plannedTeacherName;
  private Long teacherId;
  private String teacherName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.SCHEDULED;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Kind kind = Kind.REGULAR;

  private LocalDate sourceDate;
  private Long sourceScheduleId;
  private Long makeupForId;
  private Long makeupSessionId;

  @Column(nullable = false)
  private boolean customized;

  @Column(length = 500)
  private String reason;

  @Version private long version;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void create() {
    createdAt = LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE);
    updatedAt = createdAt;
  }

  @PreUpdate
  void update() {
    updatedAt = LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE);
  }

  public enum Status {
    SCHEDULED,
    CANCELLED,
    COMPLETED
  }

  public enum Kind {
    REGULAR,
    MAKEUP,
    LEGACY
  }
}
