package com.example.academic.schoolclass;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class ClassStatus {
  private ClassStatus() {}

  public static Specification<SchoolClass> is(SchoolClass.Status status) {
    return (r, q, b) -> {
      if (status == null) return b.conjunction();
      var effective =
          b.<SchoolClass.Status>selectCase()
              .when(
                  b.or(
                      b.isTrue(r.get("statusManuallySet")),
                      r.get("status").in(SchoolClass.Status.FULL, SchoolClass.Status.FINISHED)),
                  r.<SchoolClass.Status>get("status"))
              .when(b.lessThan(r.get("endDate"), LocalDate.now()), SchoolClass.Status.FINISHED)
              .when(b.greaterThan(r.get("startDate"), LocalDate.now()), SchoolClass.Status.UPCOMING)
              .otherwise(SchoolClass.Status.ONGOING);
      return b.equal(effective, status);
    };
  }
}
