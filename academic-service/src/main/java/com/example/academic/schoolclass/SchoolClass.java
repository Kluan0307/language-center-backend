package com.example.academic.schoolclass;

import com.example.academic.common.BaseEntity;
import com.example.academic.course.Course;
import com.example.academic.room.Room;
import com.example.academic.schedule.Schedule;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import lombok.*;

@Entity
@Table(
    name = "school_classes",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_class_code_branch",
            columnNames = {"code", "branch_id"}))
@Getter
@Setter
public class SchoolClass extends BaseEntity {
  private Integer expectedMinutes;

  @Column(nullable = false)
  private long durationRevision;

  private Long admissionPolicyId;

  @Column(nullable = false)
  private long admissionRevision;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(nullable = false)
  private Long teacherId;

  @Column(nullable = false)
  private String teacherName;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  private LocalDate sessionEndDate;

  @Column(nullable = false)
  private boolean sessionsInitialized;

  @Column(nullable = false)
  private Integer maxSize;

  @Column(nullable = false)
  private Integer currentSize = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.UPCOMING;

  @Column(nullable = false)
  private boolean statusManuallySet;

  @Column(nullable = false)
  private Long branchId;

  @OneToMany(mappedBy = "schoolClass", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<Schedule> schedules = new ArrayList<>();

  public enum Status {
    UPCOMING,
    ONGOING,
    FULL,
    FINISHED
  }

  @Transient
  public Status effectiveStatus() {
    if (status == Status.FINISHED) return Status.FINISHED;
    return dateStatus();
  }

  @Transient
  public LocalDate effectiveEndDate() {
    return sessionEndDate != null && sessionEndDate.isAfter(endDate) ? sessionEndDate : endDate;
  }

  @Transient
  public Status dateStatus() {
    LocalDate today = LocalDate.now(com.example.academic.common.BusinessTime.ZONE);
    return today.isAfter(effectiveEndDate())
        ? Status.FINISHED
        : today.isBefore(startDate) ? Status.UPCOMING : Status.ONGOING;
  }
}
