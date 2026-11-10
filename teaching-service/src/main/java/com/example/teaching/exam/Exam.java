package com.example.teaching.exam;

import com.example.teaching.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "exams",
    indexes = @Index(name = "idx_exam_branch_class", columnList = "branch_id,class_id"))
@Getter
@Setter
public class Exam extends BaseEntity {
 @Version private long version;
  @Column(nullable = false)
  private Long branchId;

  @Column(nullable = false)
  private Long classId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String examType;

  @Column(nullable = false)
  private LocalDate examDate;

  @Column(nullable = false, precision = 7, scale = 2)
  private BigDecimal weight;

  @Column(nullable = false, precision = 7, scale = 2)
  private BigDecimal maxScore;

  private LocalDateTime rosterFrozenAt;
  private Long rosterFrozenBy;
  private String rosterFrozenByName;
  private Long createdBy;
  private String createdByName;
  private LocalDateTime createdAt =
      LocalDateTime.now(com.example.teaching.common.BusinessTime.ZONE);
}
