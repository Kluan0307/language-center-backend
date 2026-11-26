package com.example.finance.assistant;

import com.example.finance.tuition.Tuition;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Component
// Legacy Java adapter; HTTP handlers live in FinanceInsightController.
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class FinanceReadController {
  private final FinanceReadService service;

  public ReadQuery.Result tuitions(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long studentId,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Boolean overdue,
      @RequestParam(required = false) Tuition.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.tuitions(scopeMode, branchId, studentId, classId, overdue, status, page, size);
  }

  public ReadQuery.Result receipts(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long studentId,
      @RequestParam(required = false) Long tuitionId,
      @RequestParam(required = false) Long classId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.receipts(scopeMode, branchId, studentId, tuitionId, classId, page, size);
  }
}
