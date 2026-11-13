package com.example.assistant.mcp;

import io.modelcontextprotocol.client.*;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.*;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AssistantMcpRuntime {
  private final BackendGateway backend;
  private final SessionVerifier verifier;
  private final ToolCatalog catalog;
  private final McpJsonMapper json;
  private final boolean enabled;
  private final int capacity;
  private final Duration ttl;
  private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
  private boolean stopping;

  public AssistantMcpRuntime(
      BackendGateway backend,
      SessionVerifier verifier,
      ToolCatalog catalog,
      JsonMapper mapper,
      @Value("${assistant.mcp.enabled:false}") boolean enabled,
      @Value("${assistant.mcp.max-connections:16}") int capacity,
      @Value("${assistant.mcp.session-ttl-seconds:120}") int ttlSeconds) {
    if (capacity < 1 || capacity > 64 || ttlSeconds < 1 || ttlSeconds > 600)
      throw new IllegalArgumentException("Invalid MCP resource limits");
    this.backend = backend;
    this.verifier = verifier;
    this.catalog = catalog;
    json = new JacksonMcpJsonMapper(mapper);
    this.enabled = enabled;
    this.capacity = capacity;
    ttl = Duration.ofSeconds(ttlSeconds);
  }

  public synchronized boolean enabled() {
    return enabled && !stopping;
  }

  /** Trusted host entry point, not an HTTP endpoint and not exposed as an MCP tool. */
  public synchronized Session open(String bearer, SessionVerifier.Mode mode, Long branch) {
    if (!enabled || stopping) throw new McpFault("FEATURE_DISABLED");
    reap();
    if (sessions.size() >= capacity) throw new McpFault("CAPACITY_EXCEEDED");
    var caller = verifier.authenticate(bearer, mode, branch);
    var session = new Session(caller);
    sessions.put(session.id, session);
    try {
      session.start();
      return session;
    } catch (Exception e) {
      session.close();
      if (e instanceof McpFault f) throw f;
      throw new McpFault("PROTOCOL_UNAVAILABLE");
    }
  }

  @Scheduled(fixedDelay = 10000)
  public void reap() {
    for (var session : List.copyOf(sessions.values())) if (session.expired()) session.close();
  }

  @PreDestroy
  public synchronized void shutdown() {
    stopping = true;
    for (var s : List.copyOf(sessions.values())) s.close();
  }

  public final class Session implements AutoCloseable {
    private final UUID id = UUID.randomUUID();
    private final SessionVerifier.Caller caller;
    private final long expires = System.nanoTime() + ttl.toNanos();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicInteger calls = new AtomicInteger();
    private McpSyncServer server;
    private McpSyncClient client;

    private Session(SessionVerifier.Caller caller) {
      this.caller = caller;
    }

    private void start() {
      var transport = new LocalMcpTransport(json);
      var specs =
          catalog.forRole(caller.role()).stream()
              .map(
                  def ->
                      McpServerFeatures.SyncToolSpecification.builder()
                          .tool(def.tool())
                          .callHandler((exchange, request) -> execute(def, request.arguments()))
                          .build())
              .toList();
      server =
          McpServer.sync(transport)
              .serverInfo("language-center-readonly", "1.0.0")
              .jsonMapper(json)
              .requestTimeout(Duration.ofSeconds(20))
              .validateToolInputs(true)
              .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
              .tools(specs)
              .build();
      client =
          McpClient.sync(transport.client())
              .requestTimeout(Duration.ofSeconds(20))
              .clientInfo(new McpSchema.Implementation("language-center-host", "1.0.0"))
              .build();
      client.initialize();
    }

    private boolean expired() {
      return System.nanoTime() - expires >= 0;
    }

    private void check() {
      if (closed.get()) throw new McpFault("SESSION_CLOSED");
      if (expired()) throw new McpFault("SESSION_EXPIRED");
    }

    public synchronized List<McpSchema.Tool> tools() {
      check();
      verifier.revalidate(caller);
      return client.listTools().tools();
    }

    public SessionVerifier.Identity identity() {
      return caller.identity();
    }

    public synchronized void revalidate() {
      check();
      verifier.revalidate(caller);
      check();
    }

    public Map<String, Object> scope() {
      return Map.of(
          "mode",
          caller.mode().name(),
          "branchIds",
          caller.scopeBranches(),
          "label",
          caller.mode() == SessionVerifier.Mode.WHOLE_SYSTEM
              ? "Toàn hệ thống"
              : caller.mode() == SessionVerifier.Mode.ALL_ASSIGNED
                  ? "Các chi nhánh được phân công"
                  : "Chi nhánh " + caller.branch());
    }

    public synchronized McpSchema.CallToolResult call(String name, Map<String, Object> arguments) {
      try {
        check();
        return client.callTool(new McpSchema.CallToolRequest(name, arguments));
      } catch (McpFault e) {
        return error(e.code());
      } catch (Exception e) {
        return error("INVALID_TOOL_CALL");
      }
    }

    // Package-private for protocol tests only; the host API above is the C4 integration seam.
    McpSyncClient protocolClient() {
      return client;
    }

    private McpSchema.CallToolResult execute(
        ToolCatalog.Definition def, Map<String, Object> arguments) {
      try {
        check();
        if (calls.incrementAndGet() > 8) throw new McpFault("CALL_LIMIT_EXCEEDED");
        verifier.revalidate(caller);
        check();
        if (!def.roles().contains(caller.role())) throw new McpFault("FORBIDDEN");
        var params = def.validate(arguments);
        String endpoint = def.endpoint();
        if (def.name().equals("get_learning_results")) {
          var view = params.remove("view");
          if (view.equals("CURRENT")) {
            if (caller.role() == SessionVerifier.Role.STAFF) throw new McpFault("FORBIDDEN");
            if (!params.containsKey("examId")
                || params.containsKey("studentId")
                || params.containsKey("enrollmentId")) throw new McpFault("INVALID_ARGUMENTS");
            endpoint = "current-grades";
          } else {
            if (caller.role() == SessionVerifier.Role.TEACHER) throw new McpFault("FORBIDDEN");
            if (!params.containsKey("studentId")
                || !params.containsKey("enrollmentId")
                || params.containsKey("examId")) throw new McpFault("INVALID_ARGUMENTS");
            endpoint = "published-grades";
          }
        }
        params.put("scopeMode", caller.mode().name());
        if (caller.branch() != null) params.put("branchId", caller.branch());
        String prefix =
            switch (def.target()) {
              case ACADEMIC -> "academic";
              case FINANCE -> "finance";
              case TEACHING -> "teaching";
              default -> throw new McpFault("FORBIDDEN");
            };
        var result =
            backend.get(
                def.target(),
                "/api/v1/" + prefix + "/assistant-query/" + endpoint,
                params,
                caller.bearer(),
                caller.branch());
        check();
        validateResult(result, def.name());
        verifier.revalidate(caller);
        check();
        return McpSchema.CallToolResult.builder()
            .isError(false)
            .structuredContent(result)
            .addTextContent(json.writeValueAsString(result))
            .build();
      } catch (McpFault e) {
        return error(e.code());
      } catch (Exception e) {
        return error("DEPENDENCY_UNAVAILABLE");
      }
    }

    private void validateResult(Map<String, Object> result, String source) {
      if (!Set.of("ANSWER", "NO_RESULTS").contains(result.get("status"))
          || !source.equals(result.get("source"))
          || !(result.get("scope") instanceof Map<?, ?> scope)
          || !caller.mode().name().equals(scope.get("mode"))
          || !(scope.get("branchIds") instanceof List<?> branches))
        throw new McpFault("INVALID_BACKEND_RESPONSE");
      try {
        var actual =
            branches.stream()
                .map(n -> new java.math.BigDecimal(n.toString()).longValueExact())
                .sorted()
                .toList();
        if (!actual.equals(caller.scopeBranches())) throw new McpFault("INVALID_BACKEND_RESPONSE");
      } catch (Exception e) {
        throw new McpFault("INVALID_BACKEND_RESPONSE");
      }
      if (!Set.of(
              "requestId",
              "status",
              "scope",
              "observedAt",
              "source",
              "data",
              "warnings",
              "nextPage",
              "hasMore")
          .containsAll(result.keySet())) throw new McpFault("INVALID_BACKEND_RESPONSE");
    }

    @Override
    public synchronized void close() {
      if (!closed.compareAndSet(false, true)) return;
      sessions.remove(id);
      try {
        if (client != null) client.closeGracefully();
      } catch (Exception ignored) {
      }
      try {
        if (server != null) server.closeGracefully();
      } catch (Exception ignored) {
      }
    }

    @Override
    public String toString() {
      return "Private MCP session [redacted]";
    }
  }

  private McpSchema.CallToolResult error(String code) {
    var data = Map.of("status", "ERROR", "code", code, "requestId", UUID.randomUUID().toString());
    return McpSchema.CallToolResult.builder()
        .isError(true)
        .structuredContent(data)
        .addTextContent(code)
        .build();
  }
}
