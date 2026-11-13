package com.example.assistant.model;

import com.example.assistant.chat.*;
import com.example.assistant.http.BoundedHttp;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeminiGateway implements ModelGateway {
  private final JsonMapper json;
  private final String apiKey, model;
  private final URI endpoint;
  private volatile long unavailableUntilNanos;

  @Value("${assistant.gemini.timeout-seconds:10}")
  private int timeoutSeconds = 10;

  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(3))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  public GeminiGateway(
      JsonMapper json,
      @Value("${GEMINI_API_KEY:}") String key,
      @Value("${GEMINI_MODEL:gemini-3.1-flash-lite}") String model,
      @Value("${assistant.gemini.origin:https://generativelanguage.googleapis.com}") String origin,
      @Value("${assistant.gemini.allow-local-test-server:false}") boolean allowLocal) {
    this.json = json;
    apiKey = key.trim();
    this.model = model;
    URI base = URI.create(origin);
    boolean official =
        "https".equals(base.getScheme())
            && "generativelanguage.googleapis.com".equals(base.getHost())
            && (base.getPort() == -1 || base.getPort() == 443);
    boolean local =
        allowLocal
            && "http".equals(base.getScheme())
            && Set.of("127.0.0.1", "localhost").contains(base.getHost());
    if ((!official && !local)
        || base.getUserInfo() != null
        || base.getQuery() != null
        || base.getFragment() != null
        || !(base.getPath().isEmpty() || base.getPath().equals("/"))
        || !model.matches("[a-zA-Z0-9][a-zA-Z0-9.-]{1,79}"))
      throw new IllegalArgumentException("Invalid Gemini endpoint/model configuration");
    endpoint =
        URI.create(origin.replaceAll("/+$", "") + "/v1beta/models/" + model + ":generateContent");
  }

  @Override
  public boolean configured() {
    return !apiKey.isBlank();
  }

  @jakarta.annotation.PostConstruct
  void logConfiguration() {
    org.slf4j.LoggerFactory.getLogger(GeminiGateway.class)
        .info(
            "assistant_provider model={} timeoutSeconds={} configured={}",
            model,
            Math.max(1, Math.min(25, timeoutSeconds)),
            configured());
  }

  public static Map<String, Object> decisionDeclaration() {
    return Map.of(
        "name",
        "assistant_decision",
        "description",
        "Chỉ quyết định trạng thái điều phối; không viết câu trả lời tự do.",
        "parametersJsonSchema",
        Map.of(
            "type",
            "object",
            "additionalProperties",
            false,
            "properties",
            Map.of(
                "action",
                Map.of(
                    "type",
                    "string",
                    "enum",
                    List.of(
                        "DONE",
                        "CLARIFY",
                        "OUT_OF_SCOPE",
                        "UNSUPPORTED_CAPABILITY",
                        "UNSUPPORTED_ACTION")),
                "reason",
                Map.of(
                    "type",
                    "string",
                    "enum",
                    List.of(
                        "NONE",
                        "NEED_STUDENT",
                        "NEED_CLASS",
                        "NEED_SESSION",
                        "NEED_DATE",
                        "NEED_EXAM",
                        "LEAD_PRIORITY_CONFIRM",
                        "REPHRASE"))),
            "required",
            List.of("action")));
  }

  @Override
  public Turn generate(
      String instruction,
      List<Map<String, Object>> contents,
      List<McpSchema.Tool> tools,
      Deadline deadline) {
    if (!configured()) throw new ChatFault(503, "MODEL_NOT_CONFIGURED");
    deadline.check();
    if (unavailableUntilNanos != 0 && unavailableUntilNanos - System.nanoTime() > 0)
      throw new ChatFault(503, "MODEL_COOLDOWN");
    var functions = new ArrayList<Map<String, Object>>();
    for (var tool : tools)
      functions.add(
          Map.of(
              "name",
              tool.name(),
              "description",
              tool.description(),
              "parametersJsonSchema",
              tool.inputSchema()));
    functions.add(decisionDeclaration());
    Map<String, Object> generation =
        new LinkedHashMap<>(Map.of("temperature", 0, "maxOutputTokens", 2048, "candidateCount", 1));
    if (model.startsWith("gemini-3"))
      generation.put("thinkingConfig", Map.of("thinkingLevel", "LOW"));
    var body =
        Map.of(
            "systemInstruction",
            Map.of("parts", List.of(Map.of("text", instruction))),
            "contents",
            contents,
            "tools",
            List.of(Map.of("functionDeclarations", functions)),
            "toolConfig",
            Map.of(
                "functionCallingConfig",
                Map.of(
                    "mode",
                    "ANY",
                    "allowedFunctionNames",
                    functions.stream().map(f -> f.get("name")).toList())),
            "generationConfig",
            generation);
    try {
      byte[] bytes = json.writeValueAsBytes(body);
      if (bytes.length > 131072) throw new ChatFault(400, "CONTEXT_TOO_LARGE");
      Duration maximum = Duration.ofSeconds(Math.max(1, Math.min(25, timeoutSeconds)));
      Duration timeout =
          deadline.remaining().compareTo(maximum) < 0 ? deadline.remaining() : maximum;
      var request =
          HttpRequest.newBuilder(endpoint)
              .timeout(timeout)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .header("x-goog-api-key", apiKey)
              .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
              .build();
      var response = BoundedHttp.send(http, request, 524288, timeout);
      deadline.check();
      if (response.statusCode() != 200)
        throw new ChatFault(
            response.statusCode() == 429 ? 429 : 503,
            switch (response.statusCode()) {
              case 401, 403 -> "MODEL_CREDENTIAL_REJECTED";
              case 429 -> "MODEL_RATE_LIMITED";
              case 400 -> "MODEL_REQUEST_REJECTED";
              case 404 -> "MODEL_NOT_FOUND";
              case 503 -> "MODEL_BUSY";
              default -> "MODEL_UNAVAILABLE";
            });
      return parse(json.readValue(response.body(), Object.class));
    } catch (ChatFault e) {
      if (Set.of("MODEL_BUSY", "MODEL_UNAVAILABLE").contains(e.code()))
        unavailableUntilNanos = System.nanoTime() + Duration.ofSeconds(30).toNanos();
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ChatFault(504, "TURN_TIMEOUT");
    } catch (Exception e) {
      for (Throwable cause = e; cause != null; cause = cause.getCause()) {
        if (cause instanceof java.util.concurrent.TimeoutException
            || cause instanceof java.net.http.HttpTimeoutException)
          throw unavailable(504, "MODEL_TIMEOUT");
        if (cause instanceof java.net.ConnectException
            || cause instanceof java.net.UnknownHostException
            || cause instanceof javax.net.ssl.SSLException)
          throw unavailable(503, "MODEL_NETWORK_UNAVAILABLE");
        if (cause instanceof IllegalStateException && "BODY_LIMIT".equals(cause.getMessage()))
          throw new ChatFault(503, "MODEL_RESPONSE_TOO_LARGE");
      }
      throw new ChatFault(503, "MODEL_UNAVAILABLE");
    }
  }

  private ChatFault unavailable(int status, String code) {
    unavailableUntilNanos = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    return new ChatFault(status, code);
  }

  @SuppressWarnings("unchecked")
  private Turn parse(Object response) {
    try {
      var root = (Map<String, Object>) response;
      var candidates = (List<?>) root.get("candidates");
      if (candidates == null || candidates.size() != 1) throw new IllegalArgumentException();
      var candidate = (Map<?, ?>) candidates.getFirst();
      if (!"STOP".equals(candidate.get("finishReason"))) throw new IllegalArgumentException();
      var content = (Map<String, Object>) candidate.get("content");
      if (!"model".equals(content.get("role"))) throw new IllegalArgumentException();
      var parts = (List<?>) content.get("parts");
      if (parts == null || parts.size() > 16) throw new IllegalArgumentException();
      var calls = new ArrayList<FunctionCall>();
      for (var raw : parts) {
        var part = (Map<?, ?>) raw;
        if (part.get("functionCall") instanceof Map<?, ?> call) {
          String name = (String) call.get("name");
          if (name == null || !name.matches("[a-z_]{1,64}")) throw new IllegalArgumentException();
          Map<String, Object> args =
              call.get("args") == null ? Map.of() : (Map<String, Object>) call.get("args");
          String id = (String) call.get("id");
          if (id != null && id.length() > 128) throw new IllegalArgumentException();
          calls.add(new FunctionCall(name, args, id));
        }
      }
      if (calls.isEmpty() || calls.size() > 4) throw new IllegalArgumentException();
      return new Turn(content, List.copyOf(calls));
    } catch (Exception e) {
      throw new ChatFault(503, "INVALID_MODEL_RESPONSE");
    }
  }

  @Override
  public String toString() {
    return "Gemini model adapter [credentials redacted]";
  }
}
