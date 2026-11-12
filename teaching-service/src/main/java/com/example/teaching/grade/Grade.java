package com.example.teaching.grade;

import com.example.teaching.common.BaseEntity;
import com.example.teaching.exam.Exam;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "grades",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_grade_exam_student",
            columnNames = {"exam_id", "student_id"}))
@Getter
@Setter
public class Grade extends BaseEntity {
  @Column(length = 1000)
  private String studentComment;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_id", nullable = false)
  private Exam exam;

  @Column(nullable = false)
  private Long branchId;

  @Column(nullable = false)
  private Long studentId;

  private String studentName;

  @Column(nullable = false, precision = 7, scale = 2)
  private BigDecimal score;

  @Column(nullable = false, updatable = false)
  private Long enteredBy;

  @Column(updatable = false)
  private String enteredByName;

  @Column(nullable = false, updatable = false)
  private LocalDateTime enteredAt =
      LocalDateTime.now(com.example.teaching.common.BusinessTime.ZONE);

  private Long lastModifiedBy;
  private String lastModifiedByName;
  private LocalDateTime lastModifiedAt;

  @Column(length = 500)
  private String modificationReason;

  @Column(length = 1000)
  private String comment;

  @Version private Long version;
}
