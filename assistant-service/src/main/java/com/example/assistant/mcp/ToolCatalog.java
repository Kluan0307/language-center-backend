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
        long maximum =
            Set.of("get_attendance_summary", "list_missing_attendance").contains(name)
                ? 89
                : Set.of("search_classes", "search_receipts", "get_cash_flow", "compare_cash_flow")
                        .contains(name)
                    ? 365
                    : 30;
        if (days < 0 || days > maximum) throw new McpFault("INVALID_ARGUMENTS");
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
                + " duration legacy. Phân loại dùng programCode và audienceCode, không dùng tên khóa hoặc level để đoán đối tượng. Tổng khóa lấy totalElements, mỗi khóa tính một lần.",
            "courses",
            ALL,
            paged(
                fields(
                    "keyword",
                    text(),
                    "level",
                    text(),
                    "programCode",
                    values(null, "GE", "COMMUNICATION", "IELTS", "TOEIC"),
                    "audienceCode",
                    values(null, "PRESCHOOL", "PRIMARY", "SECONDARY", "HIGH_SCHOOL", "ADULT"),
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
                    "classId",
                    id(false),
                    "from",
                    date(false),
                    "to",
                    date(false),
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
            paged(
                fields(
                    "keyword",
                    text(),
                    "classId",
                    id(false),
                    "studentId",
                    id(false),
                    "relationship",
                    values(null, "STUDYING", "RESERVED", "TRANSFERRED", "DROPPED", "COMPLETED"),
                    "status",
                    values(null, "STUDYING", "PAUSED", "GRADUATED"),
                    "currentLevel",
                    text()))));
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
                    values(null, "SCHEDULED", "CANCELLED", "COMPLETED"),
                    "kind",
                    values(null, "REGULAR", "MAKEUP", "LEGACY"),
                    "substituteOnly",
                    bool(false),
                    "upcomingOnly",
                    bool(false)))));
    definitions.add(
        academic(
            "get_session_attendance",
            "Điểm danh buổi có quyền. NOT_RECORDED không phải PRESENT; buổi tương lai/hủy trả trạng"
                + " thái.",
            "attendance",
            ALL,
            paged(
                fields(
                    "sessionId", id(true), "studentId", id(false), "missingOnly", bool(false)))));
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
                        id(false),
                        "scoreFilter",
                        values("ALL", "ALL", "ZERO", "NOT_GRADED"))))));
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
                        values(
                            null, "PENDING", "UNPAID", "PARTIAL", "PAID", "CANCELLED", "OVERDUE"),
                        "tuitionId",
                        id(false),
                        "dueFrom",
                        date(false),
                        "dueTo",
                        date(false),
                        "outstandingOnly",
                        bool(false))))));
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
                        "studentId",
                        id(false),
                        "tuitionId",
                        id(false),
                        "classId",
                        id(false),
                        "receiptId",
                        id(false),
                        "receiptNo",
                        text(),
                        "receiptState",
                        values("ALL", "ALL", "VOIDED", "REFUNDED", "AVAILABLE", "LEGACY"),
                        "from",
                        date(false),
                        "to",
                        date(false))))));
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
                    List.of("WEEKLY_CALENDAR", "FIND_COURSES", "PAYMENT_ACCOUNTING"),
                    null,
                    null,
                    ""))));
    var kinds = role == STAFF ? new String[] {"TEACHER"} : new String[] {"TEACHER", "STAFF"};
    definitions.add(
        new Definition(
            "find_personnel",
            "Tìm giáo viên/nhân viên trong phân công. Trùng tên phải chọn. STAFF chỉ teacher đang"
                + " hoạt động; không trả bằng cấp hoặc liên hệ.",
            BackendGateway.Target.AUTH,
            "personnel",
            OPS,
            Map.copyOf(
                paged(
                    fields(
                        "kind",
                        values("TEACHER", kinds),
                        "keyword",
                        text(),
                        "personnelId",
                        id(false),
                        "activeOnly",
                        bool(role == STAFF))))));
    definitions.add(
        new Definition(
            "get_personnel_statistics",
            "Đếm distinct profile trong phân công; activeOnly=true chỉ tài khoản mở và workStatus"
                + " ACTIVE. STAFF chỉ teacher đang hoạt động.",
            BackendGateway.Target.AUTH,
            "personnel-statistics",
            OPS,
            Map.copyOf(
                fields("kind", values("TEACHER", kinds), "activeOnly", bool(role == STAFF)))));
    definitions.add(
        new Definition(
            "get_personnel_profile",
            "Bằng cấp, chứng chỉ, chuyên môn hoặc vị trí của nhân sự đã xác định. Chỉ quản trị;"
                + " không có email/điện thoại/secrets.",
            BackendGateway.Target.AUTH,
            "personnel-profile",
            Set.of(SUPER_ADMIN, ADMIN),
            Map.copyOf(
                fields("kind", values("TEACHER", "TEACHER", "STAFF"), "personnelId", id(true)))));
    definitions.add(
        new Definition(
            "get_my_personnel_profile",
            "Hồ sơ giáo viên chính mình, gồm bằng cấp/chứng chỉ, không nhận ID người khác.",
            BackendGateway.Target.AUTH,
            "my-personnel-profile",
            Set.of(TEACHER),
            Map.of()));
    definitions.add(
        academic(
            "get_teacher_classes",
            "Lớp/khóa được giao cho teacherId đã xác định; currentOnly=true lớp đang trong thời"
                + " gian học. Đây là phụ trách lớp, không suy từ chuyên môn hồ sơ.",
            "teacher-classes",
            ALL,
            paged(fields("teacherId", id(true), "currentOnly", bool(true)))));
    definitions.add(
        academic(
            "search_course_admissions",
            "Khóa OPEN theo điều kiện đầu vào đã xác nhận, lấy revision mới nhất. entryLevel là mã"
                + " trình độ được nhận, không phải trình độ đầu ra.",
            "course-admissions",
            ALL,
            paged(fields("keyword", text(), "entryLevel", text(), "programCode", values(null, "GE", "COMMUNICATION", "IELTS", "TOEIC"), "audienceCode", values(null, "PRESCHOOL", "PRIMARY", "SECONDARY", "HIGH_SCHOOL", "ADULT")))));
    definitions.add(
        academic(
            "search_rooms",
            "Phòng theo trạng thái hoặc không trùng buổi đã lưu trong ngày/khoảng giờ xác định."
                + " Không bảo đảm khả dụng với lịch chưa sinh.",
            "rooms",
            OPS,
            paged(
                fields(
                    "keyword",
                    text(),
                    "status",
                    values(null, "AVAILABLE", "MAINTENANCE"),
                    "minCapacity",
                    new Arg("integer", false, 1, List.of(), 1L, 10000L, ""),
                    "from",
                    date(false),
                    "startTime",
                    text(),
                    "endTime",
                    text()))));
    definitions.add(
        academic(
            "find_available_teachers",
            "Giáo viên ACTIVE có phân công trong scope và không trùng buổi đã lưu ở bất kỳ chi"
                + " nhánh nào. Cần ngày và giờ; không xét nghỉ phép/di chuyển.",
            "available-teachers",
            OPS,
            paged(
                fields(
                    "keyword",
                    text(),
                    "from",
                    date(true),
                    "startTime",
                    new Arg("string", true, null, List.of(), null, null, ""),
                    "endTime",
                    new Arg("string", true, null, List.of(), null, null, "")))));
    definitions.add(
        academic(
            "get_academic_statistics",
            "Nhóm theo chi nhánh: lớp đang diễn ra/đã kết thúc hoặc học viên distinct đang có lớp."
                + " Không phải ảnh chụp quá khứ.",
            "academic-statistics",
            Set.of(SUPER_ADMIN, ADMIN),
            paged(
                fields(
                    "metric",
                    values(
                        "CLASSES_ONGOING",
                        "CLASSES_ONGOING",
                        "CLASSES_FINISHED",
                        "ACTIVE_STUDENTS")))));
    definitions.add(
        academic(
            "get_student_profile",
            "Hồ sơ học viên tối thiểu đã xác định; giáo viên chỉ học viên đang có quan hệ hiệu lực"
                + " với lớp mình, không có liên hệ riêng.",
            "student-profile",
            ALL,
            fields("studentId", id(true))));
    definitions.add(
        academic(
            "get_roster_statistics",
            "Đếm distinct học viên hiệu lực hôm nay trong classId hoặc các lớp được quyền; giáo"
                + " viên chỉ lớp chính mình.",
            "roster-statistics",
            ALL,
            fields("classId", id(false))));
    definitions.add(
        academic(
            "get_attendance_summary",
            "Học viên vắng ABSENT ít nhất minAbsences buổi đã ghi nhận trong khoảng tối đa 90 ngày."
                + " Loại buổi hủy/tương lai/ngoài kỳ hiệu lực; không coi chưa ghi là vắng.",
            "attendance-summary",
            ALL,
            paged(
                fields(
                    "from",
                    date(true),
                    "to",
                    date(true),
                    "classId",
                    id(false),
                    "studentId",
                    id(false),
                    "minAbsences",
                    new Arg("integer", false, 3, List.of(), 1L, 90L, "")))));
    definitions.add(
        academic(
            "list_missing_attendance",
            "Buổi đã kết thúc nhưng còn roster hiệu lực chưa ghi điểm danh, tối đa 90 ngày; teacher"
                + " được giao lớp hoặc buổi.",
            "missing-attendance",
            ALL,
            paged(fields("from", date(true), "to", date(true), "classId", id(false)))));
    definitions.add(
        new Definition(
            "list_learning_exams",
            "Bài kiểm tra và số người thiếu điểm dựa roster đã chốt. missingOnly chỉ bài đến ngày"
                + " thi, không gọi là quá hạn vì chưa có deadline chấm. Teacher chỉ lớp chính"
                + " mình.",
            BackendGateway.Target.TEACHING,
            "exams",
            Set.of(SUPER_ADMIN, ADMIN, TEACHER),
            Map.copyOf(
                paged(
                    fields(
                        "classId",
                        id(false),
                        "examId",
                        id(false),
                        "keyword",
                        text(),
                        "missingOnly",
                        bool(false))))));
    var groups = role == STAFF ? new String[] {"NONE"} : new String[] {"NONE", "BRANCH"};
    definitions.add(
        new Definition(
            "get_debt_statistics",
            "Tổng công nợ HIỆN TẠI theo billing root, bỏ CANCELLED, không cộng page; chưa có"
                + " snapshot nợ quá khứ. Đơn PENDING không phải tiền đã thu.",
            BackendGateway.Target.FINANCE,
            "debt-statistics",
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
                        "dueFrom",
                        date(false),
                        "dueTo",
                        date(false),
                        "groupBy",
                        values("NONE", groups))))));
    definitions.add(
        new Definition(
            "get_cash_flow",
            "Dòng tiền VND trong from/to theo ngày thu và ngày hủy/hoàn."
                + " netCollected=gross-void-refund, có thể âm; không phải doanh thu dồn tích.",
            BackendGateway.Target.FINANCE,
            "cash-flow",
            Set.of(SUPER_ADMIN, ADMIN),
            Map.copyOf(
                paged(
                    fields(
                        "from",
                        date(true),
                        "to",
                        date(true),
                        "groupBy",
                        values("NONE", "NONE", "BRANCH"),
                        "studentId",
                        id(false),
                        "classId",
                        id(false))))));
    definitions.add(
        new Definition(
            "compare_cash_flow",
            "So sánh hai kỳ bằng độ dài, không chồng lấn. Difference=net kỳ so sánh trừ kỳ đầu; cần"
                + " đủ bốn ngày.",
            BackendGateway.Target.FINANCE,
            "cash-comparison",
            Set.of(SUPER_ADMIN, ADMIN),
            Map.copyOf(
                fields(
                    "from",
                    date(true),
                    "to",
                    date(true),
                    "compareFrom",
                    date(true),
                    "compareTo",
                    date(true)))));
    definitions.add(
        academic(
            "search_leads",
            "Tìm khách theo tên, lịch hẹn, trạng thái, assignee trong scope. STAFF luôn chính mình;"
                + " assigneeId là ACCOUNT ID, không phải staff profile ID.",
            "leads",
            OPS,
            paged(
                fields(
                    "keyword",
                    text(),
                    "leadId",
                    id(false),
                    "assigneeId",
                    id(false),
                    "followUp",
                    values(null, "OVERDUE", "TODAY", "UPCOMING", "UNSCHEDULED"),
                    "openOnly",
                    bool(true),
                    "status",
                    values(null, "NEW", "CONTACTED", "CONSULTING", "CONVERTED", "LOST")))));
    definitions.add(
        academic(
            "get_care_statistics",
            "Đếm khách khớp điều kiện trước limit; nhóm branch/assignee chỉ quản trị; STAFF chính"
                + " mình.",
            "care-statistics",
            OPS,
            paged(
                fields(
                    "assigneeId",
                    id(false),
                    "followUp",
                    values(null, "OVERDUE", "TODAY", "UPCOMING", "UNSCHEDULED"),
                    "openOnly",
                    bool(true),
                    "groupBy",
                    values(
                        "NONE",
                        role == STAFF
                            ? new String[] {"NONE"}
                            : new String[] {"NONE", "BRANCH", "ASSIGNEE"})))));
    definitions.add(
        academic(
            "search_support_requests",
            "Tra trạng thái yêu cầu hỗ trợ trong scope, không trả nội dung trao đổi/ghi chú riêng."
                + " Xem supportId cụ thể dùng openOnly=false để không ẩn bản đã đóng.",
            "support-requests",
            OPS,
            paged(
                fields(
                    "supportId",
                    id(false),
                    "studentId",
                    id(false),
                    "openOnly",
                    bool(true),
                    "status",
                    values(null, "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")))));
    definitions.add(
        academic(
            "get_support_statistics",
            "Đếm yêu cầu OPEN/IN_PROGRESS hiện tại; nhóm chi nhánh chỉ quản trị.",
            "support-statistics",
            OPS,
            paged(
                fields(
                    "openOnly",
                    bool(true),
                    "status",
                    values(null, "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"),
                    "groupBy",
                    values("NONE", groups)))));
    definitions.add(
        academic(
            "get_center_statistics",
            "Đếm hiện trạng chi nhánh active, lớp, khóa, phòng; liệt kê chi nhánh chưa có ADMIN mở"
                + " tài khoản và membership active. Không snapshot quá khứ. Chỉ COURSES_COUNT nhận programCode/audienceCode; mỗi khóa tính một lần kể cả có nhiều đối tượng. Mặc định đếm mọi trạng thái khóa; hỏi đang mở phải truyền OPEN.",
            "center-statistics",
            Set.of(SUPER_ADMIN, ADMIN),
            paged(
                fields(
                    "metric",
                    values(
                        "BRANCHES_ACTIVE",
                        "BRANCHES_ACTIVE",
                        "BRANCHES_WITHOUT_ADMIN",
                        "ROOMS_COUNT",
                        "CLASSES_COUNT",
                        "COURSES_COUNT"),
                    "status",
                    values(null, "AVAILABLE", "MAINTENANCE", "OPEN", "CLOSED"),
                    "programCode", values(null, "GE", "COMMUNICATION", "IELTS", "TOEIC"),
                    "audienceCode", values(null, "PRESCHOOL", "PRIMARY", "SECONDARY", "HIGH_SCHOOL", "ADULT")))));
    var accountRoles =
        role == ADMIN
            ? new String[] {"STAFF", "TEACHER"}
            : new String[] {"SUPER_ADMIN", "ADMIN", "STAFF", "TEACHER", "STUDENT"};
    definitions.add(
        new Definition(
            "find_management_accounts",
            "Tra tài khoản và phân công active trong scope, không có secrets/contact. ADMIN chỉ"
                + " STAFF/TEACHER; SA có ADMIN. Trùng tên hiện danh sách, không tự gán một người.",
            BackendGateway.Target.AUTH,
            "management-accounts",
            Set.of(SUPER_ADMIN, ADMIN),
            Map.copyOf(
                paged(
                    fields(
                        "keyword",
                        text(),
                        "accountId",
                        id(false),
                        "accountRole",
                        values(null, accountRoles),
                        "enabled",
                        bool(null))))));
    definitions.add(
        new Definition(
            "get_account_statistics",
            "Đếm distinct account theo role và trạng thái, membership active; ADMIN chỉ"
                + " STAFF/TEACHER. Tổng chuỗi không cộng trùng người nhiều chi nhánh.",
            BackendGateway.Target.AUTH,
            "account-statistics",
            Set.of(SUPER_ADMIN, ADMIN),
            Map.copyOf(fields("accountRole", values(null, accountRoles), "enabled", bool(null)))));
    return definitions.stream().filter(d -> d.roles().contains(role)).toList();
  }
}
