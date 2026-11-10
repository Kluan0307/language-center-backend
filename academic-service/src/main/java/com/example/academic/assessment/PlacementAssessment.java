package com.example.academic.assessment;

import com.example.academic.student.Student;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "placement_assessments",
    indexes =
        @Index(
            name = "idx_assessment_student_program",
            columnList = "student_id,program_id,status"),
    uniqueConstraints =
        @UniqueConstraint(name = "uk_assessment_current", columnNames = "current_key"))
@Getter
@Setter
public class PlacementAssessment {
  public enum Method {
    TEST,
    INTERVIEW,
    CERTIFICATE,
    OTHER
  }

  public enum Status {
    DRAFT,
    CONFIRMED,
    SUPERSEDED,
    REVOKED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @Column(nullable = false)
  private Long branchId;

  @Column(nullable = false)
  private Long programId;

  @Column(nullable = false, length = 50)
  private String programCode;

  @Column(nullable = false)
  private String programName;

  @Column(nullable = false)
  private Long levelId;

  @Column(nullable = false, length = 50)
  private String levelCode;

  @Column(nullable = false)
  private String levelName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Method method;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  @Column(nullable = false)
  private LocalDate assessedOn;

  @Column(nullable = false)
  private String assessorName;

  @Column(nullable = false, length = 2000)
  private String source;

  @Column(precision = 12, scale = 4)
  private BigDecimal score;

  @Column(precision = 12, scale = 4)
  private BigDecimal maxScore;

  @Column(nullable = false, updatable = false)
  private Long recordedBy;

  @Column(nullable = false, updatable = false)
  private String recordedByName;

  private Long confirmedBy;
  private String confirmedByName;
  private LocalDateTime confirmedAt;
  private Long supersedesId;
  private Long supersededById;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(
      name = "current_key",
      insertable = false,
      updatable = false,
      length = 90,
      columnDefinition =
          "varchar(90) generated always as (case when status='CONFIRMED' then"
              + " concat(student_id,':',program_id) else null end)")
  private String currentKey;
}
