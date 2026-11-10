package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.example.finance.payment.*;
import com.example.finance.security.*;
import com.example.finance.stats.RevenueController;
import com.example.finance.tuition.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:revenue-report;MODE=MySQL;DB_CLOSE_DELAY=-1")
class RevenueReportTest {
  @Autowired TuitionRepository tuitions;
  @Autowired PaymentRepository payments;
  @Autowired RevenueController reports;

  void actor(Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, 1L, 1L, role, "manager"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
  }

  void payment(Long branch, int amount, LocalDate date) {
    Tuition t = new Tuition();
    t.setBranchId(branch);
    t.setStudentId(1L);
    t.setClassId(1L);
    t.setCourseId(1L);
    t.setAmount(BigDecimal.valueOf(amount));
    t.setPaidAmount(BigDecimal.valueOf(amount));
    t.setDueDate(date);
    t = tuitions.saveAndFlush(t);
    Payment p = new Payment();
    p.setTuitionId(t.getId());
    p.setBranchId(branch);
    p.setAmount(BigDecimal.valueOf(amount));
    p.setPaymentMethod(Payment.PaymentMethod.CASH);
    p.setPaidAt(date.atTime(23, 59, 59));
    p.setReceiptNo(UUID.randomUUID().toString());
    p.setRequestKey(UUID.randomUUID().toString());
    payments.saveAndFlush(p);
  }

  @Test
  void sortsMonthsChronologicallyAndKeepsCurrentDebtIndependentOfReceiptDates() {
    payment(1L, 100, LocalDate.of(2026, 1, 31));
    payment(1L, 200, LocalDate.of(2026, 2, 28));
    payment(1L, 300, LocalDate.of(2026, 10, 31));
    payment(2L, 999, LocalDate.of(2026, 2, 28));
    Tuition debt = new Tuition();
    debt.setBranchId(1L);
    debt.setStudentId(1L);
    debt.setClassId(1L);
    debt.setAmount(BigDecimal.valueOf(1000));
    debt.setDueDate(LocalDate.of(2026, 1, 1));
    tuitions.saveAndFlush(debt);
    actor(Role.ADMIN);
    try {
      var all = reports.overview(null, null, null);
      assertEquals(
          List.of("2026-01", "2026-02", "2026-10"),
          all.byMonth().stream().map(RevenueController.Group::key).toList());
      assertEquals(0, BigDecimal.valueOf(600).compareTo(all.collected()));
      var oneDay = reports.overview(null, LocalDate.of(2026, 2, 28), LocalDate.of(2026, 2, 28));
      assertEquals(0, BigDecimal.valueOf(200).compareTo(oneDay.collected()));
      assertEquals(0, BigDecimal.valueOf(1000).compareTo(oneDay.outstanding()));
      assertEquals(1, oneDay.byBranch().size());
      var empty = reports.overview(null, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2));
      assertEquals(0, BigDecimal.ZERO.compareTo(empty.collected()));
      assertEquals(0, BigDecimal.valueOf(1000).compareTo(empty.outstanding()));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void staffCannotAccessRevenueReport() {
    actor(Role.STAFF);
    try {
      assertThrows(
          org.springframework.security.access.AccessDeniedException.class,
          () -> reports.overview(null, null, null));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
