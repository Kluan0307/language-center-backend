package com.example.academic.course;

import com.example.academic.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(
    name = "courses",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_course_code_scope",
            columnNames = {"code", "scope_key"}))
@Getter
@Setter
public class Course extends BaseEntity {
  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  private String type;
  private String level;

  @Column(nullable = false)
  private Integer duration;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal tuitionFee;

  @Column(columnDefinition = "text")
  private String content;

  @Column(columnDefinition = "text")
  private String entryRequirement;

  @Column(nullable = false)
  private Integer maxStudents;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.OPEN;

  private Long branchId;

  // MySQL UNIQUE treats NULL values as distinct; use a generated scope for shared courses.
  @Column(
      name = "scope_key",
      insertable = false,
      updatable = false,
      columnDefinition = "bigint generated always as (coalesce(branch_id,0)) stored")
  private Long scopeKey;

  public enum Status {
    OPEN,
    CLOSED
  }
}
