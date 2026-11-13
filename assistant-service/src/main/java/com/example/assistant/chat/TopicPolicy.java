package com.example.assistant.chat;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TopicPolicy {
  public enum Family {
    NONE,
    COURSES,
    CLASSES,
    STUDENTS,
    STUDENT_CLASSES,
    STATS,
    SCHEDULE,
    ATTENDANCE,
    RESULTS,
    TUITION,
    RECEIPTS,
    LEADS,
    HELP
  }

  public String normalize(String text) {
    return Normalizer.normalize(
            Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT),
            Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("\\p{Cf}", "")
        .replaceAll("\\p{Cc}", " ")
        .replace('đ', 'd')
        .replaceAll("\\s+", " ")
        .trim();
  }

  public record Review(String question, String status, List<String> warnings) {
    public String reason() {
      return "CLARIFICATION_REQUIRED".equals(status) ? "NEED_RANGE" : "NONE";
    }
  }

  public boolean injection(String text) {
    String s = normalize(text).replace("\u200b", "");
    return contains(
            s,
            "ignore previous",
            "ignore all",
            "disregard instructions",
            "bo qua huong dan",
            "bo moi huong dan",
            "bo kiem quyen",
            "bo qua kiem quyen",
            "system prompt",
            "system message",
            "developer message",
            "jailbreak",
            "act as super",
            "toi la superadmin",
            "toi la super admin",
            "role=system",
            "role: system",
            "functionresponse",
            "functioncall",
            "<script",
            ".env",
            "api key",
            "hashsecret")
        || Pattern.compile(
                "(?i)(?:AIza[\\w-]{25,}|sk-[a-zA-Z0-9_-]{16,}|Bearer\\s+[a-zA-Z0-9._-]{10,})")
            .matcher(text)
            .find()
        || s.matches(
            ".*(?:https?://|file://|javascript:|select .+ from |drop table|union select).*");
  }

  public Review review(String question, Family previous) {
    if (injection(question)) return new Review("", "OUT_OF_SCOPE", List.of());
    if (write(question)) return new Review("", "UNSUPPORTED_ACTION", List.of());
    String s = normalize(question);
    if (contains(
        s,
        "phong trong",
        "giao vien ranh",
        "giao vien trong lich",
        "vang nhieu",
        "chua cham xong",
        "chua diem danh")) return new Review("", "UNSUPPORTED_CAPABILITY", List.of());
    var range =
        Pattern.compile(
                "(?:tu|from)\\s+([0-9]+(?:[.,][0-9]+)?)\\s*(?:trieu|million)?\\s*(?:den|to)\\s+([0-9]+(?:[.,][0-9]+)?)\\s*(?:trieu|million)")
            .matcher(s);
    if (range.find()
        && new java.math.BigDecimal(range.group(1).replace(',', '.'))
                .compareTo(new java.math.BigDecimal(range.group(2).replace(',', '.')))
            > 0) return new Review("", "CLARIFICATION_REQUIRED", List.of());
    if (family(question, previous) == Family.STATS
        && (s.contains("nam ngoai")
            || s.contains("thang truoc")
            || s.contains("last year")
            || s.matches(".*(?:thang|nam) [0-9]+.*")))
      return new Review("", "UNSUPPORTED_CAPABILITY", List.of());
    if (!outside(question)) return new Review(question, null, List.of());
    var parts =
        Pattern.compile("(?iu)\\s+(?:rồi|roi|và|va|sau đó|sau do|then|and|also)\\s+|[;\\n]+")
            .split(question);
    var business =
        Arrays.stream(parts)
            .filter(p -> !outside(p) && !injection(p) && family(p, previous) != Family.NONE)
            .toList();
    if (business.isEmpty()) return new Review("", "OUT_OF_SCOPE", List.of());
    return new Review(
        String.join("; ", business),
        null,
        List.of("Chỉ xử lý phần nghiệp vụ trung tâm; phần ngoài phạm vi đã được bỏ qua."));
  }

  /** Database strings never become instructions or executable markup, even in later model calls. */
  public String displayText(String value) {
    if (injection(value)) return "[Nội dung không hiển thị]";
    String text = value.replaceAll("[\\p{Cf}\\p{Cc}]", " ").replaceAll("<[^>]*>", "");
    return text.length() > 500 ? text.substring(0, 500) : text;
  }

  public boolean outside(String question) {
    String s = normalize(question);
    return contains(
        s,
        "thoi tiet",
        "weather",
        "chinh tri",
        "ke chuyen cuoi",
        "joke",
        "viet code",
        "write code",
        "write a poem",
        "viet tho",
        "cong thuc nau",
        "recipe",
        "lap trinh",
        "react native",
        "giai bai tap",
        "lam bai tap",
        "dich doan",
        "dich thuat",
        "translate",
        "viet bai ielts",
        "viet essay",
        "write an essay",
        "bo qua huong dan",
        "ignore previous",
        "system prompt",
        ".env",
        "api key");
  }

  public boolean write(String question) {
    String s = normalize(question);
    if (s.startsWith("cach ") || s.startsWith("huong dan ") || s.startsWith("how to "))
      return false;
    return contains(
        s,
        "xoa hoc vien",
        "delete student",
        "create class",
        "add student",
        "update grade",
        "xoa tat ca",
        "xoa lop",
        "tao lop",
        "them hoc vien",
        "cap nhat diem",
        "sua diem",
        "thu tien giup",
        "thu hoc phi cho",
        "xac nhan da thanh toan",
        "thay doi mat khau",
        "xac nhan thanh toan",
        "reset mat khau",
        "phan cong giup");
  }

  public boolean leadPrediction(String question) {
    return contains(
        normalize(question),
        "tiem nang nhat",
        "kha nang mua",
        "xac suat dang ky",
        "conversion prediction");
  }

  public boolean confirm(String question) {
    String s = normalize(question);
    return Set.of(
                "co",
                "dong y",
                "ok",
                "duoc",
                "yes",
                "co, dung danh sach uu tien cham soc.",
                "co, dung danh sach uu tien cham soc")
            .contains(s)
        || s.contains("uu tien cham soc");
  }

  public boolean next(String question) {
    return contains(normalize(question), "xem tiep", "trang sau", "next page");
  }

  public Family family(String question, Family previous) {
    String s = normalize(question);
    if (contains(s, "lich cua ban ay", "lich cua nguoi do", "lich cua hoc vien do"))
      return Family.SCHEDULE;
    if (contains(s, "lop con cho", "lop nao con cho", "lop sap khai giang", "classes with seats"))
      return Family.CLASSES;
    if (contains(s, "no qua han", "chua dong tien", "chua nop hoc phi")) return Family.TUITION;
    if (contains(s, "khach can goi", "can cham soc", "hen lien he")) return Family.LEADS;
    if (contains(s, "cach ", "huong dan", "how to")
        && contains(
            s,
            "lich",
            "schedule",
            "lop",
            "class",
            "hoc vien",
            "student",
            "hoc phi",
            "tuition",
            "diem",
            "grade",
            "phieu thu",
            "receipt",
            "tai khoan",
            "account",
            "khoa",
            "course",
            "khach hang",
            "lead")) return Family.HELP;
    if (contains(s, "phieu thu", "bien lai", "receipt", "lich su dong tien"))
      return Family.RECEIPTS;
    if (contains(s, "diem danh", "chuyen can", "vang", "co mat", "attendance"))
      return Family.ATTENDANCE;
    if (contains(
        s,
        "tong so hoc vien",
        "tong hoc vien",
        "so luong hoc vien",
        "bao nhieu hoc vien",
        "total students",
        "student count")) return Family.STATS;
    if (contains(s, "khach hang", "tu van", "lien he lai", "lead")) return Family.LEADS;
    if (contains(
        s,
        "khoa hoc",
        "khoa giao tiep",
        "courses",
        "hoc phi khoa",
        "khoa hoc phi",
        "khoa tu ",
        "khoa duoi ",
        "khoa toi da ")) return Family.COURSES;
    if (contains(s, "hoc phi", "cong no", "con no", "tuition", "debt")) return Family.TUITION;
    if (contains(s, "diem", "ket qua hoc", "grades", "scores")) return Family.RESULTS;
    if (contains(
        s,
        "lich hoc",
        "lich day",
        "buoi hoc",
        "schedule",
        "tuan nay",
        "toi day",
        "lich thang",
        "lich tuan")) return Family.SCHEDULE;
    if (contains(s, "dang hoc lop", "hoc tai lop", "hoc lop nao")
        || contains(s, "hoc vien", "student") && contains(s, "lop", "class"))
      return Family.STUDENT_CLASSES;
    if (contains(s, "hoc vien", "student", "ho so")) return Family.STUDENTS;
    if (contains(s, "lop", "classes")) return Family.CLASSES;
    if (previous != Family.NONE
        && (s.length() < 120
            && ((s.startsWith("con ")
                    && contains(
                        s,
                        "nguoi do",
                        "lop do",
                        "buoi do",
                        "duoi ",
                        "tren ",
                        "trieu",
                        "hoc vien do"))
                || s.startsWith("nguoi ")
                || s.startsWith("lop do")
                || s.startsWith("buoi do")
                || next(s)
                || s.matches("#?[0-9]+")))) return previous;
    return Family.NONE;
  }

  public Set<String> tools(Family family) {
    return switch (family) {
      case COURSES -> Set.of("search_courses");
      case CLASSES -> Set.of("search_classes", "search_courses");
      case STUDENTS -> Set.of("find_students");
      case STUDENT_CLASSES -> Set.of("find_students", "get_student_classes");
      case STATS -> Set.of("get_student_statistics");
      case SCHEDULE -> Set.of("search_classes", "get_student_classes", "list_sessions");
      case ATTENDANCE -> Set.of("search_classes", "list_sessions", "get_session_attendance");
      case RESULTS -> Set.of("find_students", "get_student_classes", "get_learning_results");
      case TUITION -> Set.of("find_students", "get_student_classes", "search_tuitions");
      case RECEIPTS ->
          Set.of("find_students", "get_student_classes", "search_tuitions", "search_receipts");
      case LEADS -> Set.of("list_priority_leads");
      case HELP -> Set.of("get_usage_help");
      default -> Set.of();
    };
  }

  public Map<String, Object> directArguments(String question, String tool) {
    String text = normalize(question).replaceAll("[?.!]$", "");
    if (tool.equals("search_classes")
        && Set.of("lop nao con cho va sap khai giang", "lop con cho va sap khai giang")
            .contains(text)) return Map.of("recruitingOnly", true, "status", "UPCOMING");
    if (tool.equals("search_tuitions")
        && Set.of("nhung khoan no qua han", "cac khoan no qua han", "hoc phi nao dang qua han")
            .contains(text)) return Map.of("overdue", true);
    return null;
  }

  public boolean terminal(Family family, String tool) {
    return tool.equals(
        switch (family) {
          case COURSES -> "search_courses";
          case CLASSES -> "search_classes";
          case STUDENTS -> "find_students";
          case STUDENT_CLASSES -> "get_student_classes";
          case STATS -> "get_student_statistics";
          case SCHEDULE -> "list_sessions";
          case ATTENDANCE -> "get_session_attendance";
          case RESULTS -> "get_learning_results";
          case TUITION -> "search_tuitions";
          case RECEIPTS -> "search_receipts";
          case LEADS -> "list_priority_leads";
          case HELP -> "get_usage_help";
          default -> "";
        });
  }

  private boolean contains(String text, String... values) {
    return Arrays.stream(values).anyMatch(text::contains);
  }
}
