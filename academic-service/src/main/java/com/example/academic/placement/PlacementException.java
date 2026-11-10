package com.example.academic.placement;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "placement_exceptions",
    indexes = @Index(name = "ix_placement_exception_branch", columnList = "branch_id,id"))
@Getter
@Setter
public class PlacementException {
  public enum Status {
    REQUESTED,
    APPROVED,
    REJECTED,
    CONSUMED,
    INVALIDATED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, updatable = false)
  private Long studentId;

  @Column(nullable = false, updatable = false)
  private Long classId;

  @Column(nullable = false, updatable = false)
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private PlacementDto.Intent intent;

  @Column(updatable = false)
  private Long sourceEnrollmentId;

  @Column(updatable = false)
  private Long sourceVersion;

  @Column(updatable = false)
  private Long policyId;

  @Column(nullable = false, updatable = false)
  private long classRevision;

  @Column(updatable = false)
  private Long assessmentId;

  @Column(updatable = false)
  private Long assessmentVersion;

  @Column(nullable = false, updatable = false, columnDefinition = "longtext")
  private String contextSnapshot;

  @Column(nullable = false, length = 64, updatable = false)
  private String fingerprint;

  @Column(nullable = false, length = 40, updatable = false)
  private String academicStatus;

  @Column(nullable = false, length = 500, updatable = false)
  private String requestedReason;

  @Column(nullable = false, updatable = false)
  private Long requestedBy;

  @Column(nullable = false, updatable = false)
  private String requestedByName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime requestedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.REQUESTED;

  @Column(length = 500)
  private String decisionReason;

  private Long decidedBy;
  private String decidedByName;
  private LocalDateTime decidedAt;
  private Long consumedEnrollmentId;
  private LocalDateTime consumedAt;
}
