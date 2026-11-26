package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

class PersonnelChatTest {
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
  void teacherCountKeepsZeroAndExplicitStatus() {
    h.model.add("get_personnel_statistics", Map.of());
    var r = h.ask("Có bao nhiêu giáo viên tại chi nhánh 1?");
    assertEquals("ANSWER", r.status());
    assertTrue(r.message().contains("0"));
    assertEquals("false", h.backend.calls.getFirst().get("activeOnly"));
  }

  @Test
  void managerCanCountStaff() {
    h.model.add("get_personnel_statistics", Map.of());
    var r = h.ask("Tổng số nhân viên của trung tâm?");
    assertEquals("ANSWER", r.status());
    assertEquals("STAFF", h.backend.calls.getFirst().get("kind"));
  }

  @Test
  void qualificationRequiresResolvingDuplicateNameAndRevalidatingChoice() {
    h.model.add("find_personnel", Map.of("keyword", "An", "page", 1));
    var first = h.ask("Giáo viên An có bằng cấp gì?");
    assertEquals("CLARIFICATION_REQUIRED", first.status());
    assertEquals(ChatModels.EntityType.PERSONNEL, first.choices().getFirst().type());
    h.model.add("get_personnel_profile", Map.of("personnelId", 12));
    var r =
        h.ask(
            h.request(
                "Tôi chọn người thứ hai",
                first.conversationId(),
                UUID.randomUUID(),
                new ChatModels.Selection(ChatModels.EntityType.PERSONNEL, 12L)));
    assertEquals("ANSWER", r.status());
    assertTrue(r.cards().toString().contains("Master"));
    assertFalse(r.cards().toString().contains("PRIVATE"));
    assertEquals("12", h.backend.calls.get(1).get("personnelId"));
    assertFalse(h.backend.calls.get(1).containsKey("keyword"));
    assertEquals("0", h.backend.calls.get(1).get("page"));
  }

  @Test
  void teacherCanReadSelfAndOwnTeaching() {
    h.backend.role = "TEACHER";
    h.model.add("get_my_personnel_profile", Map.of());
    assertEquals("ANSWER", h.ask("Bằng cấp của tôi?").status());
    h.model.add("get_my_personnel_profile", Map.of());
    h.model.add("get_teacher_classes", Map.of("teacherId", 11));
    assertEquals("ANSWER", h.ask("Tôi đang dạy khóa nào?").status());
  }

  @Test
  void staffCannotReadQualificationsOrStaffCounts() {
    h.backend.role = "STAFF";
    assertEquals(
        "FORBIDDEN",
        assertThrows(ChatFault.class, () -> h.ask("Giáo viên An có bằng cấp gì?")).code());
    assertEquals(
        "FORBIDDEN", assertThrows(ChatFault.class, () -> h.ask("Có bao nhiêu nhân viên?")).code());
    assertTrue(h.backend.calls.isEmpty());
  }

  @Test
  void teacherCannotReadOtherTeacher() {
    h.backend.role = "TEACHER";
    assertEquals(
        "FORBIDDEN",
        assertThrows(ChatFault.class, () -> h.ask("Giáo viên An có chứng chỉ gì?")).code());
  }

  @Test
  void actualTeachingUsesTeacherProfileId() {
    h.backend.duplicate = false;
    h.model.add("find_personnel", Map.of("keyword", "An"));
    h.model.add("get_teacher_classes", Map.of("teacherId", 11));
    var r = h.ask("Giáo viên An đang dạy các khóa nào?");
    assertEquals("ANSWER", r.status());
    assertTrue(r.cards().toString().contains("IELTS"));
  }

  @Test
  void selectedPersonnelKindCannotChangeOnFollowup() {
    h.backend.duplicate = false;
    h.model.add("find_personnel", Map.of("keyword", "An", "kind", "STAFF"));
    h.model.add("get_personnel_profile", Map.of("personnelId", 11, "kind", "TEACHER"));
    var initial = h.ask("Nhân viên An có vị trí gì?");
    assertEquals("STAFF", h.backend.calls.getLast().get("kind"));
    h.model.add("get_personnel_profile", Map.of("personnelId", 11, "kind", "TEACHER"));
    var result =
        h.ask(h.request("Hồ sơ người đó?", initial.conversationId(), UUID.randomUUID(), null));
    assertEquals("ANSWER", result.status());
    assertEquals("STAFF", h.backend.calls.getLast().get("kind"));
  }

  @Test
  void explicitActiveCountIsPinnedEvenWhenModelRequestsAll() {
    h.model.add("get_personnel_statistics", Map.of("activeOnly", false));
    h.ask("Có bao nhiêu giáo viên đang làm việc?");
    assertEquals("true", h.backend.calls.getFirst().get("activeOnly"));
  }

  @Test
  void historicalCountDoesNotPretendCurrentValueIsHistory() {
    assertEquals("UNSUPPORTED_CAPABILITY", h.ask("Tháng trước có bao nhiêu giáo viên?").status());
    assertTrue(h.backend.calls.isEmpty());
  }
}
