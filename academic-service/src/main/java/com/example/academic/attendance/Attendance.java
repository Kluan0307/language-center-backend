package com.example.academic.attendance;

import com.example.academic.common.BaseEntity;
import com.example.academic.enrollment.Enrollment;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(
    name = "attendances",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_attendance_enrollment_date",
            columnNames = {"enrollment_id", "session_date"}))
@Getter
@Setter
public class Attendance extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;

  @Column(nullable = false)
  private LocalDate sessionDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @Column(nullable = false)
  private boolean hasPermission;

  @Column(nullable = false)
  private Long recordedBy;

  @Column(nullable = false)
  private String recordedByName;

  public enum Status {
    PRESENT,
    ABSENT,
    LATE
  }
}
