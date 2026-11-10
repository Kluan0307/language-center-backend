package com.example.academic.duration;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "class_duration_plans",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_class_duration_revision",
            columnNames = {"class_id", "revision"}))
@Getter
@Setter
public class ClassDurationPlan {
  public enum Source {
    COURSE,
    CUSTOM,
    UNVERIFIED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long classId;

  @Column(nullable = false, updatable = false)
  private long revision;

  @Column(updatable = false)
  private Integer expectedMinutes;

  @Column(nullable = false, updatable = false)
  private Long courseId;

  @Column(updatable = false)
  private Long courseRevision;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private Source source;

  @Column(nullable = false, updatable = false)
  private Long actorId;

  @Column(nullable = false, updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime occurredAt;

  @Column(nullable = false, length = 500, updatable = false)
  private String reason;
}
