package com.example.academic.assessment;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "assessment_events",
    indexes = @Index(name = "idx_assessment_event", columnList = "assessment_id,id"))
@Getter
@Setter
public class AssessmentEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long assessmentId;

  @Column(nullable = false, updatable = false)
  private String action;

  @Column(columnDefinition = "longtext", nullable = false, updatable = false)
  private String beforeState;

  @Column(columnDefinition = "longtext", nullable = false, updatable = false)
  private String afterState;

  @Column(length = 500, updatable = false)
  private String reason;

  @Column(nullable = false, updatable = false)
  private Long actorId;

  @Column(nullable = false, updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime occurredAt;

  @Column(nullable = false, updatable = false)
  private long assessmentVersion;
}
