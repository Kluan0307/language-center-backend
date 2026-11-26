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
    HELP,
    PERSONNEL,
    PERSONNEL_STATS,
    PERSONNEL_TEACHING,
    PERSONNEL_SELF,
    RESOURCES,
    AVAILABLE_TEACHERS,
    ACADEMIC_STATS,
    ROSTER_STATS,
    STUDENT_PROFILE,
    ABSENCES,
    MISSING_ATTENDANCE,
    EXAMS,
    ADMISSIONS,
    DEBT_STATS,
    CASH_FLOW,
    CASH_COMPARE,
    CARE_STATS,
    SUPPORT,
    SUPPORT_STATS,
    CENTER_STATS,
    ACCOUNTS,
    ACCOUNT_STATS
  }

  public String normalize(String text) {
    return QueryLanguage.aliases(
        Normalizer.normalize(
                Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("\\p{Cf}", "")
            .replaceAll("\\p{Cc}", " ")
            .replace('đ', 'd')
            .replaceAll("\\s+", " ")
            .trim());
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
    if (contains(s, "anh chup qua khu chua ho tro"))
      return new Review("", "UNSUPPORTED_CAPABILITY", List.of());
    if (contains(s, "qua han cham", "den han")
        && contains(s, "bai kiem tra", "cham diem", "chua cham"))
      return new Review("", "UNSUPPORTED_CAPABILITY", List.of());
    var range =
        Pattern.compile(
                "(?:tu|from)\\s+([0-9]+(?:[.,][0-9]+)?)\\s*(?:trieu|million)?\\s*(?:den|to)\\s+([0-9]+(?:[.,][0-9]+)?)\\s*(?:trieu|million)")
            .matcher(s);
    if (range.find()
        && new java.math.BigDecimal(range.group(1).replace(',', '.'))
                .compareTo(new java.math.BigDecimal(range.group(2).replace(',', '.')))
            > 0) return new Review("", "CLARIFICATION_REQUIRED", List.of());
    if (Set.of(
                Family.STATS,
                Family.PERSONNEL_STATS,
                Family.ACADEMIC_STATS,
                Family.DEBT_STATS,
                Family.CARE_STATS,
                Family.SUPPORT_STATS,
                Family.CENTER_STATS,
                Family.ACCOUNT_STATS)
            .contains(family(question, previous))
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
    if (QueryLanguage.pronoun(s) && contains(s, "tuan sau", "tuan nay", "ngay mai"))
      return Family.SCHEDULE;
    if (previous != Family.NONE && (QueryLanguage.refinement(s) || QueryLanguage.total(s)))
      return previous;
    if (QueryLanguage.pronoun(s)
        && Set.of(Family.PERSONNEL, Family.PERSONNEL_TEACHING).contains(previous)
        && contains(s, "dang day", "phu trach", "day khoa")) return Family.PERSONNEL_TEACHING;
    if (contains(s, "don online", "don thanh toan")
        && contains(s, "pending", "dang cho", "co duoc tinh", "tinh la")) return Family.HELP;
    if (s.matches("(?s).*\\bns[0-9]{3,}\\b.*") && !contains(s,"nhan vien","admin ")) return Family.ACCOUNTS;

    if (contains(s, "yeu cau ho tro", "support ticket"))
      return contains(s, "bao nhieu", "tong so", "dem ") ? Family.SUPPORT_STATS : Family.SUPPORT;
    if (contains(s, "so sanh") && contains(s, "thuc thu", "dong tien", "doanh thu"))
      return Family.CASH_COMPARE;
    if (contains(
            s, "thuc thu", "doanh thu", "tong tien hoan", "so tien hoan", "tien huy", "dong tien")
        && !contains(s, "phieu nay", "phieu thu nao", "phieu thu #")) return Family.CASH_FLOW;
    if (contains(s, "tong no", "tong cong no", "cong no qua han", "no qua han la bao nhieu")
        || contains(s, "cong no", "con no")
            && contains(s, "tong", "bao nhieu", "theo tung chi nhanh")) return Family.DEBT_STATS;
    if (contains(s, "khach hang", "khach", "lead")
        && contains(s, "bao nhieu", "tong so", "chi nhanh nao", "dem ")) return Family.CARE_STATS;
    if (contains(s, "tai khoan") && contains(s, "dem ", "bao nhieu", "theo role", "tong so"))
      return Family.ACCOUNT_STATS;
    if (contains(s, "khoa hoc", "khoa ") && contains(s, "bao nhieu", "tong so", "so luong", "dem "))
      return Family.CENTER_STATS;
    if (contains(
            s,
            "admin ",
            "admin phu trach",
            "chi nhanh dang hoat dong",
            "chi nhanh chua",
            "chi nhanh nao chua")
        || contains(
            s,
            "bao nhieu chi nhanh",
            "dem chi nhanh",
            "bao nhieu phong",
            "bao nhieu khoa",
            "tong so lop",
            "tong so phong"))
      return contains(s, "admin ") && !contains(s, "chua co", "chua duoc")
          ? Family.ACCOUNTS
          : Family.CENTER_STATS;
    if (contains(s, "tai khoan nao", "tai khoan bi khoa", "tai khoan dang bi khoa"))
      return Family.ACCOUNTS;
    if (contains(s, "phieu nay", "phieu thu", "bien lai")) return Family.RECEIPTS;
    if (s.startsWith("tim hoc vien ")) return Family.STUDENTS;
    if (contains(s, "hoc luc nao", "hoc may gio")) return Family.SCHEDULE;
    if (contains(s, "khoan nao den han", "khoan sap den han")) return Family.TUITION;
    if (contains(s, "khach", "lead")
        && contains(s, "qua lich hen", "hom nay", "theo ten", "duoc giao")) return Family.LEADS;
    if (contains(s, "phong trong", "phong nao", "phong hoc")
        && !contains(s, "buoi tiep theo", "buoi sap toi")) return Family.RESOURCES;
    if (contains(
        s, "giao vien ranh", "giao vien trong lich", "giao vien nao khong", "khong co lich trung"))
      return Family.AVAILABLE_TEACHERS;
    if (contains(s, "theo tung chi nhanh", "chi nhanh nao co nhieu", "nhom theo chi nhanh")
        && contains(s, "hoc vien", "lop")) return Family.ACADEMIC_STATS;
    if (contains(s, "vang nhieu", "vang it nhat", "buoi vang", "so buoi vang"))
      return Family.ABSENCES;
    if (contains(s, "ai chua duoc diem danh", "ai chua diem danh")) return Family.ATTENDANCE;
    if (contains(
        s, "chua diem danh", "thieu diem danh", "chua hoan tat diem danh", "con thieu diem danh"))
      return Family.MISSING_ATTENDANCE;
    if (contains(s, "bao nhieu hoc vien", "si so", "tong so hoc vien")
        && (s.startsWith("lop ")
            || contains(s, "trong lop", "trong cac lop", "lop cua toi", "lop toi", "si so")))
      return Family.ROSTER_STATS;
    if (contains(
        s,
        "bao nhieu bai kiem tra",
        "bai kiem tra nao",
        "chua cham xong",
        "con thieu diem",
        "danh sach bai kiem tra")) return Family.EXAMS;
    if (contains(s, "ho so hoc vien", "thong tin hoc vien", "ho so cua ban ay"))
      return Family.STUDENT_PROFILE;
    if (contains(s, "trinh do dau vao", "dieu kien dau vao")) return Family.ADMISSIONS;
    if (contains(s, "lich") && contains(s, "giao vien", "thay ", "co ")) return Family.SCHEDULE;
    if (contains(s, "giao vien nao phu trach lop", "ai phu trach lop", "con bao nhieu cho"))
      return Family.CLASSES;
    if (contains(s, "ai co diem 0", "ai chua co diem", "diem 0", "thang diem", "bai kiem tra do"))
      return Family.RESULTS;
    if (contains(
        s,
        "buoi tiep theo",
        "buoi sap toi",
        "buoi nao toi dang day thay",
        "buoi hoc bu",
        "ngay mai toi",
        "buoi nao trong lich")) return Family.SCHEDULE;
    if (contains(s, "toi") && contains(s, "dang day", "phu trach") && contains(s, "khoa", "lop"))
      return Family.PERSONNEL_TEACHING;
    if ((contains(s, "ho so", "bang cap", "chung chi", "chuyen mon")
            && contains(s, "cua toi", "ho so toi"))
        || s.startsWith("toi ") && contains(s, "bang cap", "chung chi", "chuyen mon")
        || contains(s, "toi") && contains(s, "phan cong") && contains(s, "chi nhanh"))
      return Family.PERSONNEL_SELF;
    if (contains(s, "giao vien", "nhan vien")
        && contains(s, "bao nhieu", "tong so", "so luong", "dem ")) return Family.PERSONNEL_STATS;
    if (contains(s, "giao vien", "thay ", "co ")
        && contains(
            s,
            "dang day",
            "phu trach lop",
            "day nhung khoa",
            "day cac khoa",
            "duoc giao nhung lop",
            "duoc giao cac lop")) return Family.PERSONNEL_TEACHING;
    if (contains(
        s,
        "giao vien",
        "nhan vien",
        "bang cap",
        "chung chi",
        "trinh do hoc van",
        "chuyen mon",
        "vi tri nguoi",
        "ho so nguoi")) return Family.PERSONNEL;
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
        "khoa ",
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
      case STUDENTS -> Set.of("search_classes", "find_students");
      case STUDENT_CLASSES -> Set.of("find_students", "get_student_classes");
      case STATS -> Set.of("get_student_statistics");
      case SCHEDULE ->
          Set.of(
              "search_classes",
              "get_student_classes",
              "list_sessions",
              "find_personnel",
              "get_my_personnel_profile");
      case ATTENDANCE -> Set.of("search_classes", "list_sessions", "get_session_attendance");
      case RESULTS ->
          Set.of(
              "find_students",
              "get_student_classes",
              "search_classes",
              "list_learning_exams",
              "get_learning_results");
      case RESOURCES -> Set.of("search_rooms");
      case AVAILABLE_TEACHERS -> Set.of("find_available_teachers");
      case ACADEMIC_STATS -> Set.of("get_academic_statistics");
      case ROSTER_STATS -> Set.of("search_classes", "get_roster_statistics");
      case STUDENT_PROFILE -> Set.of("find_students", "get_student_profile");
      case ABSENCES -> Set.of("search_classes", "find_students", "get_attendance_summary");
      case MISSING_ATTENDANCE -> Set.of("search_classes", "list_missing_attendance");
      case EXAMS -> Set.of("search_classes", "list_learning_exams");
      case ADMISSIONS -> Set.of("search_course_admissions");
      case TUITION -> Set.of("find_students", "get_student_classes", "search_tuitions");
      case RECEIPTS ->
          Set.of("find_students", "get_student_classes", "search_tuitions", "search_receipts");
      case LEADS -> Set.of("list_priority_leads", "search_leads");
      case DEBT_STATS -> Set.of("find_students", "search_classes", "get_debt_statistics");
      case CASH_FLOW -> Set.of("find_students", "search_classes", "get_cash_flow");
      case CASH_COMPARE -> Set.of("compare_cash_flow");
      case CARE_STATS -> Set.of("find_management_accounts", "get_care_statistics");
      case SUPPORT -> Set.of("find_students", "search_support_requests");
      case SUPPORT_STATS -> Set.of("get_support_statistics");
      case CENTER_STATS -> Set.of("get_center_statistics");
      case ACCOUNTS -> Set.of("find_management_accounts");
      case ACCOUNT_STATS -> Set.of("get_account_statistics");
      case HELP -> Set.of("get_usage_help");
      case PERSONNEL -> Set.of("find_personnel", "get_personnel_profile");
      case PERSONNEL_STATS -> Set.of("get_personnel_statistics");
      case PERSONNEL_TEACHING ->
          Set.of("find_personnel", "get_my_personnel_profile", "get_teacher_classes");
      case PERSONNEL_SELF -> Set.of("get_my_personnel_profile");
      default -> Set.of();
    };
  }

  public Map<String, Object> directArguments(String question, String tool) {
    String text = normalize(question).replaceAll("[?.!]$", "");
    if (tool.equals("get_usage_help")
        && contains(text, "don online", "don thanh toan")
        && contains(text, "pending", "dang cho", "co duoc tinh", "tinh la"))
      return Map.of("topic", "PAYMENT_ACCOUNTING");
    if (tool.equals("get_attendance_summary")
        && Set.of("hoc vien vang nhieu", "hoc vien nao vang nhieu").contains(text)) {
      var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
      return Map.of(
          "from", today.minusDays(29).toString(), "to", today.toString(), "minAbsences", 3);
    }
    if (tool.equals("search_classes")
        && Set.of("lop nao con cho va sap khai giang", "lop con cho va sap khai giang")
            .contains(text)) return Map.of("recruitingOnly", true, "status", "UPCOMING");
    if (tool.equals("search_tuitions")
        && Set.of("nhung khoan no qua han", "cac khoan no qua han", "hoc phi nao dang qua han")
            .contains(text)) return Map.of("overdue", true);
    return null;
  }

  public boolean terminal(Family family, String tool) {
    if (family == Family.LEADS && tool.equals("search_leads")) return true;
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
          case PERSONNEL -> "get_personnel_profile";
          case PERSONNEL_STATS -> "get_personnel_statistics";
          case PERSONNEL_TEACHING -> "get_teacher_classes";
          case PERSONNEL_SELF -> "get_my_personnel_profile";
          case RESOURCES -> "search_rooms";
          case AVAILABLE_TEACHERS -> "find_available_teachers";
          case ACADEMIC_STATS -> "get_academic_statistics";
          case ROSTER_STATS -> "get_roster_statistics";
          case STUDENT_PROFILE -> "get_student_profile";
          case ABSENCES -> "get_attendance_summary";
          case MISSING_ATTENDANCE -> "list_missing_attendance";
          case EXAMS -> "list_learning_exams";
          case ADMISSIONS -> "search_course_admissions";
          case DEBT_STATS -> "get_debt_statistics";
          case CASH_FLOW -> "get_cash_flow";
          case CASH_COMPARE -> "compare_cash_flow";
          case CARE_STATS -> "get_care_statistics";
          case SUPPORT -> "search_support_requests";
          case SUPPORT_STATS -> "get_support_statistics";
          case CENTER_STATS -> "get_center_statistics";
          case ACCOUNTS -> "find_management_accounts";
          case ACCOUNT_STATS -> "get_account_statistics";
          default -> "";
        });
  }

  private boolean contains(String text, String... values) {
    return Arrays.stream(values).anyMatch(text::contains);
  }
}
