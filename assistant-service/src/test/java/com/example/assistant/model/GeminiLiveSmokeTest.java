package com.example.assistant.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.assistant.chat.*;
import com.example.assistant.mcp.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

/** Opt-in only. Uses synthetic Auth/C2 data; the sole external request is to Gemini. */
@EnabledIfEnvironmentVariable(named = "GEMINI_LIVE_SMOKE", matches = "true")
class GeminiLiveSmokeTest {
  @Test
  void vietnameseCourseQuestionUsesGeminiAndRealMcpWithSyntheticData() throws Exception {
    var json = JsonMapper.builder().build();
    String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-3.1-flash-lite");
    var evidence = new LinkedHashMap<String, Object>();
    evidence.put("model", model);
    evidence.put("data", "synthetic Auth/C2 only");
    try (var fixture = new ChatFixture()) {
      var backend =
          new BackendGateway(
              json, fixture.origin(), fixture.origin(), fixture.origin(), fixture.origin());
      var verifier = new SessionVerifier(backend);
      var runtime =
          new AssistantMcpRuntime(backend, verifier, new ToolCatalog(), json, true, 2, 120);
      var gemini =
          new GeminiGateway(
              json,
              Objects.requireNonNullElse(System.getenv("GEMINI_API_KEY"), ""),
              model,
              "https://generativelanguage.googleapis.com",
              false);
      org.springframework.test.util.ReflectionTestUtils.setField(gemini, "timeoutSeconds", 10);
      var service =
          new ChatService(
              runtime,
              gemini,
              new ConversationStore(),
              new TopicPolicy(),
              new ReplyFormatter(),
              json,
              true);
      try {
        var checks = new ArrayList<Map<String, Object>>();
        evidence.put("checks", checks);
        long started = System.nanoTime();
        var response =
            service.chat(
                verifier.current(ChatFixture.TOKEN),
                ChatFixture.TOKEN,
                1L,
                new ChatModels.Request(
                    "Thông tin khóa học giao tiếp dưới 5 triệu",
                    null,
                    UUID.randomUUID(),
                    null,
                    null),
                System.nanoTime());
        assertEquals("ANSWER", response.status());
        assertEquals("search_courses", response.sources().getLast().tool());
        assertEquals("5000000", fixture.calls.getLast().get("maxTuition"));
        assertEquals("false", fixture.calls.getLast().get("maxInclusive"));
        checks.add(
            Map.of(
                "case",
                "courses_under_5m",
                "status",
                "PASS",
                "elapsedMs",
                (System.nanoTime() - started) / 1_000_000));
        fixture.role = "TEACHER";
        started = System.nanoTime();
        var schedule = ask(service, verifier, "Tuần này tôi dạy những buổi nào?", null, null);
        assertEquals("ANSWER", schedule.status());
        assertEquals("list_sessions", schedule.sources().getLast().tool());
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate monday =
            today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertEquals(monday.toString(), fixture.calls.getLast().get("from"));
        assertEquals(monday.plusDays(6).toString(), fixture.calls.getLast().get("to"));
        checks.add(
            Map.of(
                "case",
                "teacher_weekly_schedule",
                "status",
                "PASS",
                "elapsedMs",
                (System.nanoTime() - started) / 1_000_000));
        fixture.role = "ADMIN";
        started = System.nanoTime();
        var stats = ask(service, verifier, "Tổng số học viên trong phạm vi đang xem?", null, null);
        assertEquals("ANSWER", stats.status());
        assertEquals("get_student_statistics", stats.sources().getLast().tool());
        assertTrue(stats.message().contains(": 0."));
        checks.add(
            Map.of(
                "case",
                "student_count_zero",
                "status",
                "PASS",
                "elapsedMs",
                (System.nanoTime() - started) / 1_000_000));
        fixture.role = "STAFF";
        started = System.nanoTime();
        var duplicate = ask(service, verifier, "Nguyễn Văn An đang học lớp nào?", null, null);
        assertEquals("CLARIFICATION_REQUIRED", duplicate.status());
        assertEquals(2, duplicate.choices().size());
        var picked = duplicate.choices().getLast();
        var classes =
            ask(
                service,
                verifier,
                "Chọn học viên thứ hai",
                duplicate.conversationId(),
                new ChatModels.Selection(picked.type(), picked.id()));
        assertEquals("ANSWER", classes.status());
        assertEquals("get_student_classes", classes.sources().getLast().tool());
        assertEquals("12", fixture.calls.getLast().get("studentId"));
        checks.add(
            Map.of(
                "case",
                "duplicate_student_then_classes",
                "status",
                "PASS",
                "elapsedMs",
                (System.nanoTime() - started) / 1_000_000));
        fixture.role = "ADMIN";
        var available = ask(service, verifier, "Lớp nào còn chỗ và sắp khai giảng?", null, null);
        assertEquals("ANSWER", available.status());
        assertEquals("search_classes", available.sources().getLast().tool());
        assertEquals("true", fixture.calls.getLast().get("recruitingOnly"));
        assertEquals("UPCOMING", fixture.calls.getLast().get("status"));
        checks.add(Map.of("case", "available_upcoming_classes", "status", "PASS"));
        fixture.role = "STAFF";
        var overdue = ask(service, verifier, "Những khoản nợ quá hạn?", null, null);
        assertEquals("ANSWER", overdue.status());
        assertEquals("search_tuitions", overdue.sources().getLast().tool());
        assertEquals("true", fixture.calls.getLast().get("overdue"));
        checks.add(Map.of("case", "overdue_tuitions", "status", "PASS"));
        var studentSchedule =
            ask(service, verifier, "Lịch tuần này của bạn ấy?", classes.conversationId(), null);
        assertEquals("ANSWER", studentSchedule.status());
        assertEquals("list_sessions", studentSchedule.sources().getLast().tool());
        assertEquals("9", fixture.calls.getLast().get("classId"));
        checks.add(Map.of("case", "student_schedule_followup", "status", "PASS"));
        fixture.role = "ADMIN";
        var cheaper = ask(service, verifier, "Còn dưới 3 triệu?", response.conversationId(), null);
        assertEquals("NO_RESULTS", cheaper.status());
        assertEquals("3000000", fixture.calls.getLast().get("maxTuition"));
        assertEquals("false", fixture.calls.getLast().get("maxInclusive"));
        checks.add(Map.of("case", "price_refinement_keeps_context", "status", "PASS"));
        evidence.put("checks", checks);
        evidence.put("status", "PASS");
        evidence.put("maxTuition", 5000000);
        evidence.put("maxInclusive", false);
        evidence.put("tool", "search_courses");
      } catch (ChatFault failure) {
        evidence.put("status", "BLOCKED");
        evidence.put("code", failure.code());
        throw failure;
      } finally {
        service.shutdown();
        runtime.shutdown();
        Files.createDirectories(Path.of("target"));
        Files.writeString(
            Path.of("target/c4-gemini-live.json"),
            json.writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
      }
    }
  }

  private ChatModels.Response ask(
      ChatService service,
      SessionVerifier verifier,
      String question,
      UUID conversation,
      ChatModels.Selection selection) {
    return service.chat(
        verifier.current(ChatFixture.TOKEN),
        ChatFixture.TOKEN,
        1L,
        new ChatModels.Request(question, conversation, UUID.randomUUID(), null, selection),
        System.nanoTime());
  }
}
