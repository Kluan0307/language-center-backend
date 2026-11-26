package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

class ConversationLanguageTest {
  @TestFactory
  java.util.stream.Stream<DynamicTest> criticalAliases() {
    var examples = new LinkedHashMap<TopicPolicy.Family, List<String>>();
    examples.put(
        TopicPolicy.Family.PERSONNEL_STATS,
        List.of("Có bao nhiêu giáo viên?", "So luong giang vien o CN1?", "Co may GV o CN 1?"));
    examples.put(
        TopicPolicy.Family.PERSONNEL_SELF,
        List.of("Văn bằng của tôi?", "Trình độ học vấn của tôi?", "Bang cap cua toi?"));
    examples.put(
        TopicPolicy.Family.COURSES,
        List.of(
            "Khóa IELTS nào đang mở?", "Khoa giao tiep duoi 5 trieu?", "Các khóa học đang mở?"));
    examples.put(
        TopicPolicy.Family.STUDENT_CLASSES,
        List.of(
            "HV An đang học lớp nào?",
            "Học sinh An đang học lớp nào?",
            "Hoc vien An hoc tai lop nao?"));
    examples.put(
        TopicPolicy.Family.SCHEDULE,
        List.of("Lịch giảng dạy tuần này?", "Lich day tuan nay?", "Lịch GV An?"));
    examples.put(
        TopicPolicy.Family.ROSTER_STATS,
        List.of(
            "Lớp L1 có bao nhiêu học viên?",
            "Sĩ số lớp L1?",
            "Tong so hoc vien trong cac lop toi phu trach?"));
    examples.put(
        TopicPolicy.Family.DEBT_STATS,
        List.of("Tổng công nợ còn lại?", "Tong no qua han?", "Cong no tre han la bao nhieu?"));
    examples.put(
        TopicPolicy.Family.CASH_FLOW,
        List.of("Thực thu tháng này?", "Dong tien hom nay?", "Số tiền hoàn trong kỳ?"));
    examples.put(
        TopicPolicy.Family.SUPPORT,
        List.of(
            "Yêu cầu hỗ trợ #1 đang ở trạng thái nào?",
            "Tra yeu cau ho tro #1",
            "Support ticket #1"));
    examples.put(
        TopicPolicy.Family.LEADS,
        List.of(
            "Khách nào của tôi cần liên hệ hôm nay?",
            "Tim lead theo ten An",
            "Khách hàng được giao theo tên An?"));
    examples.put(
        TopicPolicy.Family.ACCOUNTS,
        List.of(
            "Admin An quản lý chi nhánh nào?",
            "Tai khoan nao dang bi khoa?",
            "Tài khoản nào đang bị khóa?"));
    examples.put(
        TopicPolicy.Family.ABSENCES,
        List.of(
            "Học viên vắng nhiều?", "Hoc vien vang it nhat 3 buoi?", "Số buổi vắng của học viên?"));
    return examples.entrySet().stream()
        .flatMap(
            e ->
                e.getValue().stream()
                    .map(
                        q ->
                            DynamicTest.dynamicTest(
                                q,
                                () ->
                                    assertEquals(
                                        e.getKey(),
                                        new TopicPolicy().family(q, TopicPolicy.Family.NONE)))));
  }

  ChatServiceTest h;

  @BeforeEach
  void setup() {
    h = new ChatServiceTest();
    h.setup();
  }

  @AfterEach
  void close() {
    h.clear();
  }

  @Test
  void aliasesRouteCountsQualificationsAndPreserveCodes() {
    var p = new TopicPolicy();
    for (String q :
        List.of(
            "Số lượng giảng viên ở CN 1?", "Co may GV o CN 1?", "Đếm số giáo viên tại chi nhánh 1"))
      assertEquals(TopicPolicy.Family.PERSONNEL_STATS, p.family(q, TopicPolicy.Family.NONE));
    assertEquals("hv-123 gv#11", p.normalize("HV-123 GV#11"));
    assertEquals(
        TopicPolicy.Family.PERSONNEL_SELF,
        p.family("Trình độ học vấn của tôi?", TopicPolicy.Family.NONE));
    assertEquals(
        TopicPolicy.Family.DEBT_STATS,
        p.family("Tổng công nợ tháng trước?", TopicPolicy.Family.NONE));
  }

  @Test
  void priceRefinementKeepsServerFiltersEvenIfModelChangesKeyword() {
    h.model.add(
        "search_courses", Map.of("keyword", "Giao tiếp", "maxTuition", 5000000, "status", "OPEN"));
    var first = h.ask("Khóa giao tiếp dưới 5 triệu?");
    h.model.add(
        "search_courses", Map.of("keyword", "IELTS", "maxTuition", 3000000, "status", "CLOSED"));
    h.ask(h.request("Còn dưới 3 triệu?", first.conversationId(), UUID.randomUUID(), null));
    var args = h.backend.calls.getLast();
    assertEquals("Giao tiếp", args.get("keyword"));
    assertEquals("OPEN", args.get("status"));
    assertEquals("3000000", args.get("maxTuition"));
  }

  @Test
  void activeRefinementKeepsNameAndDoesNotAddPageToScalarTool() {
    h.model.add("get_personnel_statistics", Map.of("kind", "TEACHER", "activeOnly", false));
    var first = h.ask("Có bao nhiêu giáo viên?");
    h.model.add("get_personnel_statistics", Map.of("kind", "STAFF", "activeOnly", false));
    assertEquals(
        "ANSWER",
        h.ask(
                h.request(
                    "Chỉ lấy người đang hoạt động",
                    first.conversationId(),
                    UUID.randomUUID(),
                    null))
            .status());
    assertEquals("TEACHER", h.backend.calls.getLast().get("kind"));
    assertEquals("true", h.backend.calls.getLast().get("activeOnly"));
    assertFalse(h.backend.calls.getLast().containsKey("page"));
  }

