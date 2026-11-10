package com.example.teaching.evaluation;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Table(
    name = "evaluation_policies",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_evaluation_policy_revision",
            columnNames = {"class_id", "revision"}))
@Getter
@Setter
public class EvaluationPolicy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long classId;

  @Column(nullable = false, updatable = false)
  private Long branchId;

  @Column(nullable = false, updatable = false)
  private Long courseId;

  @Column(nullable = false, updatable = false)
  private long revision;

  @Column(nullable = false, precision = 5, scale = 2, updatable = false)
  private BigDecimal passPercent;

  @Column(precision = 5, scale = 2, updatable = false)
  private BigDecimal minAttendancePercent;

  @Column(nullable = false, updatable = false)
  private Long actorId;

  @Column(nullable = false, updatable = false)
  private String actorName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime confirmedAt;

  @Column(nullable = false, length = 500, updatable = false)
  private String reason;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "evaluation_policy_exams",
      joinColumns = @JoinColumn(name = "policy_id"),
      uniqueConstraints =
          @UniqueConstraint(
              name = "uk_evaluation_policy_exam",
              columnNames = {"policy_id", "exam_id"}))
  @OrderBy("examId ASC")
  private List<Rule> rules = new ArrayList<>();

  @Embeddable
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Rule {
    @Column(nullable = false)
    private Long examId;

    @Column(nullable = false)
    private long examVersion;

    @Column(nullable = false)
    private String examName;

    @Column(nullable = false)
    private LocalDate examDate;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal weight;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal maxScore;
  }
}
