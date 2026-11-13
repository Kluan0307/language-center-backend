package com.example.assistant.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.*;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;

class McpRuntimeTest {
  private final JsonMapper json = JsonMapper.builder().build();
  private HttpServer fixture;
  private AssistantMcpRuntime runtime;
  private BackendGateway gateway;
  private SessionVerifier verifier;
  private volatile String role = "ADMIN";
  private volatile boolean active = true, changePassword = false;
  private volatile List<Long> branches = List.of(1L, 2L);
  private volatile int authStatus = 200, dataStatus = 200;
  private volatile boolean redirect = false, wrongScope = false, wrongSource = false;
  private volatile boolean oversized = false, slowBody = false;
  private final List<Seen> seen = new CopyOnWriteArrayList<>();

  private record Seen(
      String method, String path, Map<String, String> query, String auth, String branch) {}

  private static final String BEARER = "Bearer fake-test-session-not-a-real-key";
  private static final String SECOND = "Bearer second-fake-test-session";

  @BeforeEach
  void setup() throws Exception {
    fixture = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
    fixture.createContext("/", this::serve);
    fixture.start();
    String base = "http://127.0.0.1:" + fixture.getAddress().getPort();
    gateway = new BackendGateway(json, base, base, base, base);
    verifier = new SessionVerifier(gateway);
    runtime = new AssistantMcpRuntime(gateway, verifier, new ToolCatalog(), json, true, 4, 120);
  }

  @AfterEach
  void cleanup() {
    runtime.shutdown();
    fixture.stop(0);
  }

