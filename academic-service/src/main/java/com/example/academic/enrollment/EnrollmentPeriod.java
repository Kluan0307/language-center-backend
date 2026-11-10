package com.example.academic.enrollment;

import com.example.academic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(
    name = "enrollment_periods",
    indexes = @Index(name = "idx_period_enrollment", columnList = "enrollment_id"))
@Getter
@Setter
public class EnrollmentPeriod extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;

  @Column(nullable = false)
  private LocalDate activeFrom;

  private LocalDate activeUntil;

  public boolean includes(LocalDate date) {
    return !date.isBefore(activeFrom) && (activeUntil == null || date.isBefore(activeUntil));
  }
}
