package com.example.assistant.chat;

import com.example.assistant.mcp.SessionVerifier;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.stereotype.Component;

/** Only server-controlled enums, UUIDs and counters. No prompt, arguments, names or bearer. */
@Component
public class AssistantAudit {
  private static final Logger log = LoggerFactory.getLogger(AssistantAudit.class);
  private final ThreadLocal<Turn> current = new ThreadLocal<>();

  public final class Turn implements AutoCloseable {
    final UUID request;
    final SessionVerifier.Role role;
    final SessionVerifier.Mode mode;
    final long start = System.nanoTime();
    int models, tools;
    String outcome = "DEPENDENCY_UNAVAILABLE";

    Turn(UUID request, SessionVerifier.Role role, SessionVerifier.Mode mode) {
      this.request = request;
      this.role = role;
      this.mode = mode;
    }

    public void outcome(String value) {
      outcome = value;
    }

    public void close() {
      log.info(
          "assistant requestId={} role={} scope={} outcome={} modelCalls={} toolCalls={}"
              + " durationMs={}",
          request,
          role,
          mode,
          outcome,
          models,
          tools,
          (System.nanoTime() - start) / 1_000_000);
      current.remove();
    }
  }

  public Turn open(UUID request, SessionVerifier.Role role, SessionVerifier.Mode mode) {
    var turn = new Turn(request, role, mode);
    current.set(turn);
    return turn;
  }

  public void model() {
    var turn = current.get();
    if (turn != null) turn.models++;
  }

  public void tool() {
    var turn = current.get();
    if (turn != null) turn.tools++;
  }
}
