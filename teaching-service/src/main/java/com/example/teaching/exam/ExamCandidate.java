package com.example.teaching.exam;

import com.example.teaching.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "exam_candidates",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_exam_candidate",
            columnNames = {"exam_id", "student_id"}))
@Getter
@Setter
public class ExamCandidate extends BaseEntity {
 private Long enrollmentId;
  @Column(nullable = false)
  private Long examId;

  @Column(nullable = false)
  private Long studentId;

  @Column(nullable = false)
  private String studentName;

  @Column(nullable = false)
  private String source;
}
