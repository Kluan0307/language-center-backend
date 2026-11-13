package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

class ExpansionTest {
  ChatServiceTest harness;

  @BeforeEach
  void setup() {
    harness = new ChatServiceTest();
    harness.setup();
  }

  @AfterEach
  void cleanup() {
    harness.clear();
  }

  @Test
  void courseFollowupKeepsKeywordAndResetsPage() {
    harness.model.add(
        "search_courses", Map.of("keyword", "giao tiếp", "maxTuition", 5000000, "page", 2));
    var first = harness.ask("Khóa giao tiếp dưới 5 triệu?");
    harness.model.add("search_courses", Map.of("maxTuition", 3000000));
    var next =
        harness.ask(
            harness.request("Còn dưới 3 triệu?", first.conversationId(), UUID.randomUUID(), null));
    assertEquals("NO_RESULTS", next.status());
    var query = harness.backend.calls.getLast();
    assertEquals("giao tiếp", query.get("keyword"));
    assertEquals("3000000", query.get("maxTuition"));
    assertEquals("false", query.get("maxInclusive"));
    assertEquals("0", query.get("page"));
  }

  @Test
  void availableUpcomingClassesPinBusinessFilters() {
    harness.model.add("search_classes", Map.of());
    assertEquals("ANSWER", harness.ask("Lớp nào còn chỗ và sắp khai giảng?").status());
    assertEquals("true", harness.backend.calls.getLast().get("recruitingOnly"));
    assertEquals("UPCOMING", harness.backend.calls.getLast().get("status"));
    assertEquals(0, harness.model.count);
  }

  @Test
  void overdueDebtDoesNotBecomeAllTuitions() {
    harness.model.add("search_tuitions", Map.of());
    assertEquals("ANSWER", harness.ask("Những khoản nợ quá hạn?").status());
    assertEquals("true", harness.backend.calls.getLast().get("overdue"));
    assertEquals(0, harness.model.count);
  }

  @Test
  void selectedStudentCanContinueToClassSchedule() {
    harness.backend.duplicate = false;
    harness.model.add("find_students", Map.of("keyword", "An"));
    var student = harness.ask("Tìm học viên An");
    harness.model.add("get_student_classes", Map.of("studentId", 11));
    harness.model.add(
        "list_sessions", Map.of("classId", 9, "from", "2026-11-09", "to", "2026-11-15"));
    // The fixture's class ID is discovered from student-classes, never guessed by the host.
    var response =
        harness.ask(
            harness.request(
                "Lịch tuần này của bạn ấy?", student.conversationId(), UUID.randomUUID(), null));
    assertEquals("ANSWER", response.status());
    assertEquals("list_sessions", response.sources().getLast().tool());
    assertEquals("9", harness.backend.calls.getLast().get("classId"));
    assertEquals(
        "11",
        harness.backend.calls.stream()
            .filter(c -> c.get("_path").endsWith("student-classes"))
            .findFirst()
            .orElseThrow()
            .get("studentId"));
  }

  @Test
  void missingOperationalSourcesAreExplicitlyUnsupported() {
    for (String question :
        List.of("Phòng trống tối nay?", "Giáo viên rảnh ngày mai?", "Học viên vắng nhiều?"))
      assertEquals("UNSUPPORTED_CAPABILITY", harness.ask(question).status());
    assertEquals(0, harness.model.count);
    assertTrue(harness.backend.calls.isEmpty());
  }

  @Test
  void multipleClassesRequireChoiceAndRevalidateStudentQuery() {
    harness.backend.duplicate = false;
    harness.backend.multipleClasses = true;
    harness.model.add("find_students", Map.of("keyword", "An"));
    var student = harness.ask("Tìm học viên An");
    harness.model.add("get_student_classes", Map.of("studentId", 11));
    var choices =
        harness.ask(
            harness.request(
                "Lịch tuần này của bạn ấy?", student.conversationId(), UUID.randomUUID(), null));
    assertEquals("CLARIFICATION_REQUIRED", choices.status());
    assertEquals(2, choices.choices().size());
    harness.model.add(
        "list_sessions", Map.of("classId", 10, "from", "2026-11-09", "to", "2026-11-15"));
    var answer =
        harness.ask(
            harness.request(
                "Chọn lớp thứ hai",
                choices.conversationId(),
                UUID.randomUUID(),
                new ChatModels.Selection(ChatModels.EntityType.CLASS, 10L)));
    assertEquals("ANSWER", answer.status());
    assertEquals("10", harness.backend.calls.getLast().get("classId"));
    assertTrue(
        harness.backend.calls.stream()
            .filter(c -> c.get("_path").endsWith("student-classes"))
            .allMatch(c -> "11".equals(c.get("studentId")) && !c.containsKey("keyword")));
  }
}
