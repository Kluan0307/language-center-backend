package com.example.academic.placement;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "enrollment_admissions",
    indexes = @Index(name = "ix_enrollment_admission", columnList = "enrollment_id,id"))
@Getter
@Setter
public class EnrollmentAdmission {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long enrollmentId;

  @Column(nullable = false, updatable = false)
  private Long studentId;

  @Column(nullable = false, updatable = false)
  private Long classId;

  @Column(updatable = false)
  private Long policyId;

  @Column(nullable = false, updatable = false)
  private long classRevision;

  @Column(updatable = false)
  private Long assessmentId;

  @Column(updatable = false)
  private Long assessmentVersion;

  @Column(updatable = false)
  private Long approvalId;

  @Column(updatable = false)
  private Long reusedDecisionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private PlacementDto.Intent intent;

  @Column(nullable = false, length = 40, updatable = false)
  private String academicStatus;

  @Column(nullable = false, length = 64, updatable = false)
  private String fingerprint;

  @Column(nullable = false, columnDefinition = "longtext", updatable = false)
  private String policySnapshot;

  @Column(nullable = false, columnDefinition = "longtext", updatable = false)
  private String assessmentSnapshot;

  @Column(nullable = false, updatable = false)
  private Long actorId;

  @Column(nullable = false, updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime acceptedAt;
}
