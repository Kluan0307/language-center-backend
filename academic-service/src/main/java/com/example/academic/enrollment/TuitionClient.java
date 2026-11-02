package com.example.academic.enrollment;

import java.math.BigDecimal;

public interface TuitionClient {
  void enrollmentCreated(Long enrollmentId, Long studentId, Long classId, BigDecimal tuitionFee);
}
