package com.example.teaching.grade;

import com.example.teaching.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "grade_changes",
    indexes = @Index(name = "idx_grade_change_grade", columnList = "grade_id"))
@Getter
@Setter
public class GradeChange extends BaseEntity {
  private Long gradeId;

  @Column(precision = 7, scale = 2)
  private BigDecimal oldScore;

  @Column(precision = 7, scale = 2)
  private BigDecimal newScore;

  private Long modifiedBy;
  private String modifiedByName;
  private LocalDateTime modifiedAt =
      LocalDateTime.now(com.example.teaching.common.BusinessTime.ZONE);

  @Column(length = 500)
  private String reason;
}
