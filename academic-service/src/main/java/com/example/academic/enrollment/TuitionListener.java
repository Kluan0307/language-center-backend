package com.example.academic.enrollment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class TuitionListener {
  private final TuitionDeliveryService delivery;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void created(EnrollmentCreated e) {
    try {
      delivery.deliver(e.enrollmentId(), e.token());
    } catch (RuntimeException failure) {
      log.error(
          "Tuition delivery could not be recorded for enrollment {}", e.enrollmentId(), failure);
    }
  }
}
