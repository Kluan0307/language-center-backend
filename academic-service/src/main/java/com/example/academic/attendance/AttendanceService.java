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

  private void date(LocalDate date) {
    if (date.isAfter(LocalDate.now()))
      throw ApiException.bad("Ngày điểm danh không được ở tương lai");
  }

  @Transactional(readOnly = true)
  public AttendanceDto.Sheet sheet(Long classId, LocalDate date) {
    SchoolClass c = classes.attendance(classId, false);
    date(date);
    var saved =
        repository.findByEnrollmentSchoolClassIdAndSessionDate(classId, date).stream()
            .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));
    var rows =
        enrollments
            .findBySchoolClassIdAndStudyStatus(classId, Enrollment.StudyStatus.STUDYING)
            .stream()
            .map(
                e -> {
                  Attendance a = saved.get(e.getId());
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
    return new AttendanceDto.Sheet(classId, c.getName(), date, rows);
  }

  public AttendanceDto.Sheet bulk(AttendanceDto.Bulk request) {
    SchoolClass c = classes.attendance(request.classId(), true);
    date(request.sessionDate());
    branches.lock(c.getBranchId());
    classes.lock(c.getId());
    classes.attendance(c.getId(), true);
    var active =
        enrollments
            .findBySchoolClassIdAndStudyStatus(c.getId(), Enrollment.StudyStatus.STUDYING)
            .stream()
            .collect(Collectors.toMap(Enrollment::getId, Function.identity()));
    Set<Long> ids = new HashSet<>();
    for (var item : request.records())
      if (!ids.add(item.enrollmentId())) throw ApiException.bad("enrollmentId bị lặp");
    if (!ids.equals(active.keySet()))
      throw ApiException.bad("Phải gửi đầy đủ và chỉ các đăng ký STUDYING của lớp");
    var saved =
        repository
            .findByEnrollmentSchoolClassIdAndSessionDate(c.getId(), request.sessionDate())
            .stream()
            .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));
    var user = Users.current();
    for (var item : request.records()) {
      Attendance a = saved.getOrDefault(item.enrollmentId(), new Attendance());
      a.setEnrollment(active.get(item.enrollmentId()));
      a.setSessionDate(request.sessionDate());
      a.setStatus(item.status());
      a.setHasPermission(
          item.status() == Attendance.Status.ABSENT && Boolean.TRUE.equals(item.hasPermission()));
      a.setRecordedBy(user.accountId());
      a.setRecordedByName(user.username());
      repository.save(a);
    }
    repository.flush();
    return sheet(c.getId(), request.sessionDate());
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
    return enrollments
        .findBySchoolClassIdAndStudyStatus(classId, Enrollment.StudyStatus.STUDYING)
        .stream()
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
