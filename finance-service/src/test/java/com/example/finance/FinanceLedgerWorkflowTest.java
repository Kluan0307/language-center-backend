package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.example.finance.common.*;
import com.example.finance.ledger.*;
import com.example.finance.payment.*;
import com.example.finance.security.*;
import com.example.finance.stats.*;
import com.example.finance.tuition.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.*;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:finance-ledger-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class FinanceLedgerWorkflowTest {
  @Autowired TuitionRepository tuitions;
  @Autowired TuitionService tuitionService;
  @Autowired PaymentRepository payments;
  @Autowired PaymentService paymentService;
  @Autowired CorrectionService service;
  @Autowired PaymentCorrectionRepository corrections;
  @Autowired TuitionAdjustmentRepository adjustments;
  @Autowired RevenueController reports;
  @Autowired FinanceLedger ledger;
  static final AtomicLong branches = new AtomicLong(1000);
  Long branch;
  Tuition tuition;

  void actor(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(7L, 8L, branch, role, "operator"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
  }

  @BeforeEach
  void setup() {
    branch = branches.incrementAndGet();
    actor(Role.ADMIN);
    tuition = new Tuition();
    tuition.setBranchId(branch);
    tuition.setStudentId(1L);
    tuition.setStudentName("Learner");
    tuition.setClassId(1L);
    tuition.setCourseId(1L);
    tuition.setOriginalAmount(new BigDecimal("1000"));
    tuition.setAmount(new BigDecimal("1000"));
    tuition.setDueDate(LocalDate.now(BusinessTime.ZONE));
    tuition = tuitions.saveAndFlush(tuition);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  BigDecimal money(int n) {
    return BigDecimal.valueOf(n);
  }

  long version() {
    return tuitionService.get(tuition.getId()).version();
  }

  PaymentDto pay(int amount) {
    return paymentService.create(
        new PaymentDto.Request(
            tuition.getId(),
            money(amount),
            Payment.PaymentMethod.CASH,
            null,
            UUID.randomUUID().toString(),
            "Actual collection"));
  }

  TuitionDto adjust(int net) {
    return tuitionService.adjust(
        tuition.getId(),
        new TuitionDto.Adjustment(
            money(1000),
            money(1000 - net),
            tuition.getDueDate(),
            "Approved fee change",
            version(),
            UUID.randomUUID().toString()));
  }

  CorrectionService.Request request(PaymentCorrection.Type type, int amount) {
    return new CorrectionService.Request(
        type,
        money(amount),
        type == PaymentCorrection.Type.REFUND ? Payment.PaymentMethod.CASH : null,
        null,
        "Verified financial correction",
        UUID.randomUUID().toString(),
        version());
  }

  @Test
  void reductionBelowCollectedCreatesCreditAndPreservesOriginalReceipt() {
    var p = pay(1000);
    var t = adjust(700);
    assertEquals(0, money(300).compareTo(t.credit()));
    assertEquals(0, t.balance().signum());
    assertEquals(Tuition.Status.PAID, t.status());
    assertEquals(Payment.Status.PAID, paymentService.get(p.id()).status());
    var h = adjustments.findByTuitionIdOrderByIdDesc(t.id()).getFirst();
    assertEquals(0, money(1000).compareTo(h.getOldAmount()));
    assertEquals(0, money(700).compareTo(h.getNewAmount()));
    assertEquals("operator", h.getRecordedByName());
  }

  @Test
  void adjustmentRetryDoesNotAppendAnotherEventAndKeyCannotChangePayload() {
    var r =
        new TuitionDto.Adjustment(
            money(900),
            money(100),
            tuition.getDueDate(),
            "Approved change",
            version(),
            "same-adjustment");
    tuitionService.adjust(tuition.getId(), r);
    tuitionService.adjust(tuition.getId(), r);
    assertEquals(1, adjustments.findByTuitionIdOrderByIdDesc(tuition.getId()).size());
    assertThrows(
        ApiException.class,
        () ->
            tuitionService.adjust(
                tuition.getId(),
                new TuitionDto.Adjustment(
                    money(900),
                    money(100),
                    tuition.getDueDate(),
                    "Different reason",
                    r.version(),
                    r.requestKey())));
  }

  @Test
  void staleAdjustmentCannotOverwriteReceipt() {
    long v = version();
    pay(100);
    assertThrows(
        ApiException.class,
        () ->
            tuitionService.adjust(
                tuition.getId(),
                new TuitionDto.Adjustment(
                    money(900),
                    money(0),
                    tuition.getDueDate(),
                    "Old browser",
                    v,
                    "stale-adjustment")));
  }

  @Test
  void staffCannotReverseRefundOrAdjust() {
    var p = pay(1000);
    var r = request(PaymentCorrection.Type.VOID, 1000);
    actor(Role.STAFF);
    assertThrows(ApiException.class, () -> service.create(p.id(), r));
    assertThrows(ApiException.class, () -> adjust(900));
  }

  @Test
  void fullVoidRestoresDebtAndKeepsOriginalImmutable() {
    var p = pay(1000);
    service.create(p.id(), request(PaymentCorrection.Type.VOID, 1000));
    var t = tuitionService.get(tuition.getId());
    assertEquals(0, t.paidAmount().signum());
    assertEquals(0, money(1000).compareTo(t.balance()));
    var original = payments.findById(p.id()).orElseThrow();
    assertEquals(Payment.Status.PAID, original.getStatus());
    assertEquals(0, money(1000).compareTo(original.getAmount()));
    assertEquals("operator", original.getRecordedByName());
    assertEquals(0, paymentService.get(p.id()).availableAmount().signum());
  }

  @Test
  void voidMustBeWholeUnprocessedReceipt() {
    var p = pay(1000);
    assertThrows(
        ApiException.class,
        () -> service.create(p.id(), request(PaymentCorrection.Type.VOID, 500)));
    service.create(p.id(), request(PaymentCorrection.Type.VOID, 1000));
    assertThrows(
        ApiException.class,
        () -> service.create(p.id(), request(PaymentCorrection.Type.VOID, 1000)));
  }

  @Test
  void refundRequiresCreditRatherThanCreatingNewDebt() {
    var p = pay(1000);
    assertThrows(
        ApiException.class,
        () -> service.create(p.id(), request(PaymentCorrection.Type.REFUND, 100)));
    assertEquals(0, money(1000).compareTo(tuitionService.get(tuition.getId()).paidAmount()));
  }

  @Test
  void partialRefundsCannotExceedCreditAndRetainReceiptAuthor() {
    var p = pay(1000);
    adjust(700);
    service.create(p.id(), request(PaymentCorrection.Type.REFUND, 100));
    service.create(p.id(), request(PaymentCorrection.Type.REFUND, 200));
    var t = tuitionService.get(tuition.getId());
    assertEquals(0, t.credit().signum());
    assertEquals(0, t.balance().signum());
    assertThrows(
        ApiException.class,
        () -> service.create(p.id(), request(PaymentCorrection.Type.REFUND, 1)));
    var dto = paymentService.get(p.id());
    assertEquals(0, money(300).compareTo(dto.refundedAmount()));
    assertEquals("operator", dto.recordedByName());
  }

  @Test
  void refundedReceiptCannotBeVoided() {
    var p = pay(1000);
    adjust(700);
    service.create(p.id(), request(PaymentCorrection.Type.REFUND, 100));
    assertThrows(
        ApiException.class,
        () -> service.create(p.id(), request(PaymentCorrection.Type.VOID, 1000)));
  }

  @Test
  void bankRefundNeedsActualReference() {
    var p = pay(1000);
    adjust(700);
    assertThrows(
        ApiException.class,
        () ->
            service.create(
                p.id(),
                new CorrectionService.Request(
                    PaymentCorrection.Type.REFUND,
                    money(100),
                    Payment.PaymentMethod.BANK_TRANSFER,
                    " ",
                    "Refund transferred",
                    "bank",
                    version())));
  }

  @Test
  void refundRetryWithOriginalVersionIsSafeAfterFurtherChanges() {
    var p = pay(1000);
    adjust(700);
    var r = request(PaymentCorrection.Type.REFUND, 100);
    var c = service.create(p.id(), r);
    adjust(600);
    assertEquals(c.getId(), service.create(p.id(), r).getId());
    assertEquals(1, corrections.findByPaymentIdOrderByIdDesc(p.id()).size());
    assertThrows(
        ApiException.class,
        () ->
            service.create(
                p.id(),
                new CorrectionService.Request(
                    r.type(),
                    money(99),
                    r.paymentMethod(),
                    null,
                    r.reason(),
                    r.requestKey(),
                    r.version())));
  }

  Payment oldReceipt() {
    var p = new Payment();
    p.setTuitionId(tuition.getId());
    p.setBranchId(branch);
    p.setAmount(money(1000));
    p.setPaymentMethod(Payment.PaymentMethod.CASH);
    p.setReceiptNo(UUID.randomUUID().toString());
    p.setRequestKey(UUID.randomUUID().toString());
    p.setPaidAt(LocalDate.now(BusinessTime.ZONE).minusDays(1).atTime(12, 0));
    p.setRecordedBy(7L);
    p.setRecordedByName("Original cashier");
    payments.saveAndFlush(p);
    tuition.setPaidAmount(money(1000));
    tuitions.saveAndFlush(tuition);
    return p;
  }

  @Test
  void voidBelongsToCurrentDateWithoutRewritingPastRevenue() {
    var p = oldReceipt();
    var today = LocalDate.now(BusinessTime.ZONE);
    service.create(p.getId(), request(PaymentCorrection.Type.VOID, 1000));
    var past = reports.overview(branch, today.minusDays(1), today.minusDays(1));
    var current = reports.overview(branch, today, today);
    assertEquals(0, money(1000).compareTo(past.collected()));
    assertEquals(0, money(-1000).compareTo(current.collected()));
    assertEquals(0, money(1000).compareTo(current.voided()));
    assertEquals(0, current.grossCollected().signum());
    assertEquals(0, money(1000).compareTo(current.outstanding()));
  }

  @Test
  void refundReportSeparatesGrossNetAndCurrentCredit() {
    var p = oldReceipt();
    adjust(700);
    service.create(p.getId(), request(PaymentCorrection.Type.REFUND, 200));
    var r = reports.overview(branch, null, null);
    assertEquals(0, money(1000).compareTo(r.grossCollected()));
    assertEquals(0, money(200).compareTo(r.refunded()));
    assertEquals(0, money(800).compareTo(r.collected()));
    assertEquals(0, money(100).compareTo(r.credit()));
    assertEquals(0, r.outstanding().signum());
  }

  @Test
  void mismatchIsReportedAndBlocksWritesWithoutAutomaticRepair() {
    tuition.setPaidAmount(money(123));
    tuitions.saveAndFlush(tuition);
    assertThrows(ApiException.class, () -> pay(10));
    assertThrows(ApiException.class, () -> adjust(900));
    var r = reports.reconciliation(0, 20, branch, false).content().getFirst();
    assertEquals(0, money(123).compareTo(r.difference()));
    assertEquals(0, money(123).compareTo(tuitionService.get(tuition.getId()).paidAmount()));
  }

  @Test
  void foreignBranchCannotReverseReceipt() {
    var p = pay(1000);
    var r = request(PaymentCorrection.Type.VOID, 1000);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(11L, 12L, 99999L, Role.ADMIN, "other"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    assertThrows(ApiException.class, () -> service.create(p.id(), r));
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentRefundsCannotSpendTheSameCredit() throws Exception {
    var p = pay(1000);
    adjust(700);
    long v = version();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      var ready = new CountDownLatch(2);
      var tasks = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < 2; i++) {
        var key = UUID.randomUUID().toString();
        tasks.add(
            pool.submit(
                () -> {
                  actor(Role.ADMIN);
                  ready.countDown();
                  start.await();
                  try {
                    service.create(
                        p.id(),
                        new CorrectionService.Request(
                            PaymentCorrection.Type.REFUND,
                            money(200),
                            Payment.PaymentMethod.CASH,
                            null,
                            "Concurrent refund",
                            key,
                            v));
                    return true;
                  } catch (ApiException e) {
                    assertEquals(409, e.getStatus().value());
                    return false;
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      int success = 0;
      for (var task : tasks) if (task.get(15, TimeUnit.SECONDS)) success++;
      assertEquals(1, success);
      assertEquals(0, money(100).compareTo(tuitionService.get(tuition.getId()).credit()));
    } finally {
      corrections.deleteAll(corrections.findByTuitionIdOrderByIdDesc(tuition.getId()));
      adjustments.deleteAll(adjustments.findByTuitionIdOrderByIdDesc(tuition.getId()));
      payments.deleteById(p.id());
      tuitions.deleteById(tuition.getId());
    }
  }

  @Test
  void unknownLegacyPaymentBlocksNewWritesEvenIfCachedNetMatches() {
    var p = new Payment();
    p.setTuitionId(tuition.getId());
    p.setBranchId(branch);
    p.setAmount(money(100));
    p.setPaymentMethod(Payment.PaymentMethod.CASH);
    p.setStatus(Payment.Status.PENDING);
    p.setReceiptNo(UUID.randomUUID().toString());
    p.setRequestKey(UUID.randomUUID().toString());
    payments.saveAndFlush(p);
    assertEquals(0, ledger.net(tuition.getId()).signum());
    assertThrows(ApiException.class, () -> pay(10));
    assertThrows(ApiException.class, () -> adjust(900));
    assertEquals(
        1, reports.reconciliation(0, 20, branch, false).content().getFirst().legacyPayments());
  }
}
