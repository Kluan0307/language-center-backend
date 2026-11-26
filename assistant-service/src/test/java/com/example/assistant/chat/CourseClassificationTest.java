package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;

class CourseClassificationTest {
  ChatServiceTest h;
  @BeforeEach void setup() { h = new ChatServiceTest(); h.setup(); }
  @AfterEach void close() { h.clear(); }

  @Test void canonicalPhrasesAndBoundaries() {
    var policy = new TopicPolicy();
    var cases = Map.of("Mẫu giáo", "PRESCHOOL", "Tiểu học", "PRIMARY", "Học sinh THCS", "SECONDARY", "THPT", "HIGH_SCHOOL", "Người đi làm", "ADULT");
    cases.forEach((phrase, code) -> assertEquals(code,
        QueryLanguage.classification(policy.normalize("Khóa IELTS cho " + phrase)).get("audienceCode")));
    assertEquals("COMMUNICATION", QueryLanguage.classification(policy.normalize("Khóa giao tiếp người lớn")).get("programCode"));
    assertEquals("GE", QueryLanguage.classification("tieng anh tong quat").get("programCode"));
    assertFalse(QueryLanguage.classification("george primaryschool").containsKey("programCode"));
    assertFalse(QueryLanguage.classification("ielts va toeic cho thcs va thpt").containsKey("audienceCode"));
    assertFalse(QueryLanguage.classification("ielts va toeic").containsKey("programCode"));
    assertTrue(QueryLanguage.classification("hoc vien an trinh do b2").isEmpty());
  }

  @Test void classificationIsPinnedThroughMcpAndRefinementKeepsTuition() {
    h.model.add("search_courses", Map.of("maxTuition", 5000000, "programCode", "TOEIC"));
    var first = h.ask("Khóa IELTS cho học sinh THCS dưới 5 triệu?");
    assertEquals("ANSWER", first.status());
    assertEquals("IELTS", h.backend.calls.getLast().get("programCode"));
    assertEquals("SECONDARY", h.backend.calls.getLast().get("audienceCode"));
    h.model.add("search_courses", Map.of("audienceCode", "ADULT"));
    h.ask(h.request("Chỉ xem người lớn", first.conversationId(), UUID.randomUUID(), null));
    assertEquals("IELTS", h.backend.calls.getLast().get("programCode"));
    assertEquals("ADULT", h.backend.calls.getLast().get("audienceCode"));
    assertEquals("5000000", h.backend.calls.getLast().get("maxTuition"));
  }

  @Test void courseCountUsesCanonicalFilters() {
    h.model.add("get_center_statistics", Map.of("metric", "COURSES_COUNT"));
    var response = h.ask("Tổng số khóa học IELTS cho THCS đang mở?");
    assertEquals("ANSWER", response.status());
    assertEquals("COURSES_COUNT", h.backend.calls.getLast().get("metric"));
    assertEquals("IELTS", h.backend.calls.getLast().get("programCode"));
    assertEquals("SECONDARY", h.backend.calls.getLast().get("audienceCode"));
    assertEquals("OPEN", h.backend.calls.getLast().get("status"));
  }

  @Test void multipleAudiencesClarifyBeforeAnyModelOrDataQuery() {
    var response = h.ask("Có bao nhiêu khóa học IELTS cho THCS và người lớn?");
    assertEquals("CLARIFICATION_REQUIRED", response.status());
    assertEquals(0, h.model.count);
    assertTrue(h.backend.calls.isEmpty());
  }
}
