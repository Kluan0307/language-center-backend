package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.finance.common.ApiException;
import com.example.finance.payment.*;
import com.example.finance.security.*;
import com.example.finance.tuition.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PaymentUnitTest {
  PaymentRepository repository;
  TuitionService tuitions;
  PaymentService service;
  Tuition tuition;

  @BeforeEach
  void setup() {
    repository = mock(PaymentRepository.class);
    tuitions = mock(TuitionService.class);
    var corrections = mock(com.example.finance.ledger.PaymentCorrectionRepository.class);
    when(corrections.total(any())).thenReturn(BigDecimal.ZERO);
    service =
        new PaymentService(
            repository,
            tuitions,
            new BranchScope(),
            new com.example.finance.ledger.FinanceLedger(
                mock(jakarta.persistence.EntityManager.class), repository, corrections));
    tuition = new Tuition();
    tuition.setId(1L);
    tuition.setBranchId(2L);
    tuition.setAmount(new BigDecimal("3000000"));
    tuition.setDueDate(LocalDate.now());
    tuition.setPaidAmount(new BigDecimal("2000000"));
    when(tuitions.lock(1L)).thenReturn(tuition);
    when(repository.collected(1L)).thenReturn(new BigDecimal("2000000"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(5L, 6L, 2L, Role.STAFF, "cashier"), null, List.of()));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  PaymentDto.Request request(String amount) {
    return new PaymentDto.Request(
        1L, new BigDecimal(amount), Payment.PaymentMethod.CASH, null, "req-1", null);
  }

  @Test
  void rejectsOverCollection() {
    assertThrows(ApiException.class, () -> service.create(request("2000000")));
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void finalInstallmentUpdatesTuitionAndCollector() {
    PaymentDto payment = service.create(request("1000000"));
    assertEquals(Tuition.Status.PAID, tuition.getStatus());
    assertEquals(5L, payment.recordedBy());
    assertNotNull(payment.receiptNo());
  }

  @Test
  void retryDoesNotCollectTwice() {
    Payment p = new Payment();
    p.setTuitionId(1L);
    p.setAmount(new BigDecimal("1000000"));
    p.setPaymentMethod(Payment.PaymentMethod.CASH);
    when(repository.findByBranchIdAndRequestKey(2L, "req-1")).thenReturn(Optional.of(p));
    service.create(request("1000000"));
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void reusedKeyCannotChangeAmount() {
    Payment p = new Payment();
    p.setTuitionId(1L);
    p.setAmount(new BigDecimal("1000000"));
    p.setPaymentMethod(Payment.PaymentMethod.CASH);
    when(repository.findByBranchIdAndRequestKey(2L, "req-1")).thenReturn(Optional.of(p));
    assertThrows(ApiException.class, () -> service.create(request("500000")));
  }

  @Test
  void reusedKeyCannotChangeReferenceOrNote() {
    Payment p = new Payment();
    p.setTuitionId(1L);
    p.setAmount(new BigDecimal("1000000"));
    p.setPaymentMethod(Payment.PaymentMethod.CASH);
    when(repository.findByBranchIdAndRequestKey(2L, "req-1")).thenReturn(Optional.of(p));
    assertThrows(
        ApiException.class,
        () ->
            service.create(
                new PaymentDto.Request(
                    1L,
                    new BigDecimal("1000000"),
                    Payment.PaymentMethod.CASH,
                    "different",
                    "req-1",
                    null)));
    assertThrows(
        ApiException.class,
        () ->
            service.create(
                new PaymentDto.Request(
                    1L,
                    new BigDecimal("1000000"),
                    Payment.PaymentMethod.CASH,
                    null,
                    "req-1",
                    "different")));
    verify(repository, never()).saveAndFlush(any());
  }
}
