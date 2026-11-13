package com.example.assistant.chat;

import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import tools.jackson.databind.json.JsonMapper;

public final class ChatFixture implements AutoCloseable {
  public static final String TOKEN = "Bearer synthetic-chat-session",
      SECOND = "Bearer synthetic-other-session",
      ROTATED = "Bearer synthetic-rotated-session";
  private final HttpServer http;
  private final JsonMapper json = JsonMapper.builder().build();
  public volatile String role = "ADMIN";
  public volatile List<Long> branches = List.of(1L, 2L);
  public volatile boolean duplicate = true, active = true;
  public volatile boolean multipleClasses;
  public volatile int failure = 200;
  public volatile String courseName = "Khóa giao tiếp mẫu";
  public volatile String studentName = "Nguyễn Văn An";
  public volatile Runnable afterRead = () -> {};
  public final List<Map<String, String>> calls = new CopyOnWriteArrayList<>();

  public ChatFixture() {
    try {
      http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      http.createContext("/", this::serve);
      http.start();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String origin() {
    return "http://127.0.0.1:" + http.getAddress().getPort();
  }

  private void serve(HttpExchange x) throws java.io.IOException {
    String token = x.getRequestHeaders().getFirst("Authorization"),
        path = x.getRequestURI().getPath();
    boolean second = SECOND.equals(token), auth = path.equals("/api/v1/auth/me");
    var p = new LinkedHashMap<String, String>();
    String q = x.getRequestURI().getRawQuery();
    if (q != null)
      for (String pair : q.split("&")) {
        var kv = pair.split("=", 2);
        p.put(
            URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
            URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
      }
    if (!auth) {
      p.put("_path", path);
      p.put("_method", x.getRequestMethod());
      calls.add(p);
    }
    int status =
        (!TOKEN.equals(token) && !ROTATED.equals(token) && !second) ? 401 : auth ? 200 : failure;
    Object body;
    if (status != 200) body = Map.of("message", "PRIVATE_PROVIDER_ERROR");
    else if (auth)
      body =
          Map.of(
              "accountId",
              second ? 200L : 100L,
              "profileId",
              second ? 12L : 11L,
              "username",
              second ? "second" : "operator",
              "role",
              second ? "STAFF" : role,
              "status",
              active,
              "mustChangePassword",
              false,
              "branchId",
              second ? 2L : 1L,
              "branchIds",
              second ? List.of(2L) : branches);
    else {
      String endpoint = path.substring(path.lastIndexOf('/') + 1);
      String source;
      Object data;
      boolean more = false;
      Integer next = null;
      switch (endpoint) {
        case "courses" -> {
          source = "search_courses";
          data =
              page(
                  List.of(
                      row(
                          "id",
                          1,
                          "code",
                          "GT1",
                          "name",
                          courseName,
                          "tuitionFee",
                          4500000,
                          "status",
                          "OPEN",
                          "path",
                          "/courses?keyword=GT1",
                          "content",
                          "IGNORED_LONG_DESCRIPTION",
                          "phone",
                          "PRIVATE_PHONE")),
                  Integer.parseInt(p.getOrDefault("page", "0")),
                  50);
          more = true;
          next = Integer.parseInt(p.getOrDefault("page", "0")) + 1;
          if (p.containsKey("maxTuition")) {
            int compare =
                new java.math.BigDecimal("4500000")
                    .compareTo(new java.math.BigDecimal(p.get("maxTuition")));
            if (compare > 0 || compare == 0 && "false".equals(p.get("maxInclusive"))) {
              data = page(List.of(), 0, 0);
              more = false;
              next = null;
            }
          }
        }
        case "students" -> {
          source = "find_students";
          String keyword = p.getOrDefault("keyword", "");
          var one =
              row(
                  "id",
                  11,
                  "studentCode",
                  "HV11",
                  "fullName",
                  studentName,
                  "branchId",
                  1,
                  "path",
                  "/students?studentId=11",
                  "phone",
                  "PRIVATE_PHONE");
          var two =
              row(
                  "id",
                  12,
                  "studentCode",
                  "HV12",
                  "fullName",
                  "Nguyễn Văn An",
                  "branchId",
                  1,
                  "path",
                  "/students?studentId=12");
          var rows =
              keyword.equals("HV12")
                  ? List.of(two)
                  : keyword.equals("HV11") || !duplicate ? List.of(one) : List.of(one, two);
          data = page(rows, 0, rows.size());
        }
        case "student-classes" -> {
          source = "get_student_classes";
          data =
              page(
                  List.of(
                      row(
                          "enrollmentId",
                          301,
                          "studentId",
                          Long.parseLong(p.get("studentId")),
                          "classId",
                          9,
                          "classCode",
                          "L1",
                          "className",
                          "Giao tiếp 1",
                          "relationship",
                          "ACTIVE",
                          "path",
                          "/classes?classId=9")),
                  0,
                  1);
        }
        case "student-statistics" -> {
          source = "get_student_statistics";
          data =
              Map.of(
                  "metric",
                  p.getOrDefault("metric", "TOTAL_PROFILES"),
                  "value",
                  0,
                  "asOf",
                  "2026-11-13");
        }
        case "classes" -> {
          source = "search_classes";
          data =
              page(List.of(row("id", 9, "code", "L1", "name", "Giao tiếp 1", "branchId", 1)), 0, 1);
        }
        case "sessions" -> {
          source = "list_sessions";
          data =
              page(
                  List.of(
                      row(
                          "id",
                          101,
                          "classId",
                          9,
                          "className",
                          "Giao tiếp 1",
                          "branchId",
                          1,
                          "sessionDate",
                          "2026-11-13",
                          "startTime",
                          "18:00",
                          "status",
                          "SCHEDULED")),
                  0,
                  1);
        }
        case "attendance" -> {
          source = "get_session_attendance";
          data =
              Map.of(
                  "sessionId",
                  101,
                  "state",
                  "AVAILABLE",
                  "records",
                  page(
                      List.of(
                          row(
                              "studentId",
                              11,
                              "studentName",
                              "Nguyễn Văn An",
                              "recorded",
                              false,
                              "status",
                              "NOT_RECORDED")),
                      0,
                      1),
                  "notRecorded",
                  1);
        }
        case "tuitions" -> {
          source = "search_tuitions";
          data =
              page(
                  List.of(
                      row(
                          "id",
                          41,
                          "billingEnrollmentId",
                          301,
                          "studentId",
                          11,
                          "amount",
                          5000000,
                          "paidAmount",
                          2000000,
                          "balance",
                          3000000,
                          "status",
                          "PARTIAL",
                          "path",
                          "/tuition?tuitionId=41")),
                  0,
                  1);
        }
        case "receipts" -> {
          source = "search_receipts";
          data =
              page(
                  List.of(
                      row(
                          "id",
                          51,
                          "receiptNo",
                          "PT51",
                          "tuitionId",
                          41,
                          "amount",
                          2000000,
                          "refundedAmount",
                          0,
                          "availableAmount",
                          2000000,
                          "status",
                          "PAID")),
                  0,
                  1);
        }
        case "current-grades", "published-grades" -> {
          source = "get_learning_results";
          data =
              Map.of(
                  "view",
                  endpoint.equals("current-grades") ? "CURRENT" : "PUBLISHED",
                  "records",
                  page(List.of(row("examId", 21, "score", 0, "status", "GRADED")), 0, 1));
        }
        case "priority-leads" -> {
          source = "list_priority_leads";
          data =
              page(
                  List.of(row("id", 61, "fullName", "Khách mẫu", "priorityReason", "OVERDUE")),
                  0,
                  1);
        }
        case "usage-help" -> {
          source = "get_usage_help";
          data =
              Map.of(
                  "topic", "WEEKLY_CALENDAR", "text", "Mở lịch và chọn Tuần.", "path", "/schedule");
        }
        default -> {
          source = "unknown";
          data = Map.of();
        }
      }
      if (endpoint.equals("student-classes") && multipleClasses)
        data =
            page(
                List.of(
                    row(
                        "classId",
                        9,
                        "classCode",
                        "L1",
                        "className",
                        "Lớp 1",
                        "studentId",
                        11,
                        "branchId",
                        1),
                    row(
                        "classId",
                        10,
                        "classCode",
                        "L2",
                        "className",
                        "Lớp 2",
                        "studentId",
                        11,
                        "branchId",
                        1)),
                0,
                2);
      String mode = p.getOrDefault("scopeMode", "ACTIVE_BRANCH");
      Object ids =
          mode.equals("WHOLE_SYSTEM")
              ? List.of()
              : mode.equals("ALL_ASSIGNED")
                  ? branches
                  : List.of(Long.parseLong(p.getOrDefault("branchId", "1")));
      var result =
          row(
              "requestId",
              UUID.randomUUID().toString(),
              "status",
              data instanceof Map<?, ?> pageData
                      && pageData.get("content") instanceof List<?> rows
                      && rows.isEmpty()
                  ? "NO_RESULTS"
                  : "ANSWER",
              "scope",
              Map.of("mode", mode, "branchIds", ids, "label", "Chi nhánh mẫu"),
              "observedAt",
              OffsetDateTime.now().toString(),
              "source",
              source,
              "data",
              data,
              "warnings",
              List.of(),
              "hasMore",
              more,
              "nextPage",
              next);
      body = result;
    }
    if (!auth) afterRead.run();
    byte[] bytes = json.writeValueAsBytes(body);
    x.getResponseHeaders().set("Content-Type", "application/json");
    x.sendResponseHeaders(status, bytes.length);
    try (var out = x.getResponseBody()) {
      out.write(bytes);
    }
  }

  private Map<String, Object> page(List<?> rows, int page, long total) {
    return Map.of(
        "content",
        rows,
        "page",
        page,
        "size",
        5,
        "totalElements",
        total,
        "totalPages",
        (total + 4) / 5);
  }

  private Map<String, Object> row(Object... args) {
    var map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < args.length; i += 2) map.put((String) args[i], args[i + 1]);
    return map;
  }

  public void reset() {
    role = "ADMIN";
    branches = List.of(1L, 2L);
    duplicate = true;
    multipleClasses = false;
    active = true;
    failure = 200;
    courseName = "Khóa giao tiếp mẫu";
    studentName = "Nguyễn Văn An";
    afterRead = () -> {};
    calls.clear();
  }

  @Override
  public void close() {
    http.stop(0);
  }
}
