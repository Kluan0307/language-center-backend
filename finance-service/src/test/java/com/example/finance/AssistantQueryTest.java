package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.example.finance.assistant.*;
import com.example.finance.common.*;
import com.example.finance.ledger.*;
import com.example.finance.payment.*;
import com.example.finance.security.*;
import com.example.finance.tuition.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:assistant-finance;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "payment.jobs.enabled=false",
      "notification.jobs.enabled=false"
    })
@Transactional
class AssistantQueryTest {
  @Autowired FinanceReadService service;
  @Autowired PaymentService legacy;
  @Autowired EntityManager em;
  Tuition a, b;
  Payment receipt;
  long nextEnrollment = 100;

  void actor(Role role, List<Long> ids) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(
                    100L, 11L, ids.isEmpty() ? null : ids.getFirst(), role, "operator", ids),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Tuition bill(long branch, boolean cancelled) {
    var t = new Tuition();
    t.setBranchId(branch);
    t.setStudentId(11L);
    t.setStudentName("Student");
    t.setClassId(9L);
    t.setClassName("Class");
    t.setEnrollmentId(++nextEnrollment);
    t.setAmount(new BigDecimal("1000"));
    t.setPaidAmount(BigDecimal.ZERO);
    t.setDueDate(LocalDate.now(BusinessTime.ZONE).minusDays(1));
    if (cancelled) t.setStatus(Tuition.Status.CANCELLED);
    em.persist(t);
    return t;
  }

  Payment receipt(Tuition t) {
    var p = new Payment();
    p.setBranchId(t.getBranchId());
    p.setTuitionId(t.getId());
    p.setAmount(new BigDecimal("400"));
    p.setPaymentMethod(Payment.PaymentMethod.CASH);
    p.setReceiptNo(UUID.randomUUID().toString());
    p.setNote("PRIVATE_NOTE");
    p.setReferenceCode("PRIVATE_REFERENCE");
    em.persist(p);
    return p;
  }

  @BeforeEach
  void seed() {
    actor(Role.ADMIN, List.of(1L, 2L));
    a = bill(1, false);
    b = bill(2, false);
    var foreign = bill(3, false);
    bill(1, true);
    receipt = receipt(a);
    receipt(b);
    receipt(foreign);
    em.flush();
  }

  PageResponse<?> page(ReadQuery.Result r) {
    return (PageResponse<?>) r.data();
  }

  @Test
  void overdueExcludesCancelledAndCountPrecedesPageLimit() {
    assertEquals(
        1,
        page(service.tuitions(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, true, null, 0, 1))
            .totalElements());
    assertEquals(
        2,
        page(service.tuitions(ReadQuery.Mode.ALL_ASSIGNED, null, null, null, true, null, 0, 1))
            .totalElements());
    assertEquals(
        1,
        page(service.tuitions(ReadQuery.Mode.ALL_ASSIGNED, null, null, null, true, null, 1, 1))
            .content()
            .size());
  }

  @Test
  void paymentsAggregateExplicitlyAndLegacyCallRemainsActiveBranch() {
    assertEquals(
        2,
        page(service.receipts(ReadQuery.Mode.ALL_ASSIGNED, null, null, null, null, 0, 1))
            .totalElements());
    assertEquals(1, legacy.search(0, 20, null, null, null, null, null).totalElements());
    actor(Role.SUPER_ADMIN, List.of());
    assertEquals(
        3,
        page(service.receipts(ReadQuery.Mode.WHOLE_SYSTEM, null, null, null, null, 0, 20))
            .totalElements());
  }

  @Test
  void projectionsExcludeNotesReferencesAndAuditActors() {
    var row =
        (FinanceReadService.ReceiptRow)
            page(service.receipts(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, null, 0, 5))
                .content()
                .getFirst();
    assertFalse(row.toString().contains("PRIVATE"));
    assertTrue(row.path().startsWith("/payments?paymentId="));
    assertEquals(0, new BigDecimal("400").compareTo(row.availableAmount()));
    var tuition =
        (FinanceReadService.TuitionRow)
            page(service.tuitions(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, true, null, 0, 5))
                .content()
                .getFirst();
    assertEquals(a.getEnrollmentId(), tuition.billingEnrollmentId());
  }

  @Test
  void correctionsArePreservedInsteadOfCallingOriginalAmountNetRevenue() {
    var c = new PaymentCorrection();
    c.setPaymentId(receipt.getId());
    c.setTuitionId(a.getId());
    c.setBranchId(1L);
    c.setType(PaymentCorrection.Type.REFUND);
    c.setAmount(new BigDecimal("100"));
    c.setReason("Test correction");
    c.setRequestKey(UUID.randomUUID().toString());
    c.setExpectedVersion(0L);
    c.setRecordedBy(100L);
    c.setRecordedByName("Operator");
    em.persist(c);
    em.flush();
    var row =
        (FinanceReadService.ReceiptRow)
            page(service.receipts(ReadQuery.Mode.ACTIVE_BRANCH, null, null, a.getId(), null, 0, 5))
                .content()
                .getFirst();
    assertEquals(0, new BigDecimal("100").compareTo(row.refundedAmount()));
    assertEquals(0, new BigDecimal("300").compareTo(row.availableAmount()));
  }

  @Test
  void teachersStudentsAndStaffAggregateCannotReadFinance() {
    for (var role : List.of(Role.TEACHER, Role.STUDENT)) {
      actor(role, List.of(1L));
      assertThrows(
          org.springframework.security.access.AccessDeniedException.class,
          () -> service.receipts(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, null, 0, 5));
    }
    actor(Role.STAFF, List.of(1L));
    assertThrows(
        ApiException.class,
        () -> service.receipts(ReadQuery.Mode.ALL_ASSIGNED, null, null, null, null, 0, 5));
  }

  @Test
  void scopesValidationAndEmptyResultsAreExplicit() {
    assertThrows(
        ApiException.class,
        () -> service.receipts(ReadQuery.Mode.ACTIVE_BRANCH, 3L, null, null, null, 0, 5));
    assertThrows(
        ApiException.class,
        () -> service.receipts(ReadQuery.Mode.ALL_ASSIGNED, 1L, null, null, null, 0, 5));
    assertThrows(
        ApiException.class,
        () -> service.tuitions(ReadQuery.Mode.ACTIVE_BRANCH, null, -1L, null, null, null, 0, 5));
    assertThrows(
        ApiException.class,
        () -> service.receipts(ReadQuery.Mode.ACTIVE_BRANCH, null, null, null, null, 0, 21));
    assertEquals(
        "NO_RESULTS",
        service.receipts(ReadQuery.Mode.ACTIVE_BRANCH, null, 999L, null, null, 0, 5).status());
    actor(Role.ADMIN, List.of());
    assertEquals(
        0,
        page(service.receipts(ReadQuery.Mode.ALL_ASSIGNED, null, null, null, null, 0, 5))
            .totalElements());
  }
}
