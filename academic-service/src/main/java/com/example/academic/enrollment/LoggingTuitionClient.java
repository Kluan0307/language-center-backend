package com.example.academic.enrollment;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingTuitionClient implements TuitionClient {
  @Override
  public void enrollmentCreated(
      Long enrollmentId, Long studentId, Long classId, BigDecimal tuitionFee) {
    log.info(
        "Tuition placeholder: enrollmentId={}, studentId={}, classId={}, tuitionFee={}",
        enrollmentId,
        studentId,
        classId,
        tuitionFee);
  }
}
