package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;

class UsageLimiterTest {
  @Test
  void requestLimitsArePerAccountAndResetAtWindow() {
    var clock = new AtomicLong();
    var limiter = new UsageLimiter(2, 2, 3, 1, clock::get);
    limiter.request(1);
    limiter.request(1);
    assertEquals("RATE_LIMITED", assertThrows(ChatFault.class, () -> limiter.request(1)).code());
    limiter.request(2);
    clock.addAndGet(Duration.ofMinutes(1).toNanos());
    limiter.request(1);
  }

  @Test
  void modelBudgetCountsFailuresAndDoesNotExpandBySession() {
    var limiter = new UsageLimiter(5, 2, 10, 1);
    limiter.model(1);
    limiter.model(1);
    assertEquals(
        "MODEL_RATE_LIMITED", assertThrows(ChatFault.class, () -> limiter.model(1)).code());
    limiter.model(2);
  }

  @Test
  void globalBudgetSurvivesNewAccountsAndResets() {
    var clock = new AtomicLong();
    var limiter = new UsageLimiter(3, 3, 2, 1, clock::get);
    limiter.model(1);
    limiter.model(2);
    assertThrows(ChatFault.class, () -> limiter.model(3));
    clock.addAndGet(Duration.ofMinutes(1).toNanos());
    limiter.model(3);
  }

  @Test
  void concurrentLeaseIsNotResetWithRateWindow() throws Exception {
    var clock = new AtomicLong();
    var limiter = new UsageLimiter(2, 2, 3, 1, clock::get);
    var lease = limiter.enter(1);
    clock.addAndGet(Duration.ofMinutes(2).toNanos());
    assertThrows(ChatFault.class, () -> limiter.enter(1));
    lease.close();
    try (var next = limiter.enter(1)) {
      assertNotNull(next);
    }
  }

  @Test
  void parallelRequestsCannotOverspendModelBudget() throws Exception {
    var limiter = new UsageLimiter(30, 5, 100, 2);
    var passed = new AtomicInteger();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < 30; i++)
        executor.submit(
            () -> {
              try {
                limiter.model(1);
                passed.incrementAndGet();
              } catch (ChatFault ignored) {
              }
            });
    }
    assertEquals(5, passed.get());
  }

  @Test
  void identityBucketsAreBounded() {
    var limiter = new UsageLimiter(30, 12, 120, 2);
    for (int i = 0; i < 2000; i++) limiter.request(i);
    assertEquals(429, assertThrows(ChatFault.class, () -> limiter.request(2001)).status());
  }
}