  private void serve(HttpExchange x) throws java.io.IOException {
    var query = new LinkedHashMap<String, String>();
    String raw = x.getRequestURI().getRawQuery();
    if (raw != null)
      for (String part : raw.split("&")) {
        String[] pair = part.split("=", 2);
        query.put(
            URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
            pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
      }
    String path = x.getRequestURI().getPath();
    seen.add(
        new Seen(
            x.getRequestMethod(),
            path,
            query,
            x.getRequestHeaders().getFirst("Authorization"),
            x.getRequestHeaders().getFirst("X-Branch-Id")));
    boolean auth = path.equals("/api/v1/auth/me");
    int status = auth ? authStatus : dataStatus;
    boolean second = SECOND.equals(x.getRequestHeaders().getFirst("Authorization"));
    if (auth && !second && !BEARER.equals(x.getRequestHeaders().getFirst("Authorization")))
      status = 401;
    if (redirect && !auth) {
      x.getResponseHeaders().add("Location", "/forbidden-target");
      status = 302;
    }
    Map<String, Object> body;
    if (status != 200) body = Map.of("message", "PRIVATE_ERROR_BODY " + BEARER);
    else if (auth)
      body =
          second
              ? Map.of(
                  "accountId",
                  200,
                  "profileId",
                  12,
                  "username",
                  "second-operator",
                  "role",
                  "STAFF",
                  "status",
                  true,
                  "mustChangePassword",
                  false,
                  "branchId",
                  2,
                  "branchIds",
                  List.of(2L))
              : Map.of(
                  "accountId",
                  100,
                  "profileId",
                  11,
                  "username",
                  "operator",
                  "role",
                  role,
                  "status",
                  active,
                  "mustChangePassword",
                  changePassword,
                  "branchId",
                  1,
                  "branchIds",
                  branches);
    else {
      String source =
          switch (path.substring(path.lastIndexOf('/') + 1)) {
            case "courses" -> "search_courses";
            case "classes" -> "search_classes";
            case "students" -> "find_students";
            case "student-classes" -> "get_student_classes";
            case "student-statistics" -> "get_student_statistics";
            case "sessions" -> "list_sessions";
            case "attendance" -> "get_session_attendance";
            case "current-grades", "published-grades" -> "get_learning_results";
            case "tuitions" -> "search_tuitions";
            case "receipts" -> "search_receipts";
            case "priority-leads" -> "list_priority_leads";
            case "usage-help" -> "get_usage_help";
            default -> "unexpected";
          };
      String mode = query.getOrDefault("scopeMode", "ACTIVE_BRANCH");
      List<Long> ids =
          mode.equals("WHOLE_SYSTEM")
              ? List.of()
              : mode.equals("ALL_ASSIGNED")
                  ? branches
                  : List.of(Long.parseLong(query.get("branchId")));
      body =
          Map.of(
              "requestId",
              "fixture-id",
              "status",
              "ANSWER",
              "scope",
              Map.of(
                  "mode", mode, "branchIds", wrongScope ? List.of(999) : ids, "label", "Fixture"),
              "observedAt",
              OffsetDateTime.now().toString(),
              "source",
              wrongSource ? "wrong" : source,
              "data",
              Map.of("content", List.of(Map.of("id", 1, "name", "Fixture")), "totalElements", 1),
              "warnings",
              List.of(),
              "hasMore",
              false);
    }
    if (oversized && !auth) body = Map.of("data", "x".repeat(300000));
    byte[] bytes = json.writeValueAsBytes(body);
    x.getResponseHeaders().set("Content-Type", "application/json");
    x.sendResponseHeaders(status, bytes.length);
    if (slowBody && !auth)
      try {
        Thread.sleep(7000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    try (var output = x.getResponseBody()) {
      output.write(bytes);
    }
  }

  private AssistantMcpRuntime.Session open() {
    return runtime.open(BEARER, SessionVerifier.Mode.ACTIVE_BRANCH, 1L);
  }

  private List<Seen> dataCalls() {
    return seen.stream().filter(s -> !s.path().equals("/api/v1/auth/me")).toList();
  }

  private void error(McpSchema.CallToolResult result) {
    assertEquals(Boolean.TRUE, result.isError());
    assertFalse(result.toString().contains(BEARER));
    assertFalse(result.toString().contains("PRIVATE_ERROR_BODY"));
  }

  @Test
  void realSdkNegotiatesListsAndCallsToolsOverSerializedJsonRpc() {
    try (var s = open()) {
      assertTrue(s.protocolClient().isInitialized());
      assertNotNull(s.protocolClient().getCurrentInitializationResult().protocolVersion());
      assertEquals(12, s.tools().size());
      var r = s.call("search_courses", Map.of("maxTuition", 5000000, "maxInclusive", false));
      assertEquals(false, r.isError());
      assertNotNull(r.structuredContent());
      var call = dataCalls().getFirst();
      assertEquals("GET", call.method());
      assertEquals("5000000", call.query().get("maxTuition"));
      assertEquals("false", call.query().get("maxInclusive"));
      assertEquals("ACTIVE_BRANCH", call.query().get("scopeMode"));
      assertEquals("1", call.query().get("branchId"));
      assertEquals("1", call.branch());
      assertEquals(BEARER, call.auth());
      assertFalse(r.toString().contains(BEARER));
    }
  }

  @Test
  void teacherDiscoveryHidesOperationsAndDirectProtocolCallsStillCannotUseThem() {
    role = "TEACHER";
    try (var s = open()) {
      var names = s.tools().stream().map(McpSchema.Tool::name).toList();
      assertEquals(8, names.size());
      assertFalse(names.contains("search_tuitions"));
      assertFalse(names.contains("list_priority_leads"));
      assertFalse(names.contains("get_student_statistics"));
      assertThrows(
          Exception.class,
          () ->
              s.protocolClient()
                  .callTool(new McpSchema.CallToolRequest("search_receipts", Map.of())));
      assertEquals(0, dataCalls().size());
      error(
          s.call(
              "get_learning_results",
              Map.of("view", "PUBLISHED", "studentId", 11, "enrollmentId", 111)));
      assertEquals(0, dataCalls().size());
    }
  }

  @Test
  void schemasAreClosedReadOnlyAndContainNoScopeIdentityOrUrls() throws Exception {
    var exported = new LinkedHashMap<String, Object>();
    for (var r : SessionVerifier.Role.values()) {
      role = r.name();
      try (var s = open()) {
        var tools = s.tools();
        exported.put(role, tools);
        for (var tool : tools) {
          assertEquals(true, tool.annotations().readOnlyHint());
          assertEquals(false, tool.annotations().destructiveHint());
          assertEquals(false, tool.annotations().openWorldHint());
          String schema = json.writeValueAsString(tool.inputSchema());
          assertTrue(schema.contains("\"additionalProperties\":false"));
          for (String key :
              List.of("bearer", "scopeMode", "branchId", "role", "url", "sql", "token"))
            assertFalse(((Map<?, ?>) tool.inputSchema().get("properties")).containsKey(key));
        }
      }
    }
    Files.createDirectories(Path.of("target"));
    Files.writeString(
        Path.of("target/mcp-tool-catalog.json"),
        json.writerWithDefaultPrettyPrinter().writeValueAsString(exported));
  }

  @Test
  void unknownArgumentsAndBadTypesCannotReachBackend() {
    try (var s = open()) {
      for (var args :
          List.<Map<String, Object>>of(
              Map.of("scopeMode", "WHOLE_SYSTEM"),
              Map.of("branchId", 3),
              Map.of("url", "http://127.0.0.1/private"),
              Map.of("sql", "select *"),
              Map.of("size", 21),
              Map.of("page", 0.5),
              Map.of("maxInclusive", "false"),
              Map.of("maxTuition", -1),
              Map.of("keyword", "a".repeat(121)))) error(s.call("search_courses", args));
      error(s.call("get_student_classes", Map.of()));
      assertEquals(0, dataCalls().size());
    }
  }

  @Test
  void injectedMethodOrToolNamesHaveNoGeneralHttpOrMutationCapability() {
    try (var s = open()) {
      for (String tool :
          List.of("execute_sql", "http_request", "delete_student", "../auth/me", "POST /payments"))
        error(s.call(tool, Map.of()));
      assertEquals(0, dataCalls().size());
    }
  }

  @Test
  void allAssignedAndWholeSystemRequireTrustedExplicitHostContext() {
    try (var s = runtime.open(BEARER, SessionVerifier.Mode.ALL_ASSIGNED, null)) {
      assertFalse(s.call("search_courses", Map.of()).isError());
      assertEquals("ALL_ASSIGNED", dataCalls().getLast().query().get("scopeMode"));
      assertNull(dataCalls().getLast().branch());
    }
    assertThrows(McpFault.class, () -> runtime.open(BEARER, SessionVerifier.Mode.ALL_ASSIGNED, 1L));
    assertThrows(
        McpFault.class, () -> runtime.open(BEARER, SessionVerifier.Mode.ACTIVE_BRANCH, 3L));
    role = "STAFF";
    assertThrows(
        McpFault.class, () -> runtime.open(BEARER, SessionVerifier.Mode.ALL_ASSIGNED, null));
    assertThrows(
        McpFault.class, () -> runtime.open(BEARER, SessionVerifier.Mode.WHOLE_SYSTEM, null));
    role = "SUPER_ADMIN";
    try (var s = runtime.open(BEARER, SessionVerifier.Mode.WHOLE_SYSTEM, null)) {
      assertFalse(s.call("search_courses", Map.of()).isError());
      assertEquals("WHOLE_SYSTEM", dataCalls().getLast().query().get("scopeMode"));
      assertFalse(dataCalls().getLast().query().containsKey("branchId"));
    }
  }

  @Test
  void logoutAndAccountLockAreRecheckedBeforeDirectToolCalls() {
    try (var s = open()) {
      authStatus = 401;
      error(s.protocolClient().callTool(new McpSchema.CallToolRequest("search_courses", Map.of())));
      assertEquals(0, dataCalls().size());
    }
    authStatus = 200;
    try (var s = open()) {
      active = false;
      error(s.call("search_courses", Map.of()));
      assertEquals(0, dataCalls().size());
    }
  }

  @Test
  void changedAssignmentsRoleAndRequiredPasswordChangeCannotReuseSession() {
    try (var s = open()) {
      branches = List.of(1L);
      error(s.call("search_courses", Map.of()));
      assertEquals(0, dataCalls().size());
    }
    branches = List.of(1L, 2L);
    try (var s = open()) {
      role = "TEACHER";
      error(s.call("search_receipts", Map.of()));
      assertEquals(0, dataCalls().size());
    }
    role = "ADMIN";
    try (var s = open()) {
      changePassword = true;
      error(s.call("search_courses", Map.of()));
      assertEquals(0, dataCalls().size());
    }
  }

  @Test
  void tokenIsValidatedByAuthAndNeverTransportedAsToolArgument() {
    for (String token : Arrays.asList(null, "wrong", "Bearer x\r\nX-Test:bad"))
      assertThrows(
          McpFault.class, () -> runtime.open(token, SessionVerifier.Mode.ACTIVE_BRANCH, 1L));
    assertTrue(seen.isEmpty());
    authStatus = 401;
    assertThrows(McpFault.class, this::open);
    assertEquals(0, dataCalls().size());
    authStatus = 200;
    role = "STUDENT";
    assertThrows(McpFault.class, this::open);
  }

  @Test
  void downstreamRejectsAreNotRetriedOrTurnedIntoZeroAndDoNotLeakBodies() {
    try (var s = open()) {
      for (int code : List.of(400, 401, 403, 404, 429, 500)) {
        dataStatus = code;
        error(s.call("search_courses", Map.of()));
      }
      assertEquals(6, dataCalls().size());
    }
  }

  @Test
  void redirectsAreNotFollowed() {
    try (var s = open()) {
      redirect = true;
      error(s.call("search_courses", Map.of()));
      assertEquals(1, dataCalls().size());
      assertFalse(seen.stream().anyMatch(r -> r.path().equals("/forbidden-target")));
    }
  }

  @Test
  void backendScopeOrSourceMismatchFailsClosed() {
    try (var s = open()) {
      wrongScope = true;
      error(s.call("search_courses", Map.of()));
      wrongScope = false;
      wrongSource = true;
      error(s.call("search_courses", Map.of()));
    }
  }

  @Test
  void learningResultViewSelectsOnlyAuthorizedFixedEndpoint() {
    role = "STAFF";
    try (var s = open()) {
      error(s.call("get_learning_results", Map.of("view", "CURRENT", "examId", 1)));
      assertTrue(dataCalls().isEmpty());
      assertFalse(
          s.call("get_learning_results", Map.of("studentId", 11, "enrollmentId", 111)).isError());
      assertTrue(dataCalls().getLast().path().endsWith("/published-grades"));
    }
    role = "TEACHER";
    try (var s = open()) {
      assertFalse(s.call("get_learning_results", Map.of("examId", 1)).isError());
      assertTrue(dataCalls().getLast().path().endsWith("/current-grades"));
    }
  }

  @Test
  void allRegisteredToolsUseTheirFiniteC2GetMappings() {
    try (var s = open()) {
      Map<String, Map<String, Object>> inputs =
          Map.of(
              "get_student_classes",
              Map.of("studentId", 11),
              "list_sessions",
              Map.of("from", "2026-11-13", "to", "2026-11-14"),
              "get_session_attendance",
              Map.of("sessionId", 1),
              "get_learning_results",
              Map.of("studentId", 11, "enrollmentId", 111),
              "get_usage_help",
              Map.of("topic", "WEEKLY_CALENDAR"));
      // A new private session after eight calls also proves sessions do not share a call budget.
      var names = s.tools().stream().map(McpSchema.Tool::name).toList();
      int i = 0;
      for (String name : names) {
        if (i++ == 8) break;
        assertFalse(s.call(name, inputs.getOrDefault(name, Map.of())).isError(), name);
      }
      try (var second = open()) {
        for (String name : names.subList(8, names.size()))
          assertFalse(second.call(name, inputs.getOrDefault(name, Map.of())).isError(), name);
      }
      assertEquals(12, dataCalls().size());
      assertTrue(
          dataCalls().stream()
              .allMatch(r -> r.method().equals("GET") && r.path().contains("/assistant-query/")));
    }
  }

  @Test
  void capacityCallBudgetAndCloseReleaseResources() {
    runtime.shutdown();
    runtime = new AssistantMcpRuntime(gateway, verifier, new ToolCatalog(), json, true, 1, 120);
    try (var s = open()) {
      assertThrows(McpFault.class, this::open);
      for (int i = 0; i < 8; i++) assertFalse(s.call("search_courses", Map.of()).isError());
      error(s.call("search_courses", Map.of()));
      assertEquals(8, dataCalls().size());
      s.close();
      error(s.call("search_courses", Map.of()));
    }
    try (var s = open()) {
      assertFalse(s.call("search_courses", Map.of()).isError());
    }
  }

  @Test
  void ttlAndFeatureFlagAreEnforced() throws Exception {
    runtime.shutdown();
    runtime = new AssistantMcpRuntime(gateway, verifier, new ToolCatalog(), json, true, 1, 1);
    try (var s = open()) {
      Thread.sleep(1100);
      error(s.call("search_courses", Map.of()));
      assertEquals(0, dataCalls().size());
      runtime.reap();
    }
    runtime.shutdown();
    runtime = new AssistantMcpRuntime(gateway, verifier, new ToolCatalog(), json, false, 1, 120);
    assertThrows(McpFault.class, this::open);
  }

  @Test
  void oversizedBackendPayloadIsBoundedBeforeParsing() {
    try (var s = open()) {
      oversized = true;
      var r = s.call("search_courses", Map.of());
      error(r);
      assertTrue(r.toString().contains("RESULT_TOO_LARGE"));
    }
  }

  @Test
  void timeoutCoversSlowBodyAfterSuccessfulHeaders() {
    try (var s = open()) {
      slowBody = true;
      assertTimeout(Duration.ofSeconds(8), () -> error(s.call("search_courses", Map.of())));
      assertEquals(1, dataCalls().size());
    }
  }

  @Test
  void parallelUsersKeepTheirOwnCredentialAndBranchWithoutThreadLocals() throws Exception {
    try (var first = open();
        var second = runtime.open(SECOND, SessionVerifier.Mode.ACTIVE_BRANCH, 2L);
        var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var a = workers.submit(() -> first.call("search_courses", Map.of("keyword", "first")));
      var b = workers.submit(() -> second.call("search_courses", Map.of("keyword", "second")));
      assertFalse(a.get().isError());
      assertFalse(b.get().isError());
      assertEquals(2, dataCalls().size());
      for (var request : dataCalls()) {
        if (request.query().get("keyword").equals("first")) {
          assertEquals(BEARER, request.auth());
          assertEquals("1", request.branch());
        } else {
          assertEquals(SECOND, request.auth());
          assertEquals("2", request.branch());
        }
      }
      assertFalse(first.toString().contains(BEARER));
      assertFalse(second.toString().contains(SECOND));
    }
  }

  @Test
  void opaqueInvalidTokenAndUnknownHelpTopicFailBeforeBusinessAccess() {
    assertThrows(
        McpFault.class,
        () ->
            runtime.open(
                "Bearer unrecognized-test-session", SessionVerifier.Mode.ACTIVE_BRANCH, 1L));
    try (var s = open()) {
      error(s.call("get_usage_help", Map.of("topic", "READ_ENV")));
      assertTrue(dataCalls().isEmpty());
    }
  }
}
