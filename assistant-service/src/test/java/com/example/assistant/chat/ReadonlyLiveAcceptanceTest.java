package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;

import com.example.assistant.model.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real Gemini, synthetic local Auth/business HTTP fixtures and real MCP SDK. No real center data.
 */
@EnabledIfEnvironmentVariable(named = "ASSISTANT_R8_LIVE", matches = "true")
class ReadonlyLiveAcceptanceTest {
  @Test
  void realModelMatrix() throws Exception {
    var json = JsonMapper.builder().build();
    var rows = new ArrayList<Map<String, Object>>();
    String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-3.1-flash-lite");
    var gateway =
        new GeminiGateway(
            json,
            Objects.requireNonNullElse(System.getenv("GEMINI_API_KEY"), ""),
            model,
            "https://generativelanguage.googleapis.com",
            false);
    String blocker = gateway.configured() ? null : "MODEL_NOT_CONFIGURED";
    var cases = ReadonlyAcceptanceTest.cases();
    for (var spec : cases) {
      var row = new LinkedHashMap<String, Object>();
      row.put("id", spec.get("id"));
      row.put("role", spec.get("role"));
      row.put("layer", "REAL_GEMINI_SYNTHETIC_MCP");
      if (blocker != null) {
        row.put("status", "NOT_RUN");
        row.put("reason", blocker);
      } else {
        long started = System.nanoTime();
        try {
          ReadonlyAcceptanceTest.execute(spec, gateway);
          row.put("status", "PASS");
        } catch (ChatFault fault) {
          row.put("status", "FAIL");
          row.put("code", fault.code());
          if (Set.of(
                  "MODEL_UNAVAILABLE",
                  "MODEL_RATE_LIMITED",
                  "MODEL_COOLDOWN",
                  "MODEL_NOT_CONFIGURED",
                  "MODEL_AUTH_FAILED",
                  "MODEL_BUSY",
                  "MODEL_TIMEOUT",
                  "MODEL_CREDENTIAL_REJECTED",
                  "MODEL_REQUEST_REJECTED",
                  "MODEL_NOT_FOUND",
                  "TURN_TIMEOUT")
              .contains(fault.code())) {
            blocker = fault.code();
            row.put("status", "BLOCKED");
          }
        } catch (AssertionError e) {
          row.put("status", "FAIL");
          row.put("reason", "ORACLE_MISMATCH");
        } catch (Exception e) {
          row.put("status", "FAIL");
          row.put("reason", e.getClass().getSimpleName());
        }
        row.put("elapsedMs", (System.nanoTime() - started) / 1_000_000);
      }
      rows.add(row);
      Files.createDirectories(Path.of("target"));
      var report = new LinkedHashMap<String, Object>();
      report.put("runAt", Instant.now().toString());
      report.put("model", model);
      report.put("data", "Synthetic Auth/business fixtures only; not live MySQL");
      report.put("cases", rows);
      report.put(
          "status",
          blocker != null
              ? "BLOCKED"
              : rows.size() < cases.size()
                  ? "RUNNING"
                  : rows.stream().allMatch(r -> r.get("status").equals("PASS")) ? "PASS" : "FAIL");
      Files.writeString(
          Path.of("target/r8-live-acceptance.json"),
          json.writerWithDefaultPrettyPrinter().writeValueAsString(report));
      System.out.println("R8 LIVE " + row.get("id") + " " + row.get("status"));
    }
    assertTrue(
        rows.stream().allMatch(r -> r.get("status").equals("PASS")),
        "R8 live matrix incomplete; see sanitized target/r8-live-acceptance.json");
  }
}
