package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.example.finance.common.ApiException;
import com.example.finance.payment.*;
import com.example.finance.security.*;
import com.example.finance.stats.RevenueController;
import com.example.finance.tuition.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class FinanceTransactionTest {
  @Autowired TuitionRepository tuitions;
  @Autowired PaymentRepository payments;
  @Autowired PaymentService service;
  @Autowired RevenueController reports;
  @Autowired TuitionService tuitionService;

  @Test
  void splitPaymentsRetryAndAdjustmentRetainCorrectBalanceAndAudit() {
    user(Role.STAFF);
    try {
      Tuition t = new Tuition();
      t.setBranchId(1L);
      t.setStudentId(99L);
      t.setClassId(99L);
      t.setAmount(new BigDecimal("1000"));
      t.setOriginalAmount(new BigDecimal("1000"));
      t.setDueDate(LocalDate.now().minusDays(1));
      Long id = tuitions.saveAndFlush(t).getId();
      var request =
          new PaymentDto.Request(
              id,
              new BigDecimal("400"),
              Payment.PaymentMethod.BANK_TRANSFER,
              "BANK-1",
              UUID.randomUUID().toString(),
              "First installment");
      var first = service.create(request);
      assertEquals(first.id(), service.create(request).id());
      assertEquals(new BigDecimal("600.00"), tuitionService.get(id).balance());
      assertTrue(tuitionService.get(id).overdue());
      var adjustment =
          new TuitionDto.Adjustment(
              new BigDecimal("1000"),
              new BigDecimal("200"),
              LocalDate.now().plusDays(7),
              "Discount approved",
              tuitionService.get(id).version(),
              UUID.randomUUID().toString());
      assertThrows(ApiException.class, () -> tuitionService.adjust(id, adjustment));
      user(Role.ADMIN);
      var adjusted = tuitionService.adjust(id, adjustment);
      assertEquals(7L, adjusted.adjustedBy());
      assertEquals("cashier", adjusted.adjustedByName());
      assertEquals("Discount approved", adjusted.adjustmentReason());
      assertNotNull(adjusted.adjustedAt());
      assertEquals(0, new BigDecimal("400").compareTo(adjusted.balance()));
      assertThrows(
          ApiException.class,
          () ->
              tuitionService.adjust(
                  id,
                  new TuitionDto.Adjustment(
                      new BigDecimal("100"),
                      new BigDecimal("200"),
                      LocalDate.now(),
                      "Invalid reduction",
                      tuitionService.get(id).version(),
                      UUID.randomUUID().toString())));
      var finalRequest =
          new PaymentDto.Request(
              id,
              new BigDecimal("400"),
              Payment.PaymentMethod.CASH,
              null,
              UUID.randomUUID().toString(),
              null);
      var last = service.create(finalRequest);
      assertEquals(Tuition.Status.PAID, tuitionService.get(id).status());
      assertEquals(last.id(), service.create(finalRequest).id());
      assertEquals(2, service.list(0, 20, null, null, id).totalElements());
      assertFalse(tuitionService.get(id).overdue());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  void user(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(7L, 8L, 1L, role, "cashier"),
                null,
                List.of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + role.name()))));
  }

  @Test
  void twoCashiersCannotSpendTheSameBalance() throws Exception {
    Tuition t = new Tuition();
    t.setBranchId(1L);
    t.setStudentId(1L);
    t.setClassId(1L);
    t.setCourseId(1L);
    t.setAmount(new BigDecimal("100"));
    t.setOriginalAmount(new BigDecimal("100"));
    t.setDueDate(LocalDate.now());
    t = tuitions.saveAndFlush(t);
    Long id = t.getId();
    CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> tasks = new ArrayList<>();
      for (int n = 0; n < 2; n++) {
        String key = "concurrent-" + UUID.randomUUID();
        tasks.add(
            pool.submit(
                () -> {
                  user(Role.STAFF);
                  ready.countDown();
                  start.await();
                  try {
                    service.create(
                        new PaymentDto.Request(
                            id,
                            new BigDecimal("100"),
                            Payment.PaymentMethod.CASH,
                            null,
                            key,
                            null));
                    return true;
                  } catch (ApiException conflict) {
                    assertEquals(409, conflict.getStatus().value());
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
    }
    assertEquals(0, new BigDecimal("100").compareTo(payments.collected(id)));
    assertEquals(Tuition.Status.PAID, tuitions.findById(id).orElseThrow().getStatus());
    user(Role.ADMIN);
    try {
      var report = reports.overview(1L, null, null);
      assertFalse(report.byMonth().isEmpty());
      assertFalse(report.byCourse().isEmpty());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
