package com.example.academic.lifecycle;

import com.example.academic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "learning_events",
    indexes = {
      @Index(name = "idx_learning_student", columnList = "student_id,id"),
      @Index(name = "idx_learning_enrollment", columnList = "enrollment_id,id")
    })
@Getter
@Setter
public class LearningEvent extends BaseEntity {
  @Column(nullable = false)
  private Long branchId;

  @Column(nullable = false)
  private Long studentId;

  private Long enrollmentId;
  private Long relatedEnrollmentId;

  @Column(nullable = false)
  private String action;

  private String oldStatus;

  @Column(nullable = false)
  private String newStatus;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(nullable = false)
  private LocalDate effectiveDate;

  private LocalDate expectedReturnDate;

  @Column(nullable = false)
  private Long actorId;

  @Column(nullable = false)
  private String actorName;

  @Column(nullable = false)
  private LocalDateTime occurredAt =
      LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE);
}
