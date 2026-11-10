package com.example.academic.attendance;

import com.example.academic.branch.BranchService;
import com.example.academic.common.*;
import com.example.academic.enrollment.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AttendanceService {
  private final AttendanceRepository repository;
  private final EnrollmentRepository enrollments;
  private final SchoolClassService classes;
  private final BranchService branches;
  private final EnrollmentPeriodRepository periods;
  private final com.example.academic.session.SessionPlanner sessions;
  private final AttendanceChangeRepository changes;

  private Map<Long, Enrollment> roster(Long classId, LocalDate date) {
    Map<Long, Enrollment> result = new LinkedHashMap<>();
    Set<Long> tracked = new HashSet<>();
    for (EnrollmentPeriod p : periods.findByEnrollmentSchoolClassId(classId)) {
      tracked.add(p.getEnrollment().getId());
      if (p.includes(date)) result.put(p.getEnrollment().getId(), p.getEnrollment());
    }
    for (Enrollment e : enrollments.findBySchoolClassId(classId))
      if (!tracked.contains(e.getId())
          && e.getStudyStatus() == Enrollment.StudyStatus.STUDYING
          && !date.isBefore(e.getEnrolledAt().toLocalDate())) result.put(e.getId(), e);
    for (Attendance a : repository.findByEnrollmentSchoolClassIdAndSessionDate(classId, date))
      result.put(a.getEnrollment().getId(), a.getEnrollment());
    return result;
  }

  private void date(LocalDate date) {
    if (date.isAfter(LocalDate.now(BusinessTime.ZONE)))
      throw ApiException.bad("Ngày điểm danh không được ở tương lai");
  }

  private boolean manager() {
    return Users.current().role() == Role.ADMIN || Users.current().role() == Role.SUPER_ADMIN;
  }

  @Transactional(readOnly = true)
  public AttendanceDto.Sheet sheet(Long classId, LocalDate date) {
    return sheet(classId, date, null);
  }

  @Transactional(readOnly = true)
  public AttendanceDto.Sheet sheet(Long classId, LocalDate date, Long sessionId) {
    var session = sessions.resolve(sessionId, classId, date, false);
    date(session.getSessionDate());
    if (session.getStatus() == com.example.academic.session.ClassSession.Status.CANCELLED)
      throw ApiException.conflict("Buổi đã hủy không được điểm danh");
    var c = session.getSchoolClass();
    var actual = session.getSessionDate();
    var saved =
        repository.findByEnrollmentSchoolClassIdAndSessionDate(c.getId(), actual).stream()
            .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));
    var rows =
        roster(c.getId(), actual).values().stream()
            .map(
                e -> {
                  var a = saved.get(e.getId());
                  return new AttendanceDto.Row(
                      e.getId(),
                      e.getStudent().getId(),
                      e.getStudent().getFullName(),
                      e.getStudent().getCurrentLevel(),
                      a == null ? Attendance.Status.PRESENT : a.getStatus(),
                      a != null && a.isHasPermission(),
                      a != null,
                      a == null ? null : a.getRecordedBy(),
                      a == null ? null : a.getRecordedByName());
                })
            .toList();
    boolean classHistory =
        Users.current().role() != Role.TEACHER
            || Objects.equals(Users.current().profileId(), c.getTeacherId());
    return new AttendanceDto.Sheet(
        c.getId(),
        c.getName(),
        actual,
        rows,
        session.getId(),
        session.getVersion(),
        session.getStatus(),
        session.getStartTime(),
        session.getEndTime(),
        session.getTeacherName(),
        sessions.canAttend(session),
        session.getStatus() == com.example.academic.session.ClassSession.Status.COMPLETED,
        classHistory);
  }

  public AttendanceDto.Sheet bulk(AttendanceDto.Bulk request) {
    var initial =
        sessions.resolve(request.sessionId(), request.classId(), request.sessionDate(), false);
    var session = sessions.lockSession(initial.getId());
    var c = session.getSchoolClass();
    date(session.getSessionDate());
    if (!request.sessionDate().equals(session.getSessionDate())
        || !request.classId().equals(c.getId()))
      throw ApiException.conflict("Buổi học đã đổi ngày/lớp. Hãy tải lại");
    if (session.getStatus() == com.example.academic.session.ClassSession.Status.CANCELLED)
      throw ApiException.conflict("Buổi đã hủy không được điểm danh");
    var active = roster(c.getId(), session.getSessionDate());
    if (active.isEmpty())
      throw ApiException.bad("Không có học viên có hiệu lực; rà soát ghi danh hoặc hủy buổi");
    Set<Long> ids = new HashSet<>();
    for (var item : request.records())
      if (!ids.add(item.enrollmentId())) throw ApiException.bad("enrollmentId bị lặp");
    if (!ids.equals(active.keySet()))
      throw ApiException.bad("Phải gửi đầy đủ danh sách học viên có hiệu lực tại ngày điểm danh");
    var saved =
        repository
            .findByEnrollmentSchoolClassIdAndSessionDate(c.getId(), session.getSessionDate())
            .stream()
            .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));
    boolean correction =
        session.getStatus() == com.example.academic.session.ClassSession.Status.COMPLETED;
    boolean changed =
        request.records().stream()
            .anyMatch(
                i -> {
                  var a = saved.get(i.enrollmentId());
                  return a == null
                      || a.getStatus() != i.status()
                      || a.isHasPermission()
                          != (i.status() == Attendance.Status.ABSENT
                              && Boolean.TRUE.equals(i.hasPermission()));
                });
    if (correction && !changed) return sheet(c.getId(), session.getSessionDate(), session.getId());
    if (!sessions.canAttend(session)) throw ApiException.forbidden();
    if ((request.version() != null && request.version() != session.getVersion())
        || (correction && request.version() == null))
      throw ApiException.conflict("Buổi học đã thay đổi. Hãy tải lại trước khi lưu");
    String reason = request.reason() == null ? null : request.reason().trim();
    if (correction
        && (!manager() || reason == null || reason.length() < 3 || reason.length() > 500))
      throw ApiException.bad("Admin phải nhập lý do sửa điểm danh từ 3 đến 500 ký tự");
    var user = Users.current();
    for (var item : request.records()) {
      var a = saved.get(item.enrollmentId());
      boolean permission =
          item.status() == Attendance.Status.ABSENT && Boolean.TRUE.equals(item.hasPermission());
      if (a != null && a.getStatus() == item.status() && a.isHasPermission() == permission) {
        if (a.getClassSession() == null) a.setClassSession(session);
        continue;
      }
      if (correction) {
        var change = new AttendanceChange();
        change.setSession(session);
        change.setEnrollmentId(item.enrollmentId());
        change.setStudentName(active.get(item.enrollmentId()).getStudent().getFullName());
        change.setOldStatus(a == null ? null : a.getStatus());
        change.setNewStatus(item.status());
        change.setOldPermission(a == null ? null : a.isHasPermission());
        change.setNewPermission(permission);
        change.setPreviousRecordedBy(a == null ? null : a.getRecordedBy());
        change.setPreviousRecordedByName(a == null ? null : a.getRecordedByName());
        change.setChangedBy(user.accountId());
        change.setChangedByName(user.username());
        change.setChangedAt(LocalDateTime.now(BusinessTime.ZONE));
        change.setReason(reason);
        changes.save(change);
      }
      if (a == null) a = new Attendance();
      a.setEnrollment(active.get(item.enrollmentId()));
      a.setClassSession(session);
      a.setSessionDate(session.getSessionDate());
      a.setStatus(item.status());
      a.setHasPermission(permission);
      a.setRecordedBy(user.accountId());
      a.setRecordedByName(user.username());
      repository.save(a);
    }
    repository.flush();
    if (correction) {
      session.setUpdatedAt(LocalDateTime.now(BusinessTime.ZONE));
      sessions.record(session, "ATTENDANCE_CORRECTED", null, reason);
      sessions.touch(session);
    } else sessions.complete(session);
    return sheet(c.getId(), session.getSessionDate(), session.getId());
  }

  @Transactional(readOnly = true)
  public List<AttendanceDto.Change> changes(Long sessionId) {
    if (!manager()) throw ApiException.forbidden();
    sessions.require(sessionId, false);
    return changes.findBySessionIdOrderByIdDesc(sessionId).stream()
        .map(AttendanceDto.Change::of)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AttendanceDto.History> history(Long classId, LocalDate from, LocalDate to) {
    classes.attendance(classId, false);
    if (from != null && to != null && from.isAfter(to))
      throw ApiException.bad("from không được sau to");
    Map<LocalDate, long[]> counts = new TreeMap<>();
    for (Attendance a : repository.findByEnrollmentSchoolClassIdOrderBySessionDateAsc(classId)) {
      if (from != null && a.getSessionDate().isBefore(from)
          || to != null && a.getSessionDate().isAfter(to)) continue;
      counts.computeIfAbsent(a.getSessionDate(), d -> new long[3])[a.getStatus().ordinal()]++;
    }
    return counts.entrySet().stream()
        .map(
            e ->
                new AttendanceDto.History(
                    e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AttendanceDto.Summary> summary(Long classId) {
    classes.attendance(classId, false);
    Map<Long, long[]> counts = new HashMap<>();
    for (Attendance a : repository.findByEnrollmentSchoolClassIdOrderBySessionDateAsc(classId))
      counts
          .computeIfAbsent(a.getEnrollment().getId(), id -> new long[3])[a.getStatus().ordinal()]++;
    return enrollments.findBySchoolClassId(classId).stream()
        .map(
            e -> {
              long[] n = counts.getOrDefault(e.getId(), new long[3]);
              long total = n[0] + n[1] + n[2];
              BigDecimal rate =
                  total == 0
                      ? BigDecimal.ZERO
                      : BigDecimal.valueOf(n[0] + n[2])
                          .multiply(BigDecimal.valueOf(100))
                          .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
              return new AttendanceDto.Summary(
                  e.getId(),
                  e.getStudent().getId(),
                  e.getStudent().getFullName(),
                  n[0],
                  n[1],
                  n[2],
                  total,
                  rate);
            })
        .toList();
  }
}
