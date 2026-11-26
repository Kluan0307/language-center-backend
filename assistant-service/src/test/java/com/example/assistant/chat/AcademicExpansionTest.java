package com.example.assistant.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

class AcademicExpansionTest {
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
  void roomAvailabilityAsksForWindowBeforeModelOrApiCall() {
    assertEquals("CLARIFICATION_REQUIRED", h.ask("Phòng nào trống?").status());
    assertEquals(0, h.model.count);
    assertTrue(h.backend.calls.isEmpty());
  }

  @Test
  void specificRoomWindowUsesFixedTool() {
    h.model.add(
        "search_rooms", Map.of("from", "2026-11-15", "startTime", "18:00", "endTime", "20:00"));
    assertEquals(
        "ANSWER", h.ask("Phòng nào không có lịch ngày 2026-11-15 từ 18:00 đến 20:00?").status());
  }

  @Test
  void teacherScheduleResolvesTeacherNameBeforeSessionQuery() {
    h.backend.duplicate = false;
    h.model.add("find_personnel", Map.of("keyword", "An"));
    h.model.add("list_sessions", Map.of("teacherId", 11, "from", "2026-11-13", "to", "2026-11-19"));
    assertEquals("ANSWER", h.ask("Lịch tuần này của giáo viên An?").status());
    assertEquals("TEACHER", h.backend.calls.getFirst().get("kind"));
  }

  @Test
  void teacherSubstituteAndMakeupFiltersArePinned() {
    h.backend.role = "TEACHER";
    h.model.add(
        "list_sessions", Map.of("from", "2026-11-13", "to", "2026-11-19", "substituteOnly", false));
    assertEquals("ANSWER", h.ask("Buổi nào tôi đang dạy thay?").status());
    assertEquals("true", h.backend.calls.getLast().get("substituteOnly"));
    h.model.add("list_sessions", Map.of("from", "2026-11-13", "to", "2026-11-19"));
    h.ask("Tuần này có buổi học bù nào của tôi?");
    assertEquals("MAKEUP", h.backend.calls.getLast().get("kind"));
  }

  @Test
  void distinctRosterCountCanBeZeroAndTeacherCanUseIt() {
    h.backend.role = "TEACHER";
    h.model.add("get_roster_statistics", Map.of());
    var r = h.ask("Tổng số học viên distinct trong các lớp tôi phụ trách?");
    assertEquals("ANSWER", r.status());
    assertTrue(r.cards().toString().contains("value=0"));
  }

  @Test
  void absencesUseSeparateAggregateRatherThanAttendanceDefault() {
    h.model.add(
        "get_attendance_summary",
        Map.of("from", "2026-10-15", "to", "2026-11-13", "minAbsences", 3));
    var r = h.ask("Học viên nào vắng ít nhất 3 buổi đã ghi nhận trong 30 ngày?");
    assertEquals("ANSWER", r.status());
    assertTrue(r.cards().toString().contains("absentCount=3"));
  }

  @Test
  void missingAttendanceHasSeparateIntent() {
    h.model.add("list_missing_attendance", Map.of("from", "2026-10-15", "to", "2026-11-13"));
    assertEquals("ANSWER", h.ask("Buổi nào đã kết thúc nhưng còn thiếu điểm danh?").status());
  }

  @Test
  void staffCannotReadDraftExamProgress() {
    h.backend.role = "STAFF";
    assertEquals(
        "FORBIDDEN",
        assertThrows(ChatFault.class, () -> h.ask("Có bao nhiêu bài kiểm tra còn thiếu điểm?"))
            .code());
    assertTrue(h.backend.calls.isEmpty());
  }

  @Test
  void zeroScoreIsPinnedAndDifferentFromMissing() {
    h.backend.role = "TEACHER";
    h.model.add("get_learning_results", Map.of("view", "CURRENT", "examId", 501));
    h.ask("Ai có điểm 0 trong bài kiểm tra #501?");
    assertEquals("ZERO", h.backend.calls.getLast().get("scoreFilter"));
  }

  @Test
  void missingGradesNeverMeansLateWithoutDeadline() {
    assertEquals(
        "UNSUPPORTED_CAPABILITY",
        h.ask("Bài kiểm tra nào đã đến hạn nhưng còn học viên chưa được chấm?").status());
    assertTrue(h.backend.calls.isEmpty());
  }

  @Test
  void admissionInputLevelUsesPolicyTool() {
    h.backend.role = "STAFF";
    h.model.add("search_course_admissions", Map.of("entryLevel", "A2"));
    assertEquals("ANSWER", h.ask("Khóa có trình độ đầu vào A2 đang mở?").status());
  }

  @Test
  void duplicateExamNamesRequireVerifiedSelection() {
    h.model.add("list_learning_exams", Map.of("keyword", "Midterm"));
    var r = h.ask("Điểm bài kiểm tra Midterm?");
    assertEquals("CLARIFICATION_REQUIRED", r.status());
    assertEquals(ChatModels.EntityType.EXAM, r.choices().getFirst().type());
    h.model.add("get_learning_results", Map.of("view", "CURRENT", "examId", 502));
    var next = h.ask(h.request("Chọn bài thứ hai", r.conversationId(), UUID.randomUUID(), null));
    assertEquals("ANSWER", next.status());
    assertEquals("502", h.backend.calls.get(1).get("examId"));
  }

  @Test
  void financialGroupingNeverBecomesAcademicCount() {
    var policy = new TopicPolicy();
    assertEquals(
        TopicPolicy.Family.DEBT_STATS,
        policy.family("Tổng công nợ còn lại theo từng chi nhánh?", TopicPolicy.Family.NONE));
  }

  @Test
  void crossBranchRankingAsksToChooseAggregateScope() {
    assertEquals(
        "CLARIFICATION_REQUIRED",
        h.ask("Có bao nhiêu lớp đang diễn ra theo từng chi nhánh?").status());
    assertEquals(0, h.model.count);
    assertTrue(h.backend.calls.isEmpty());
  }
}
