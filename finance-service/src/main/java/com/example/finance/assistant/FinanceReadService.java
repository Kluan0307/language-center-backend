package com.example.finance.assistant;

import com.example.finance.common.*;
import com.example.finance.payment.*;
import com.example.finance.tuition.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class FinanceReadService {
  private final ReadQuery query;
  private final TuitionService tuitions;
  private final PaymentService payments;

  public record TuitionRow(
      Long id,
      Long billingEnrollmentId,
      Long studentId,
      String studentName,
      Long classId,
      String className,
      BigDecimal amount,
      BigDecimal paidAmount,
      BigDecimal balance,
      BigDecimal credit,
      String currency,
      LocalDate dueDate,
      Tuition.Status status,
      boolean overdue,
      Long branchId,
      String path) {}

  public record ReceiptRow(
      Long id,
      String receiptNo,
      Long tuitionId,
      Long studentId,
      String studentName,
      Long classId,
      String className,
      BigDecimal amount,
      BigDecimal voidedAmount,
      BigDecimal refundedAmount,
      BigDecimal availableAmount,
      Payment.Status status,
      LocalDateTime paidAt,
      Long branchId,
      String path) {}

  private Long branch(ReadQuery.Scope s) {
    return s.mode() == ReadQuery.Mode.ACTIVE_BRANCH ? s.branchIds().getFirst() : null;
  }

  public ReadQuery.Result tuitions(
      ReadQuery.Mode mode,
      Long branch,
      Long studentId,
      Long classId,
      Boolean overdue,
      Tuition.Status status,
      int page,
      int size) {
    var s = query.scope(mode, branch);
    query.page(page, size);
    query.id(studentId);
    query.id(classId);
    var data =
        tuitions.list(
            page,
            size,
            null,
            status,
            branch(s),
            classId,
            studentId,
            overdue,
            mode == ReadQuery.Mode.ALL_ASSIGNED);
    var rows =
        data.content().stream()
            .map(
                t ->
                    new TuitionRow(
                        t.id(),
                        t.enrollmentId(),
                        t.studentId(),
                        t.studentName(),
                        t.classId(),
                        t.className(),
                        t.amount(),
                        t.paidAmount(),
                        t.balance(),
                        t.credit(),
                        t.currency(),
                        t.dueDate(),
                        t.status(),
                        t.overdue(),
                        t.branchId(),
                        "/tuition?tuitionId=" + t.id() + "&branchId=" + t.branchId()))
            .toList();
    return query.result(
        s,
        "search_tuitions",
        new PageResponse<>(rows, page, size, data.totalElements(), data.totalPages()));
  }

  public ReadQuery.Result receipts(
      ReadQuery.Mode mode,
      Long branch,
      Long studentId,
      Long tuitionId,
      Long classId,
      int page,
      int size) {
    var s = query.scope(mode, branch);
    query.page(page, size);
    query.id(studentId);
    query.id(tuitionId);
    query.id(classId);
    var data =
        payments.search(
            page,
            size,
            null,
            branch(s),
            tuitionId,
            studentId,
            classId,
            mode == ReadQuery.Mode.ALL_ASSIGNED);
    var rows =
        data.content().stream()
            .map(
                t ->
                    new ReceiptRow(
                        t.id(),
                        t.receiptNo(),
                        t.tuitionId(),
                        t.studentId(),
                        t.studentName(),
                        t.classId(),
                        t.className(),
                        t.amount(),
                        t.voidedAmount(),
                        t.refundedAmount(),
                        t.availableAmount(),
                        t.status(),
                        t.paidAt(),
                        t.branchId(),
                        "/payments?paymentId=" + t.id() + "&branchId=" + t.branchId()))
            .toList();
    return query.result(
        s,
        "search_receipts",
        new PageResponse<>(rows, page, size, data.totalElements(), data.totalPages()));
  }
}
