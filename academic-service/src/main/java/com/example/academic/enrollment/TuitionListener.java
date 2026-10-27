package com.example.academic.enrollment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
@RequiredArgsConstructor
public class TuitionListener {
  private final TuitionClient client;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void created(EnrollmentCreated e) {
    client.enrollmentCreated(e.enrollmentId(), e.studentId(), e.classId(), e.tuitionFee());
  }
}
