package com.example.finance.ledger;

import com.example.finance.payment.*;
import com.example.finance.tuition.*;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class LedgerController {
  private final TuitionService tuitions;
  private final PaymentService payments;
  private final TuitionAdjustmentRepository adjustments;
  private final PaymentCorrectionRepository corrections;
  private final CorrectionService service;

  public record History(List<TuitionAdjustment> adjustments, List<PaymentCorrection> corrections) {}

  @GetMapping("/tuitions/{id}/history")
  @Transactional(readOnly = true)
  public History history(@PathVariable Long id) {
    tuitions.require(id);
    return new History(
        adjustments.findByTuitionIdOrderByIdDesc(id), corrections.findByTuitionIdOrderByIdDesc(id));
  }

  @GetMapping("/payments/{id}/corrections")
  @Transactional(readOnly = true)
  public List<PaymentCorrection> corrections(@PathVariable Long id) {
    payments.get(id);
    return corrections.findByPaymentIdOrderByIdDesc(id);
  }

  @PostMapping("/payments/{id}/corrections")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public PaymentCorrection correct(
      @PathVariable Long id, @Valid @RequestBody CorrectionService.Request r) {
    return service.create(id, r);
  }
}
