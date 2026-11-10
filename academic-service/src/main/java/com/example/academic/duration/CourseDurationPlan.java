package com.example.academic.duration;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "course_duration_plans",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_course_duration_revision",
            columnNames = {"course_id", "revision"}))
@Getter
@Setter
public class CourseDurationPlan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long courseId;

  @Column(nullable = false, updatable = false)
  private long revision;

  @Column(nullable = false, updatable = false)
  private Integer expectedMinutes;

  @Column(nullable = false, updatable = false)
  private Long actorId;

  @Column(nullable = false, updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime occurredAt;

  @Column(nullable = false, length = 500, updatable = false)
  private String reason;
}
