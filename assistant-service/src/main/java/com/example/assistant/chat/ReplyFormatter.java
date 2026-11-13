package com.example.assistant.chat;

import com.example.assistant.mcp.SessionVerifier;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ReplyFormatter {
  private final TopicPolicy textPolicy = new TopicPolicy();
  private static final Set<String> KEYS =
      Set.of(
          "id",
          "code",
          "name",
          "type",
          "level",
          "tuitionFee",
          "currency",
          "expectedMinutes",
          "durationVerified",
          "status",
          "branchId",
          "path",
          "courseId",
          "courseName",
          "teacherName",
          "teacherId",
          "currentSize",
          "maxSize",
          "startDate",
          "endDate",
          "studentCode",
          "fullName",
          "currentLevel",
          "enrollmentId",
          "studentId",
          "classId",
          "classCode",
          "className",
          "studyStatus",
          "relationship",
          "metric",
          "value",
          "asOf",
          "sessionDate",
          "startTime",
          "endTime",
          "roomName",
          "kind",
          "substitute",
          "sessionId",
          "state",
          "records",
          "enrollmentIds",
          "studentName",
          "recorded",
          "notRecorded",
          "billingEnrollmentId",
          "amount",
          "paidAmount",
          "balance",
          "credit",
          "dueDate",
          "overdue",
          "receiptNo",
          "tuitionId",
          "voidedAmount",
          "refundedAmount",
          "availableAmount",
          "paidAt",
          "view",
          "examId",
          "maxScore",
          "rosterState",
          "score",
          "examName",
          "examDate",
          "revision",
          "publishedAt",
          "assignedTo",
          "assignedToName",
          "nextContactAt",
          "lastContactedAt",
          "priorityReason",
          "topic",
          "text",
          "content",
          "page",
          "size",
          "totalElements",
          "totalPages");
  private static final Set<String> ROUTES =
      Set.of(
          "/courses",
          "/classes",
          "/my-classes",
          "/schedule",
          "/my-schedule",
          "/students",
          "/tuition",
          "/payments",
          "/leads",
          "/grades",
          "/attendance");
  private static final Set<String> TEACHER_ROUTES =
      Set.of("/courses", "/my-classes", "/my-schedule", "/grades", "/attendance");

  Object clean(Object value, SessionVerifier.Role role, String tool, int depth) {
    if (depth > 8) throw new ChatFault(503, "INVALID_BACKEND_RESPONSE");
    if (value instanceof Map<?, ?> map) {
      var safe = new LinkedHashMap<String, Object>();
      for (var e : map.entrySet()) {
        String key = String.valueOf(e.getKey());
        if (!KEYS.contains(key)
            || key.equals("content") && !(e.getValue() instanceof List<?>)
            || Set.of("text", "topic").contains(key) && !tool.equals("get_usage_help")) continue;
        if (key.equals("path")) {
          String path = path(e.getValue(), role);
          if (path != null) safe.put(key, path);
        } else safe.put(key, clean(e.getValue(), role, tool, depth + 1));
      }
      return safe;
    }
    if (value instanceof List<?> list)
      return list.stream().limit(20).map(v -> clean(v, role, tool, depth + 1)).toList();
    if (value instanceof String text) return textPolicy.displayText(text);
    if (value == null || value instanceof Number || value instanceof Boolean) return value;
    return null;
  }

  private String path(Object value, SessionVerifier.Role role) {
    try {
      if (!(value instanceof String s)
          || s.length() > 600
          || s.contains("\\")
          || s.contains("\r")
          || s.contains("\n")) return null;
      URI uri = URI.create(s);
      if (uri.isAbsolute()
          || uri.getRawAuthority() != null
          || uri.getFragment() != null
          || !Objects.equals(uri.getPath(), uri.getRawPath())
          || !ROUTES.contains(uri.getPath())
          || role == SessionVerifier.Role.TEACHER && !TEACHER_ROUTES.contains(uri.getPath()))
        return null;
      if (uri.getRawQuery() != null) {
        Set<String> allowed =
            switch (uri.getPath()) {
              case "/courses" -> Set.of("keyword", "branchId");
              case "/students" -> Set.of("studentId");
              case "/classes", "/my-classes" -> Set.of("classId");
              case "/schedule", "/my-schedule", "/attendance" -> Set.of("sessionId");
              case "/tuition" -> Set.of("tuitionId", "branchId");
              case "/payments" -> Set.of("paymentId", "branchId");
              case "/grades" -> Set.of("examId", "branchId");
              case "/leads" -> Set.of("leadId");
              default -> Set.of();
            };
        var seen = new HashSet<String>();
        for (String pair : uri.getRawQuery().split("&", -1)) {
          var parts = pair.split("=", -1);
          if (parts.length != 2 || !allowed.contains(parts[0]) || !seen.add(parts[0])) return null;
          String v = java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
          if (parts[0].equals("keyword")) {
            if (v.length() > 120 || textPolicy.injection(v) || v.matches(".*[\\p{Cf}\\p{Cc}<>].*"))
              return null;
          } else if (!v.matches("[1-9][0-9]{0,17}")) return null;
        }
      }
      return s;
    } catch (Exception e) {
      return null;
    }
  }

  public ChatModels.Response withWarnings(ChatModels.Response r, List<String> warnings) {
    if (warnings.isEmpty()) return r;
    var combined = new ArrayList<>(r.warnings());
    combined.addAll(warnings);
    return new ChatModels.Response(
        r.requestId(),
        r.conversationId(),
        r.status(),
        r.message(),
        r.scope(),
        r.observedAt(),
        r.cards(),
        r.choices(),
        r.sources(),
        List.copyOf(combined),
        r.hasMore(),
        r.nextPage());
  }

  @SuppressWarnings("unchecked")
  public ChatModels.Evidence evidence(
      String tool, Map<String, Object> args, Map<String, Object> raw, SessionVerifier.Role role) {
    var p = new LinkedHashMap<String, Object>(raw);
    p.put("data", clean(raw.get("data"), role, tool, 0));
    return new ChatModels.Evidence(tool, Map.copyOf(args), p);
  }

  public ChatModels.Response decision(
      UUID request,
      UUID conversation,
      Map<String, Object> scope,
      String status,
      String reason,
      List<ChatModels.Choice> choices,
      List<ChatModels.Evidence> evidence) {
    String message =
        !choices.isEmpty()
            ? "Có nhiều kết quả phù hợp. Bạn hãy chọn đúng đối tượng trong danh sách; trợ lý chưa"
                + " tự chọn thay bạn."
            : ChatMessages.decision(status, reason);
    return new ChatModels.Response(
        request,
        conversation,
        status,
        message,
        scope,
        OffsetDateTime.now(),
        List.of(),
        choices,
        sources(evidence),
        List.of(),
        false,
        null);
  }

  public ChatModels.Response answer(
      UUID request,
      UUID conversation,
      Map<String, Object> scope,
      List<ChatModels.Evidence> evidence,
      List<String> warnings) {
    if (evidence.isEmpty())
      return decision(
          request, conversation, scope, "UNSUPPORTED_CAPABILITY", "NONE", List.of(), List.of());
    var last = evidence.getLast();
    Object data = last.payload().get("data");
    String status = String.valueOf(last.payload().get("status"));
    var cards = List.of(new ChatModels.Card(last.tool(), title(last.tool()), data));
    String message;
    if (last.tool().equals("get_student_statistics")
        && data instanceof Map<?, ?> stats
        && stats.get("value") instanceof Number n) {
      message =
          switch (String.valueOf(stats.get("metric"))) {
            case "TOTAL_PROFILES" -> "Tổng số hồ sơ học viên";
            case "PROFILE_STUDYING" -> "Số hồ sơ có trạng thái đang học";
            case "ACTIVE_IN_CLASS" -> "Số học viên đang có lớp";
            default -> "Kết quả thống kê";
          };
      message += " trong phạm vi đang xem: " + n + ".";
    } else if (status.equals("NO_RESULTS"))
      message = "Không tìm thấy dữ liệu phù hợp trong phạm vi đang xem.";
    else if (data instanceof Map<?, ?> map
        && Set.of("NOT_STARTED", "CANCELLED").contains(String.valueOf(map.get("state"))))
      message =
          "NOT_STARTED".equals(map.get("state"))
              ? "Buổi học chưa diễn ra; chưa có kết quả điểm danh."
              : "Buổi học đã hủy; không có kết quả điểm danh để tra cứu.";
    else {
      var rows = ReferenceIndex.rows(data);
      Object total = page(data).get("totalElements");
      message =
          total instanceof Number
              ? "Đang hiển thị "
                  + rows.size()
                  + " trên "
                  + total
                  + " kết quả trong phạm vi đang xem."
              : "Đã lấy được kết quả tra cứu trong phạm vi đang xem.";
    }
    boolean more = Boolean.TRUE.equals(last.payload().get("hasMore"));
    Integer next = last.payload().get("nextPage") instanceof Number n ? n.intValue() : null;
    return new ChatModels.Response(
        request,
        conversation,
        status,
        message,
        scope,
        OffsetDateTime.now(),
        cards,
        List.of(),
        sources(evidence),
        List.copyOf(warnings),
        more,
        next);
  }

  private List<ChatModels.Source> sources(List<ChatModels.Evidence> evidence) {
    return evidence.stream()
        .map(
            e ->
                new ChatModels.Source(
                    e.tool(),
                    String.valueOf(e.payload().get("observedAt")),
                    scope(e.payload().get("scope"))))
        .toList();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> scope(Object value) {
    return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
  }

  private Map<?, ?> page(Object value) {
    if (value instanceof Map<?, ?> map) {
      if (map.get("records") instanceof Map<?, ?> records) return records;
      return map;
    }
    return Map.of();
  }

  private String title(String tool) {
    return switch (tool) {
      case "search_courses" -> "Khóa học";
      case "search_classes", "get_student_classes" -> "Lớp học";
      case "find_students" -> "Học viên";
      case "get_student_statistics" -> "Thống kê học viên";
      case "list_sessions" -> "Lịch học và lịch dạy";
      case "get_session_attendance" -> "Điểm danh";
      case "get_learning_results" -> "Kết quả học tập";
      case "search_tuitions" -> "Học phí và công nợ";
      case "search_receipts" -> "Phiếu thu";
      case "list_priority_leads" -> "Khách hàng ưu tiên chăm sóc";
      case "get_usage_help" -> "Hướng dẫn sử dụng";
      default -> "Kết quả tra cứu";
    };
  }
}
