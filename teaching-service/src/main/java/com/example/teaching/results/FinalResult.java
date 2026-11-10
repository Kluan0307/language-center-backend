package com.example.teaching.results;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "final_results",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"enrollment_id", "revision"}),
      @UniqueConstraint(columnNames = "request_id")
    })
@Getter
@Setter
public class FinalResult {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long classId;
  private Long branchId;
  private Long enrollmentId;
  private Long studentId;
  private long revision;
  private long policyRevision;
  private long academicSourceRevision;
  private long teachingSourceRevision;

  @Column(nullable = false, length = 36)
  private String requestId;

  @Column(nullable = false, length = 64)
  private String fingerprint;

  @Column(nullable = false, length = 20)
  private String outcome;

  @Column(nullable = false, length = 20)
  private String state;

  @Column(nullable = false, length = 500)
  private String reason;

  private Long confirmedBy;
  private String confirmedByName;
  private LocalDateTime confirmedAt;

  @Lob
  @Column(columnDefinition = "LONGTEXT", nullable = false)
  private String snapshot;

  private Long reopenedBy;
  private String reopenedByName;
  private LocalDateTime reopenedAt;

  @Column(length = 500)
  private String reopenReason;
}
