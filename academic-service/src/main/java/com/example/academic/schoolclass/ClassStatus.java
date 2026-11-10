package com.example.academic.schoolclass;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class ClassStatus {
  private ClassStatus() {}

  public static Specification<SchoolClass> is(SchoolClass.Status status) {
    return (r, q, b) -> {
      if (status == null) return b.conjunction();
      var actualEnd=b.<LocalDate>selectCase().when(b.greaterThan(r.get("sessionEndDate"),r.<LocalDate>get("endDate")),r.<LocalDate>get("sessionEndDate")).otherwise(r.<LocalDate>get("endDate"));
      if (status == SchoolClass.Status.FULL)
        return b.and(
            b.greaterThanOrEqualTo(r.get("currentSize"), r.get("maxSize")),
            b.notEqual(r.get("status"), SchoolClass.Status.FINISHED),
            b.greaterThanOrEqualTo(
                actualEnd, LocalDate.now(com.example.academic.common.BusinessTime.ZONE)));
      var effective =
          b.<SchoolClass.Status>selectCase()
              .when(
                  b.equal(r.get("status"), SchoolClass.Status.FINISHED),
                  SchoolClass.Status.FINISHED)
              .when(
                  b.lessThan(
                      actualEnd,
                      LocalDate.now(com.example.academic.common.BusinessTime.ZONE)),
                  SchoolClass.Status.FINISHED)
              .when(
                  b.greaterThan(
                      r.get("startDate"),
                      LocalDate.now(com.example.academic.common.BusinessTime.ZONE)),
                  SchoolClass.Status.UPCOMING)
              .otherwise(SchoolClass.Status.ONGOING);
      return b.equal(effective, status);
    };
  }
}
