package com.example.teaching.evaluation;

import com.example.teaching.exam.*;
import com.example.teaching.grade.Grade;
import java.math.*;
import java.time.*;
import java.util.*;

public final class EvaluationCalculator {
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int SCALE = 20;

  private static boolean includes(AcademicEvaluationContext.Learner e, LocalDate date) {
    return e.periods() != null && e.periods().stream().anyMatch(p -> p.includes(date));
  }

  private static void issue(List<EvaluationDto.Issue> list, String code, String text) {
    list.add(new EvaluationDto.Issue(code, text));
  }

  public static EvaluationDto.Report calculate(
      EvaluationPolicy policy,
      AcademicEvaluationContext context,
      List<Exam> exams,
      List<Grade> grades,
      List<ExamCandidate> candidates,
      LocalDate today) {
    Map<Long, Exam> byExam = new HashMap<>();
    exams.forEach(e -> byExam.put(e.getId(), e));
    Map<String, Grade> byGrade = new HashMap<>();
    grades.forEach(g -> byGrade.put(g.getExam().getId() + ":" + g.getStudentId(), g));
    Map<String, ExamCandidate> roster = new HashMap<>();
    candidates.forEach(c -> roster.put(c.getExamId() + ":" + c.getStudentId(), c));
    Set<Long> used = new HashSet<>();
    var rows = new ArrayList<EvaluationDto.Row>();
    var attendanceIndex = new AttendanceIndex(context);
    for (var learner : context.enrollments()) {
      var reasons = new ArrayList<EvaluationDto.Issue>();
      var results = new ArrayList<EvaluationDto.ExamResult>();
      int graded = 0, applicable = 0, missing = 0;
      BigDecimal sum = BigDecimal.ZERO, totalWeight = BigDecimal.ZERO;
      if (policy == null)
        issue(reasons, "POLICY_UNCONFIGURED", "Quản lý chưa xác nhận quy tắc của lớp");
      if (learner.periods() == null || learner.periods().isEmpty())
        issue(reasons, "MISSING_ENROLLMENT_PERIODS", "Chưa đủ căn cứ khoảng học của đăng ký");
      if (policy != null)
        for (var rule : policy.getRules()) {
          var exam = byExam.get(rule.getExamId());
          String applies = "NOT_APPLICABLE";
          if (exam == null) {
            applies = "UNKNOWN";
          } else if (exam.getRosterFrozenAt() != null) {
            var frozen = roster.get(exam.getId() + ":" + learner.studentId());
            if (frozen != null) {
              if (frozen.getEnrollmentId() != null)
                applies =
                    frozen.getEnrollmentId().equals(learner.enrollmentId())
                        ? "APPLICABLE"
                        : "NOT_APPLICABLE";
              else
                applies =
                    periodApplicability(learner, context.enrollments(), rule.getExamDate(), true);
            }
          } else
            applies =
                periodApplicability(learner, context.enrollments(), rule.getExamDate(), false);
          String gradeStatus = "NOT_APPLICABLE";
          BigDecimal score = null, percent = null;
          if (applies.equals("UNKNOWN")) {
            gradeStatus = "UNKNOWN_ROSTER";
            issue(
                reasons,
                "UNKNOWN_ROSTER",
                "Chưa gắn được danh sách dự thi với đăng ký: " + rule.getExamName());
          } else if (applies.equals("APPLICABLE")) {
            applicable++;
            var g = byGrade.get(rule.getExamId() + ":" + learner.studentId());
            if (g == null) {
              gradeStatus = rule.getExamDate().isAfter(today) ? "NOT_YET_DUE" : "MISSING";
              if (rule.isRequired()) {
                missing++;
                issue(
                    reasons,
                    gradeStatus.equals("NOT_YET_DUE")
                        ? "REQUIRED_EXAM_NOT_YET_DUE"
                        : "MISSING_REQUIRED_GRADE",
                    "Bài bắt buộc chưa có điểm: " + rule.getExamName());
              }
            } else {
              score = g.getScore();
              if (score == null
                  || score.signum() < 0
                  || rule.getMaxScore() == null
                  || rule.getMaxScore().signum() <= 0
                  || rule.getWeight() == null
                  || rule.getWeight().signum() <= 0
                  || score.compareTo(rule.getMaxScore()) > 0
                  || rule.getExamDate().isAfter(today)) {
                gradeStatus = "INVALID";
                issue(
                    reasons,
                    "INVALID_GRADE_SOURCE",
                    "Điểm/thang/ngày chưa hợp lệ: " + rule.getExamName());
                if (rule.isRequired()) missing++;
              } else {
                gradeStatus = "GRADED";
                used.add(g.getId());
                graded++;
                percent =
                    score.multiply(HUNDRED).divide(rule.getMaxScore(), SCALE, RoundingMode.HALF_UP);
                sum = sum.add(percent.multiply(rule.getWeight()));
                totalWeight = totalWeight.add(rule.getWeight());
              }
            }
          }
          results.add(
              new EvaluationDto.ExamResult(
                  rule.getExamId(),
                  rule.getExamName(),
                  rule.getExamDate(),
                  rule.isRequired(),
                  rule.getWeight(),
                  rule.getMaxScore(),
                  applies,
                  gradeStatus,
                  score,
                  percent == null ? null : percent.setScale(2, RoundingMode.HALF_UP)));
        }
      BigDecimal raw =
          totalWeight.signum() == 0 ? null : sum.divide(totalWeight, SCALE, RoundingMode.HALF_UP);
      if (policy != null && raw == null)
        issue(reasons, "NO_APPLICABLE_GRADE", "Cần ít nhất một bài áp dụng có điểm hợp lệ");
      var attendance =
          attendance(
              learner,
              context,
              policy == null ? null : policy.getMinAttendancePercent(),
              today,
              attendanceIndex);
      if (policy != null
          && policy.getMinAttendancePercent() != null
          && attendance.status().equals("UNKNOWN"))
        issue(
            reasons,
            "ATTENDANCE_UNKNOWN",
            "Chưa đủ nguồn chuyên cần: khoảng học/buổi/điểm danh hoặc mẫu số bằng0");
      String status =
          reasons.isEmpty() && raw != null
              ? (raw.compareTo(policy.getPassPercent()) >= 0
                      && (policy.getMinAttendancePercent() == null
                          || attendance.percent().compareTo(policy.getMinAttendancePercent()) >= 0)
                  ? "PASS"
                  : "FAIL")
              : "INSUFFICIENT";
      rows.add(
          new EvaluationDto.Row(
              learner.enrollmentId(),
              learner.studentId(),
              learner.studentName(),
              learner.studyStatus(),
              raw == null ? null : raw.setScale(2, RoundingMode.HALF_UP),
              raw,
              status.equals("INSUFFICIENT") ? null : status.equals("PASS"),
              status,
              graded,
              applicable,
              missing,
              attendance,
              List.copyOf(results),
              List.copyOf(reasons)));
    }
    long passed = rows.stream().filter(r -> r.status().equals("PASS")).count(),
        failed = rows.stream().filter(r -> r.status().equals("FAIL")).count(),
        assessable = passed + failed;
    var warnings = new ArrayList<EvaluationDto.Issue>();
    if (policy == null)
      issue(warnings, "POLICY_UNCONFIGURED", "Không dùng mặc định50% để kết luận");
    if (policy != null && grades.stream().anyMatch(g -> !used.contains(g.getId())))
      issue(
          warnings,
          "UNUSED_GRADES",
          "Có điểm ngoài quy tắc/khoảng học hoặc chưa đủ căn cứ gắn đăng ký; không tự cộng vào điểm"
              + " tạm");
    return new EvaluationDto.Report(
        context.classId(),
        policy == null ? 0 : policy.getRevision(),
        policy == null ? "UNCONFIGURED" : "CONFIRMED",
        List.copyOf(rows),
        assessable == 0
            ? null
            : BigDecimal.valueOf(passed * 100)
                .divide(BigDecimal.valueOf(assessable), 2, RoundingMode.HALF_UP),
        assessable,
        rows.size() - assessable,
        passed,
        failed,
        LocalDateTime.now(com.example.teaching.common.BusinessTime.ZONE),
        context.duration(),
        List.copyOf(warnings));
  }