  @Test
  void ordinalWithConResumesPendingSelection() {
    h.model.add("find_students", Map.of("keyword", "An"));
    var first = h.ask("Học viên An đang học lớp nào?");
    h.model.add("get_student_classes", Map.of("studentId", 12));
    assertEquals(
        "ANSWER",
        h.ask(h.request("Còn người thứ hai?", first.conversationId(), UUID.randomUUID(), null))
            .status());
    assertEquals("12", h.backend.calls.getLast().get("studentId"));
  }

  @Test
  void femalePronounRetainsVerifiedPersonnelId() {
    h.backend.duplicate = false;
    h.model.add("find_personnel", Map.of("keyword", "An"));
    h.model.add("get_personnel_profile", Map.of("personnelId", 11));
    var first = h.ask("Giáo viên An có bằng cấp gì?");
    h.model.add("get_teacher_classes", Map.of("teacherId", 11));
    assertEquals(
        "ANSWER",
        h.ask(
                h.request(
                    "Còn cô ấy đang dạy lớp nào?", first.conversationId(), UUID.randomUUID(), null))
            .status());
  }

  @Test
  void pronounWithoutContextAsksBeforeModelCall() {
    assertEquals("CLARIFICATION_REQUIRED", h.ask("Còn cô ấy?").status());
    assertEquals(0, h.model.count);
  }

  @Test
  void newNamedEntityCannotReuseOldPersonnelId() {
    h.backend.duplicate = false;
    h.model.add("find_personnel", Map.of("keyword", "An"));
    h.model.add("get_personnel_profile", Map.of("personnelId", 11));
    var first = h.ask("Giáo viên An có bằng cấp gì?");
    h.model.add("get_personnel_profile", Map.of("personnelId", 11));
    var next =
        h.ask(
            h.request(
                "Còn giáo viên Bình có bằng cấp gì?",
                first.conversationId(),
                UUID.randomUUID(),
                null));
    assertEquals("CLARIFICATION_REQUIRED", next.status());
  }

  @Test
  void totalUsesFreshServerCountAndDoesNotSumVisibleRows() {
    h.model.add("search_courses", Map.of("keyword", "GT"));
    var first = h.ask("Khóa giao tiếp?");
    int calls = h.model.count;
    var result =
        h.ask(
            h.request(
                "Có bao nhiêu tất cả, không chỉ trang này?",
                first.conversationId(),
                UUID.randomUUID(),
                null));
    assertEquals(calls, h.model.count);
    assertTrue(result.cards().toString().contains("value=50"));
    assertEquals(2, h.backend.calls.size());
  }

  @Test
  void branchAliasNeverSilentlyReturnsCurrentBranch() {
    assertEquals("CLARIFICATION_REQUIRED", h.ask("Có bao nhiêu GV ở CN 2?").status());
    assertTrue(h.backend.calls.isEmpty());
  }

  @Test
  void cashWindowFollowupUsesLastVerifiedPeriod() {
    h.model.add("get_cash_flow", Map.of("from", "2026-11-01", "to", "2026-11-10"));
    var first = h.ask("Thực thu từ 2026-11-01 đến 2026-11-10?");
    h.model.add("get_cash_flow", Map.of("from", "2025-01-01", "to", "2025-01-10"));
    h.ask(
        h.request(
            "Số tiền hoàn trong khoảng thời gian đó?",
            first.conversationId(),
            UUID.randomUUID(),
            null));
    assertEquals("2026-11-01", h.backend.calls.getLast().get("from"));
    assertEquals("2026-11-10", h.backend.calls.getLast().get("to"));
  }

  @Test
  void accountLockRefinementKeepsRoleAndChangesEnabled() {
    h.backend.role = "SUPER_ADMIN";
    h.model.add("find_management_accounts", Map.of("accountRole", "ADMIN", "enabled", true));
    var c = h.ask("Admin An quản lý chi nhánh nào?");
    h.model.add("find_management_accounts", Map.of("accountRole", "TEACHER", "enabled", true));
    h.ask(h.request("Chỉ lấy đã khóa", c.conversationId(), UUID.randomUUID(), null));
    assertEquals("ADMIN", h.backend.calls.getLast().get("accountRole"));
    assertEquals("false", h.backend.calls.getLast().get("enabled"));
  }

  @Test
  void leadTodayRefinementKeepsNameAndChangesFollowUp() {
    h.backend.role = "STAFF";
    h.model.add("search_leads", Map.of("keyword", "An", "followUp", "OVERDUE"));
    var c = h.ask("Khách An theo tên quá lịch hẹn?");
    h.model.add("search_leads", Map.of("keyword", "Binh", "followUp", "OVERDUE"));
    h.ask(h.request("Chỉ xem hôm nay", c.conversationId(), UUID.randomUUID(), null));
    assertEquals("An", h.backend.calls.getLast().get("keyword"));
    assertEquals("TODAY", h.backend.calls.getLast().get("followUp"));
  }

  @Test
  void inactiveOnlyIsNotMistakenForAllStatuses() {
    h.model.add("get_personnel_statistics", Map.of());
    var c = h.ask("Có bao nhiêu giáo viên?");
    assertEquals(
        "UNSUPPORTED_CAPABILITY",
        h.ask(
                h.request(
                    "Chỉ lấy người ngừng hoạt động", c.conversationId(), UUID.randomUUID(), null))
            .status());
  }
}
