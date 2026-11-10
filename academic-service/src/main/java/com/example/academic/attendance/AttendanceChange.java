package com.example.academic.attendance;

import com.example.academic.common.BaseEntity;
import com.example.academic.session.ClassSession;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "attendance_changes",
    indexes = @Index(name = "idx_attendance_change_session", columnList = "session_id,id"))
@Getter
@Setter
public class AttendanceChange extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private ClassSession session;

  @Column(nullable = false)
  private Long enrollmentId;

  @Column(nullable = false)
  private String studentName;

  @Enumerated(EnumType.STRING)
  private Attendance.Status oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Attendance.Status newStatus;

  private Boolean oldPermission;

  @Column(nullable = false)
  private boolean newPermission;

  private Long previousRecordedBy;
  private String previousRecordedByName;

  @Column(nullable = false)
  private Long changedBy;

  @Column(nullable = false)
  private String changedByName;

  @Column(nullable = false)
  private LocalDateTime changedAt;

  @Column(nullable = false, length = 500)
  private String reason;
}
