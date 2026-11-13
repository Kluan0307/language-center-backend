package com.example.assistant.mcp;

import static com.example.assistant.mcp.SessionVerifier.Role.*;

import io.modelcontextprotocol.spec.McpSchema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ToolCatalog {
  record Arg(
      String type,
      boolean required,
      Object defaultValue,
      List<String> values,
      Long min,
      Long max,
      String format) {
    Map<String, Object> schema() {
      var s = new LinkedHashMap<String, Object>();
      s.put("type", type);
      if (defaultValue != null) s.put("default", defaultValue);
      if (!values.isEmpty()) s.put("enum", values);
      if (min != null) s.put("minimum", min);
      if (max != null) s.put("maximum", max);
      if (type.equals("string")) s.put("maxLength", format.equals("date") ? 10 : 120);
      if (!format.isEmpty()) s.put("format", format);
      return Map.copyOf(s);
    }

    Object validate(Object value) {
      try {
        return switch (type) {
          case "string" -> {
            if (!(value instanceof String text)
                || text.length() > 120
                || !values.isEmpty() && !values.contains(text))
              throw new McpFault("INVALID_ARGUMENTS");
            if (format.equals("date")) {
              if (text.length() != 10) throw new McpFault("INVALID_ARGUMENTS");
              LocalDate.parse(text);
            }
            yield text;
          }
          case "boolean" -> {
            if (!(value instanceof Boolean)) throw new McpFault("INVALID_ARGUMENTS");
            yield value;
          }
          case "integer" -> {
            if (!(value instanceof Number)) throw new McpFault("INVALID_ARGUMENTS");
            long n = new BigDecimal(value.toString()).longValueExact();
            if (min != null && n < min || max != null && n > max)
              throw new McpFault("INVALID_ARGUMENTS");
            yield n;
          }
          case "number" -> {
            if (!(value instanceof Number)) throw new McpFault("INVALID_ARGUMENTS");
            var n = new BigDecimal(value.toString());
            if (n.signum() < 0 || n.scale() > 2 || (long) n.precision() - n.scale() > 13)
              throw new McpFault("INVALID_ARGUMENTS");
            yield n;
          }
          default -> throw new McpFault("INVALID_ARGUMENTS");
        };
      } catch (McpFault e) {
        throw e;
      } catch (Exception e) {
        throw new McpFault("INVALID_ARGUMENTS");
      }
    }
  }

  record Definition(
      String name,
      String description,
      BackendGateway.Target target,
      String endpoint,
      Set<SessionVerifier.Role> roles,
      Map<String, Arg> args) {
    McpSchema.Tool tool() {
      var props = new LinkedHashMap<String, Object>();
      var required = new ArrayList<String>();
      args.forEach(
          (k, v) -> {
            props.put(k, v.schema());
            if (v.required()) required.add(k);
          });
      return McpSchema.Tool.builder()
          .name(name)
          .description(description)
          .inputSchema(
              Map.of(
                  "type",
                  "object",
                  "properties",
                  props,
                  "required",
                  required,
                  "additionalProperties",
                  false))
          .annotations(new McpSchema.ToolAnnotations(name, true, false, true, false, false))
          .build();
    }

    Map<String, Object> validate(Map<String, Object> input) {
      if (input == null) input = Map.of();
      if (!args.keySet().containsAll(input.keySet())) throw new McpFault("INVALID_ARGUMENTS");
      var p = new LinkedHashMap<String, Object>();
      for (var e : args.entrySet()) {
        var a = e.getValue();
        if (input.containsKey(e.getKey())) p.put(e.getKey(), a.validate(input.get(e.getKey())));
        else if (a.defaultValue() != null) p.put(e.getKey(), a.defaultValue());
        else if (a.required()) throw new McpFault("INVALID_ARGUMENTS");
      }
      if (p.get("minTuition") instanceof java.math.BigDecimal min
          && p.get("maxTuition") instanceof java.math.BigDecimal max
          && min.compareTo(max) > 0) throw new McpFault("INVALID_ARGUMENTS");
      if (p.get("from") instanceof String from && p.get("to") instanceof String to) {
        long days =
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(from), LocalDate.parse(to));
        if (days < 0 || days > 30) throw new McpFault("INVALID_ARGUMENTS");
      }
      return p;
    }
  }

  private static final Set<SessionVerifier.Role> ALL = Set.of(SUPER_ADMIN, ADMIN, STAFF, TEACHER),
      OPS = Set.of(SUPER_ADMIN, ADMIN, STAFF);

  private Arg text() {
    return new Arg("string", false, null, List.of(), null, null, "");
  }

  private Arg id(boolean required) {
    return new Arg("integer", required, null, List.of(), 1L, Long.MAX_VALUE, "");
  }

  private Arg bool(Object value) {
    return new Arg("boolean", false, value, List.of(), null, null, "");
  }

  private Arg values(Object value, String... enums) {
    return new Arg("string", false, value, List.of(enums), null, null, "");
  }

  private Arg date(boolean required) {
    return new Arg("string", required, null, List.of(), null, null, "date");
  }

  private Map<String, Arg> fields(Object... pairs) {
    var p = new LinkedHashMap<String, Arg>();
    for (int i = 0; i < pairs.length; i += 2) p.put((String) pairs[i], (Arg) pairs[i + 1]);
    return p;
  }

  private Map<String, Arg> paged(Map<String, Arg> p) {
    p.put("page", new Arg("integer", false, 0, List.of(), 0L, 1000000L, ""));
    p.put("size", new Arg("integer", false, 5, List.of(), 1L, 20L, ""));
    return p;
  }

  private Definition academic(
      String name,
      String description,
      String endpoint,
      Set<SessionVerifier.Role> roles,
      Map<String, Arg> args) {
    return new Definition(
        name, description, BackendGateway.Target.ACADEMIC, endpoint, roles, Map.copyOf(args));
  }

  List<Definition> forRole(SessionVerifier.Role role) {
    var definitions = new ArrayList<Definition>();
    var money = new Arg("number", false, null, List.of(), 0L, null, "");
    definitions.add(
        academic(
            "search_courses",
            "Tra cứu khóa trong phạm vi. Giá VND; dưới dùng maxInclusive=false. Không suy diễn"
                + " duration legacy.",
            "courses",
            ALL,
            paged(
                fields(
                    "keyword",
                    text(),
                    "level",
                    text(),
                    "type",
                    text(),
                    "status",
                    values("OPEN", "OPEN", "CLOSED"),
                    "minTuition",
                    money,
                    "maxTuition",
                    money,
                    "maxInclusive",
                    bool(true)))));
    definitions.add(
        academic(
            "search_classes",
            "Tra cứu lớp có quyền. recruitingOnly chỉ là còn chỗ theo dữ liệu, không bảo đảm đủ"
                + " điều kiện ghi danh.",
            "classes",
            ALL,
            paged(
                fields(
                    "keyword",
                    text(),
                    "courseId",
                    id(false),
                    "status",
                    values(null, "UPCOMING", "ONGOING", "FULL", "FINISHED"),
                    "recruitingOnly",
                    bool(false)))));
    definitions.add(
        academic(
            "find_students",
            "Tìm theo tên/mã, trả lựa chọn tối thiểu. Trùng tên phải hỏi lại; giáo viên chỉ roster"
                + " lớp chính mình.",
            "students",
            ALL,
            paged(fields("keyword", text(), "classId", id(false)))));
    definitions.add(
        academic(
            "get_student_classes",
            "Các quan hệ lớp của học viên đã xác định; phân biệt"
                + " ACTIVE/NOT_STARTED/RESERVED/TRANSFERRED. Chỉ hỗ trợ hôm nay.",
            "student-classes",
            ALL,
            paged(fields("studentId", id(true), "asOf", date(false)))));
    definitions.add(
        academic(
            "get_student_statistics",
            "Đếm hồ sơ hoặc học viên distinct đang có lớp. Không dùng trạng thái hồ sơ thay cho"
                + " đăng ký hiệu lực, chỉ hôm nay.",
            "student-statistics",
            OPS,
            fields(
                "metric",
                values("TOTAL_PROFILES", "TOTAL_PROFILES", "PROFILE_STUDYING", "ACTIVE_IN_CLASS"),
                "asOf",
                date(false))));
    definitions.add(
        academic(
            "list_sessions",
            "Buổi thực tế, tối đa 31 ngày. Có nhãn hủy/học bù/dạy thay, giáo viên chỉ actual"
                + " teacher.",
            "sessions",
            ALL,
            paged(
                fields(
                    "from",
                    date(true),
                    "to",
                    date(true),
                    "classId",
                    id(false),
                    "teacherId",
                    id(false),
                    "status",
                    values(null, "SCHEDULED", "CANCELLED", "COMPLETED")))));
    definitions.add(
        academic(
            "get_session_attendance",
            "Điểm danh buổi có quyền. NOT_RECORDED không phải PRESENT; buổi tương lai/hủy trả trạng"
                + " thái.",
            "attendance",
            ALL,
            paged(fields("sessionId", id(true)))));
    var views =
        role == TEACHER
            ? new String[] {"CURRENT"}
            : role == STAFF ? new String[] {"PUBLISHED"} : new String[] {"CURRENT", "PUBLISHED"};
    definitions.add(
        new Definition(
            "get_learning_results",
            "Điểm CURRENT hoặc PUBLISHED theo quyền. Điểm 0 khác chưa chấm. Không suy quyền cả lớp"
                + " từ buổi dạy thay.",
            BackendGateway.Target.TEACHING,
            "",
            ALL,
            Map.copyOf(
                paged(
                    fields(
                        "view",
                        values(role == TEACHER ? "CURRENT" : "PUBLISHED", views),
                        "examId",
                        id(false),
                        "studentId",
                        id(false),
                        "enrollmentId",
                        id(false))))));
    definitions.add(
        new Definition(
            "search_tuitions",
            "Học phí và công nợ theo billing root, không cộng một trang thành tổng. Không ghi nhận"
                + " thanh toán.",
            BackendGateway.Target.FINANCE,
            "tuitions",
            OPS,
            Map.copyOf(
                paged(
                    fields(
                        "studentId",
                        id(false),
                        "classId",
                        id(false),
                        "overdue",
                        bool(null),
                        "status",
                        values(null, "PENDING", "UNPAID", "PARTIAL", "PAID", "CANCELLED"))))));
    definitions.add(
        new Definition(
            "search_receipts",
            "Phiếu thu với hủy/hoàn/khả dụng. Đơn PENDING không phải phiếu thu; không trả thành"
                + " doanh thu.",
            BackendGateway.Target.FINANCE,
            "receipts",
            OPS,
            Map.copyOf(
                paged(
                    fields(
                        "studentId", id(false), "tuitionId", id(false), "classId", id(false))))));
    definitions.add(
        academic(
            "list_priority_leads",
            "Ưu tiên chăm sóc theo lịch hẹn, không dự đoán khả năng mua. STAFF chỉ lead được giao.",
            "priority-leads",
            OPS,
            fields("limit", new Arg("integer", false, 5, List.of(), 1L, 20L, ""))));
    definitions.add(
        academic(
            "get_usage_help",
            "Hướng dẫn đã duyệt về lịch tuần hoặc tra cứu khóa học; không đọc file hoặc web ngoài.",
            "usage-help",
            ALL,
            fields(
                "topic",
                new Arg(
                    "string",
                    true,
                    null,
                    List.of("WEEKLY_CALENDAR", "FIND_COURSES"),
                    null,
                    null,
                    ""))));
    return definitions.stream().filter(d -> d.roles().contains(role)).toList();
  }
}