  private static String periodApplicability(
      AcademicEvaluationContext.Learner learner,
      List<AcademicEvaluationContext.Learner> all,
      LocalDate date,
      boolean frozenMember) {
    if (learner.periods() == null || learner.periods().isEmpty()) return "UNKNOWN";
    var matches =
        all.stream()
            .filter(e -> e.studentId().equals(learner.studentId()) && includes(e, date))
            .toList();
    if (matches.size() > 1)
      return matches.stream().anyMatch(e -> e.enrollmentId().equals(learner.enrollmentId()))
          ? "UNKNOWN"
          : "NOT_APPLICABLE";
    if (matches.isEmpty()) return frozenMember ? "UNKNOWN" : "NOT_APPLICABLE";
    return matches.getFirst().enrollmentId().equals(learner.enrollmentId())
        ? "APPLICABLE"
        : "NOT_APPLICABLE";
  }

  private static EvaluationDto.Attendance attendance(
      AcademicEvaluationContext.Learner e,
      AcademicEvaluationContext c,
      BigDecimal min,
      LocalDate today,
      AttendanceIndex index) {
    if (min == null) return new EvaluationDto.Attendance("NOT_REQUIRED", null, 0, 0, 0, 0);
    if (e.periods() == null || e.periods().isEmpty())
      return new EvaluationDto.Attendance("UNKNOWN", null, 0, 0, 0, 0);
    long expected = 0, attended = 0, excused = 0, missing = 0;
    for (var s : c.sessions()) {
      if (!"COMPLETED".equals(s.status())) continue;
      boolean inPeriod = includes(e, s.sessionDate());
      boolean clock =
          s.startTime() != null
              && s.endTime() != null
              && s.endTime().isAfter(s.startTime())
              && s.startTime().getSecond() == 0
              && s.endTime().getSecond() == 0
              && s.startTime().getNano() == 0
              && s.endTime().getNano() == 0
              && !s.sessionDate().isAfter(today);
      var marks = new ArrayList<AcademicEvaluationContext.Mark>();
      marks.addAll(index.exact.getOrDefault(e.enrollmentId() + ":" + s.id(), List.of()));
      if (index.dates.getOrDefault(s.sessionDate(), 0L) == 1)
        marks.addAll(
            index.legacy.getOrDefault(e.enrollmentId() + ":" + s.sessionDate(), List.of()));
      marks.removeIf(m -> !Objects.equals(m.sessionDate(), s.sessionDate()));
      if (!inPeriod) {
        if (!marks.isEmpty()) {
          expected++;
          missing++;
        }
        continue;
      }
      expected++;
      if (!clock
          || marks.size() != 1
          || marks.getFirst().status() == null
          || !Set.of("PRESENT", "LATE", "ABSENT").contains(marks.getFirst().status())) {
        missing++;
        continue;
      }
      var mark = marks.getFirst();
      if (mark.status().equals("PRESENT") || mark.status().equals("LATE")) attended++;
      else if (mark.hasPermission()) excused++;
    }
    BigDecimal percent =
        expected == 0 || missing > 0
            ? null
            : BigDecimal.valueOf(attended * 100)
                .divide(BigDecimal.valueOf(expected), SCALE, RoundingMode.HALF_UP);
    return new EvaluationDto.Attendance(
        percent == null ? "UNKNOWN" : "KNOWN", percent, attended, expected, excused, missing);
  }

  private static class AttendanceIndex {
    final Map<String, List<AcademicEvaluationContext.Mark>> exact = new HashMap<>(),
        legacy = new HashMap<>();
    final Map<LocalDate, Long> dates = new HashMap<>();

    AttendanceIndex(AcademicEvaluationContext c) {
      for (var session : c.sessions()) dates.merge(session.sessionDate(), 1L, Long::sum);
      for (var mark : c.attendance()) {
        var target = mark.sessionId() == null ? legacy : exact;
        String key =
            mark.enrollmentId()
                + ":"
                + (mark.sessionId() == null ? mark.sessionDate() : mark.sessionId());
        target.computeIfAbsent(key, k -> new ArrayList<>()).add(mark);
      }
    }
  }
}
