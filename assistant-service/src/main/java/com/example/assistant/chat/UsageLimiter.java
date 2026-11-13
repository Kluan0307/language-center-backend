package com.example.assistant.chat;

import java.time.Duration;
import java.util.*;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Per-account, not per token. Bounded memory; limits apply to one service instance. */
@Component
public class UsageLimiter {
  private static final long WINDOW = Duration.ofMinutes(1).toNanos();
  private final int requests, models, globalModels, parallel;
  private final LongSupplier clock;
  private final Map<Long, Bucket> accounts = new HashMap<>();
  private long globalStart;
  private int globalCount;

  private static final class Bucket {
    long start;
    int requests, models, active;

    Bucket(long now) {
      start = now;
    }
  }

  @org.springframework.beans.factory.annotation.Autowired
  public UsageLimiter(
      @Value("${assistant.limits.requests-per-minute:30}") int requests,
      @Value("${assistant.limits.model-calls-per-minute:12}") int models,
      @Value("${assistant.limits.global-model-calls-per-minute:120}") int globalModels,
      @Value("${assistant.limits.concurrent-per-account:2}") int parallel) {
    this(requests, models, globalModels, parallel, System::nanoTime);
  }

  UsageLimiter(int requests, int models, int globalModels, int parallel, LongSupplier clock) {
    if (requests < 1
        || requests > 600
        || models < 1
        || models > 120
        || globalModels < 1
        || globalModels > 2000
        || parallel < 1
        || parallel > 8) throw new IllegalArgumentException("Invalid assistant rate limits");
    this.requests = requests;
    this.models = models;
    this.globalModels = globalModels;
    this.parallel = parallel;
    this.clock = clock;
    globalStart = clock.getAsLong();
  }

  private Bucket bucket(long account) {
    long now = clock.getAsLong();
    accounts
        .entrySet()
        .removeIf(e -> e.getValue().active == 0 && now - e.getValue().start >= WINDOW);
    var bucket = accounts.get(account);
    if (bucket == null) {
      if (accounts.size() >= 2000) throw new ChatFault(429, "RATE_LIMITED");
      bucket = new Bucket(now);
      accounts.put(account, bucket);
    }
    if (now - bucket.start >= WINDOW) {
      bucket.start = now;
      bucket.requests = 0;
      bucket.models = 0;
    }
    return bucket;
  }

  public synchronized void request(long account) {
    var bucket = bucket(account);
    if (bucket.requests >= requests) throw new ChatFault(429, "RATE_LIMITED");
    bucket.requests++;
  }

  public synchronized AutoCloseable enter(long account) {
    var bucket = bucket(account);
    if (bucket.active >= parallel) throw new ChatFault(429, "CAPACITY_EXCEEDED");
    bucket.active++;
    return () -> {
      synchronized (UsageLimiter.this) {
        bucket.active--;
      }
    };
  }

  public synchronized void model(long account) {
    var bucket = bucket(account);
    long now = clock.getAsLong();
    if (now - globalStart >= WINDOW) {
      globalStart = now;
      globalCount = 0;
    }
    if (bucket.models >= models || globalCount >= globalModels)
      throw new ChatFault(429, "MODEL_RATE_LIMITED");
    bucket.models++;
    globalCount++;
  }
}
