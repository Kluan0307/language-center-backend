package com.example.academic.enrollment;

import com.example.academic.common.*;
import com.example.academic.security.BranchScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TuitionDeliveryService {
  private final EnrollmentRepository repository;
  private final BranchScope scope;
  private final RemoteApi api;
  private final jakarta.persistence.EntityManager entityManager;

  @Value("${services.finance.url}")
  private String base;

  public record Request(Long enrollmentId) {}

  public record Receipt(Long id) {}

  public record Delivery(Long enrollmentId, Enrollment.TuitionDelivery status, String error) {}

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Delivery deliver(Long id, String token) {
    Enrollment e = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(e.getSchoolClass().getBranchId());
    if (e.getBillingEnrollmentId() != null && !e.getBillingEnrollmentId().equals(e.getId()))
      e = repository.findById(e.getBillingEnrollmentId()).orElseThrow(ApiException::missing);
    scope.check(e.getSchoolClass().getBranchId());
    entityManager.refresh(e, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    if (e.getTuitionDelivery() != Enrollment.TuitionDelivery.CREATED) {
      try {
        api.post(base + "/api/v1/finance/tuitions", new Request(e.getId()), Receipt.class, token);
        e.setTuitionDelivery(Enrollment.TuitionDelivery.CREATED);
        e.setTuitionDeliveryError(null);
      } catch (RuntimeException ex) {
        e.setTuitionDelivery(Enrollment.TuitionDelivery.FAILED);
        e.setTuitionDeliveryError("Tuition delivery failed; retry after checking finance");
        log.warn(
            "Tuition delivery failed for enrollment {}: {}",
            e.getId(),
            ex.getClass().getSimpleName());
      }
    }
    return new Delivery(e.getId(), e.getTuitionDelivery(), e.getTuitionDeliveryError());
  }
}
