package com.example.assistant.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.assistant.chat.*;
import com.sun.net.httpserver.*;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;

class GeminiGatewayTest {
  HttpServer fixture;
  JsonMapper json = JsonMapper.builder().build();
  GeminiGateway client;
  int status = 200, count;
  boolean plain = false, oversized = false;
  Map<?, ?> seen;
  String key, path;

  @BeforeEach
  void setup() throws Exception {
    fixture = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    fixture.createContext(
        "/",
        x -> {
          count++;
          key = x.getRequestHeaders().getFirst("x-goog-api-key");
          path = x.getRequestURI().toString();
          seen = json.readValue(x.getRequestBody().readAllBytes(), Map.class);
          Object content =
              plain
                  ? Map.of("role", "model", "parts", List.of(Map.of("text", "UNTRUSTED_FREE_TEXT")))
                  : Map.of(
                      "role",
                      "model",
                      "parts",
                      List.of(
                          Map.of(
                              "functionCall",
                              Map.of(
                                  "name",
                                  "search_courses",
                                  "args",
                                  Map.of("maxTuition", 5000000, "maxInclusive", false),
                                  "id",
                                  "call-sample"),
                              "thoughtSignature",
                              "opaque-signature")));
          Object body =
              status == 200
                  ? Map.of(
                      "candidates", List.of(Map.of("finishReason", "STOP", "content", content)))
                  : Map.of("error", Map.of("message", "DO_NOT_LEAK_FAKE_SECRET"));
          byte[] bytes =
              oversized
                  ? ("x".repeat(600000)).getBytes(StandardCharsets.UTF_8)
                  : json.writeValueAsBytes(body);
          x.getResponseHeaders().set("Content-Type", "application/json");
          x.sendResponseHeaders(status, bytes.length);
          try (var out = x.getResponseBody()) {
            out.write(bytes);
          }
        });
    fixture.start();
    client = client("fake-api-secret", true);
  }

  GeminiGateway client(String apiKey, boolean allow) {
    return new GeminiGateway(
        json,
        apiKey,
        "gemini-3.8-flash",
        "http://127.0.0.1:" + fixture.getAddress().getPort(),
        allow);
  }

  @AfterEach
  void cleanup() {
    fixture.stop(0);
  }

  List<McpSchema.Tool> tools() {
    return List.of(
        McpSchema.Tool.builder()
            .name("search_courses")
            .description("Sample courses")
            .inputSchema(
                Map.of(
                    "type",
                    "object",
                    "properties",
                    Map.of("maxTuition", Map.of("type", "number")),
                    "additionalProperties",
                    false))
            .build());
  }

  ModelGateway.Turn call() {
    return client.generate(
        "Synthetic system instruction",
        List.of(
            Map.of(
                "role",
                "user",
                "parts",
                List.of(Map.of("text", "Khóa giao tiếp mẫu dưới 5 triệu?")))),
        tools(),
        new Deadline(System.nanoTime(), Duration.ofSeconds(5)));
  }

  @Test
  void usesOfficialRestShapeHeaderKeyAndClosedToolSchemas() {
    var result = call();
    assertEquals("call-sample", result.calls().getFirst().id());
    assertEquals(
        "opaque-signature",
        ((Map<?, ?>) ((List<?>) result.content().get("parts")).getFirst()).get("thoughtSignature"));
    assertEquals("fake-api-secret", key);
    assertFalse(path.contains("secret"));
    assertTrue(path.endsWith("/models/gemini-3.8-flash:generateContent"));
    assertFalse(seen.toString().contains("fake-api-secret"));
    assertTrue(seen.toString().contains("parametersJsonSchema"));
    assertTrue(seen.toString().contains("assistant_decision"));
    assertTrue(seen.toString().contains("allowedFunctionNames"));
  }

  @Test
  void freeTextIsNeverAnAnswer() {
    plain = true;
    assertEquals("INVALID_MODEL_RESPONSE", assertThrows(ChatFault.class, this::call).code());
  }

  @Test
  void quotaAndCredentialsAreSafeErrorsAndNeverRetried() {
    for (int code : List.of(429, 403, 404, 500)) {
      status = code;
      var failure = assertThrows(ChatFault.class, this::call);
      assertFalse(failure.toString().contains("DO_NOT_LEAK"));
      assertFalse(failure.toString().contains("fake-api-secret"));
      if (code == 429) assertEquals("MODEL_RATE_LIMITED", failure.code());
    }
    assertEquals(4, count);
  }

  @Test
  void oversizedResponseIsBounded() {
    oversized = true;
    assertEquals("MODEL_RESPONSE_TOO_LARGE", assertThrows(ChatFault.class, this::call).code());
  }

  @Test
  void overloadedProviderIsNotCalledAgainDuringCooldownAndCanRecover() {
    status = 503;
    assertEquals("MODEL_BUSY", assertThrows(ChatFault.class, this::call).code());
    status = 200;
    assertEquals("MODEL_COOLDOWN", assertThrows(ChatFault.class, this::call).code());
    assertEquals(1, count);
    org.springframework.test.util.ReflectionTestUtils.setField(
        client, "unavailableUntilNanos", System.nanoTime() - 1);
    assertEquals("search_courses", call().calls().getFirst().name());
    assertEquals(2, count);
  }

  @Test
  void missingKeyAndUnsafeOriginsFailBeforeRequests() {
    client = client("", true);
    assertFalse(client.configured());
    assertEquals("MODEL_NOT_CONFIGURED", assertThrows(ChatFault.class, this::call).code());
    assertThrows(IllegalArgumentException.class, () -> client("fake", false));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GeminiGateway(
                json, "fake", "gemini-3.8-flash", "https://untrusted.example", false));
    assertEquals(0, count);
  }

  @Test
  void oversizedRequestIsRejectedWithoutProviderCall() {
    assertEquals(
        "CONTEXT_TOO_LARGE",
        assertThrows(
                ChatFault.class,
                () ->
                    client.generate(
                        "sample",
                        List.of(
                            Map.of(
                                "role",
                                "user",
                                "parts",
                                List.of(Map.of("text", "a".repeat(150000))))),
                        tools(),
                        new Deadline(System.nanoTime(), Duration.ofSeconds(5))))
            .code());
    assertEquals(0, count);
  }

  @Test
  void expiredDeadlineDoesNotSendKey() {
    assertEquals(
        "TURN_TIMEOUT",
        assertThrows(
                ChatFault.class,
                () ->
                    client.generate(
                        "sample",
                        List.of(),
                        tools(),
                        new Deadline(
                            System.nanoTime() - Duration.ofSeconds(2).toNanos(),
                            Duration.ofSeconds(1))))
            .code());
    assertEquals(0, count);
  }
}
