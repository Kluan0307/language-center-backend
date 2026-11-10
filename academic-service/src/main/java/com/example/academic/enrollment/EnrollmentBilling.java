package com.example.academic.enrollment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnrollmentBilling(
    Long enrollmentId,
    Long branchId,
    Long studentId,
    String studentName,
    Long classId,
    String className,
    Long courseId,
    String courseName,
    BigDecimal tuitionFee,
    LocalDate dueDate) {
  public static EnrollmentBilling of(Enrollment e) {
    var c = e.getSchoolClass();
    return new EnrollmentBilling(
        e.getId(),
        c.getBranchId(),
        e.getStudent().getId(),
        e.getStudent().getFullName(),
        c.getId(),
        c.getName(),
        c.getCourse().getId(),
        c.getCourse().getName(),
        e.getTuitionFee() == null ? c.getCourse().getTuitionFee() : e.getTuitionFee(),
        c.getStartDate());
  }
}
