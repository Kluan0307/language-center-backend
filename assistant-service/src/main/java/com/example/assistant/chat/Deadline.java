package com.example.assistant.chat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Deadline {
  private final long end;
  private final AtomicBoolean cancelled = new AtomicBoolean();

  public Deadline(long started, Duration budget) {
    end = started + budget.toNanos();
  }

  public void check() {
    if (cancelled.get() || Thread.currentThread().isInterrupted() || System.nanoTime() - end >= 0)
      throw new ChatFault(504, "TURN_TIMEOUT");
  }

  public Duration remaining() {
    check();
    return Duration.ofNanos(end - System.nanoTime());
  }

  public void cancel() {
    cancelled.set(true);
  }
}
