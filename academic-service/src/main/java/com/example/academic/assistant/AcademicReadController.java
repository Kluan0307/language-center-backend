package com.example.academic.assistant;

import com.example.academic.course.Course;
import com.example.academic.session.ClassSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic/assistant-query")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','TEACHER')")
public class AcademicReadController {
  private final AcademicReadService service;
  private final ReadQuery query;

  @GetMapping("/courses")
  public ReadQuery.Result courses(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String level,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Course.Status status,
      @RequestParam(required = false) BigDecimal minTuition,
      @RequestParam(required = false) BigDecimal maxTuition,
      @RequestParam(defaultValue = "true") boolean maxInclusive,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.courses(
        scopeMode,
        branchId,
        keyword,
        level,
        type,
        status,
        minTuition,
        maxTuition,
        maxInclusive,
        page,
        size);
  }

  @GetMapping("/classes")
  public ReadQuery.Result classes(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long courseId,
      @RequestParam(required = false) com.example.academic.schoolclass.SchoolClass.Status status,
      @RequestParam(defaultValue = "false") boolean recruitingOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.classes(
        scopeMode, branchId, keyword, courseId, status, recruitingOnly, page, size);
  }

  @GetMapping("/students")
  public ReadQuery.Result students(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long classId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.students(scopeMode, branchId, keyword, classId, page, size);
  }

  @GetMapping("/student-classes")
  public ReadQuery.Result studentClasses(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam Long studentId,
      @RequestParam(required = false) LocalDate asOf,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.studentClasses(scopeMode, branchId, studentId, asOf, page, size);
  }

  @GetMapping("/student-statistics")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public ReadQuery.Result statistics(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "TOTAL_PROFILES") String metric,
      @RequestParam(required = false) LocalDate asOf) {
    return service.statistics(scopeMode, branchId, metric, asOf);
  }

  @GetMapping("/sessions")
  public ReadQuery.Result sessions(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) Long teacherId,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) ClassSession.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.sessions(scopeMode, branchId, classId, teacherId, from, to, status, page, size);
  }

  @GetMapping("/attendance")
  public ReadQuery.Result attendance(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam Long sessionId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return service.attendance(scopeMode, branchId, sessionId, page, size);
  }

  @GetMapping("/priority-leads")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
  public ReadQuery.Result leads(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "5") int limit) {
    return service.leads(scopeMode, branchId, limit);
  }

  // Reviewed static instructions only. No file retrieval, user supplied URLs or dynamic Markdown.
  @GetMapping("/usage-help")
  public ReadQuery.Result help(
      @RequestParam(defaultValue = "ACTIVE_BRANCH") ReadQuery.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam String topic) {
    var s = query.scope(scopeMode, branchId);
    var teacher =
        com.example.academic.security.Users.current().role()
            == com.example.academic.security.Role.TEACHER;
    Object help =
        switch (topic) {
          case "WEEKLY_CALENDAR" ->
              Map.of(
                  "topic",
                  topic,
                  "text",
                  "Mở lịch, chọn chế độ Tuần và ngày cần xem; chọn một buổi để xem chi tiết. Buổi"
                      + " hủy, học bù và dạy thay có nhãn riêng.",
                  "path",
                  teacher ? "/my-schedule" : "/schedule");
          case "FIND_COURSES" ->
              Map.of(
                  "topic",
                  topic,
                  "text",
                  "Mở Khóa học, tìm theo tên hoặc mã và kiểm tra trình độ, học phí cùng trạng thái"
                      + " khóa.",
                  "path",
                  "/courses");
          default ->
              throw com.example.academic.common.ApiException.bad(
                  "UNSUPPORTED_CAPABILITY: topic hướng dẫn chưa được hỗ trợ");
        };
    return query.result(s, "get_usage_help", help);
  }
}
