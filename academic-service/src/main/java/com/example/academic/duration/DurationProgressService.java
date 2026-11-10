package com.example.academic.duration;

import com.example.academic.common.*;
import com.example.academic.schoolclass.*;
import com.example.academic.security.*;
import com.example.academic.session.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class DurationProgressService {
  private final SchoolClassRepository classes;
  private final ClassSessionRepository sessions;
  private final BranchScope scope;

  public static Long minutes(LocalTime start, LocalTime end) {
    if (start == null
        || end == null
        || !end.isAfter(start)
        || start.getSecond() != 0
        || end.getSecond() != 0
        || start.getNano() != 0
        || end.getNano() != 0) return null;
    return Duration.between(start, end).toMinutes();
  }

  private void access(SchoolClass c) {
    scope.check(c.getBranchId());
    scope.teacher(c.getTeacherId(), false);
  }

  public DurationDto.Progress get(Long id) {
    return batch(List.of(id)).getFirst();
  }

  public List<DurationDto.Progress> batch(List<Long> ids) {
    if (ids == null
        || ids.isEmpty()
        || ids.size() > 200
        || ids.stream().anyMatch(id -> id == null || id <= 0)
        || new HashSet<>(ids).size() != ids.size())
      throw ApiException.bad("Chọn 1 đến 200 mã lớp khác nhau");
    var all = classes.findAllById(ids);
    if (all.size() != ids.size()) throw ApiException.missing();
    all.forEach(this::access);
    var rows = sessions.durationRows(ids);
    Map<Long, List<SessionDurationRow>> groups = new HashMap<>();
    for (var r : rows) groups.computeIfAbsent(r.getClassId(), k -> new ArrayList<>()).add(r);
    Map<Long, DurationDto.Progress> result = new HashMap<>();
    for (var c : all)
      result.put(
          c.getId(),
          calculate(
              c, groups.getOrDefault(c.getId(), List.of()), LocalDate.now(BusinessTime.ZONE)));
    return ids.stream().map(result::get).toList();
  }

  public DurationDto.Progress calculate(
      SchoolClass c, List<SessionDurationRow> rows, LocalDate today) {
    Map<Long, SessionDurationRow> byId = new HashMap<>();
    Map<Long, Long> makeupCounts = new HashMap<>();
    for (var r : rows) {
      byId.put(r.getId(), r);
      if (r.getStatus() != ClassSession.Status.CANCELLED
          && r.getKind() == ClassSession.Kind.MAKEUP
          && r.getMakeupForId() != null) makeupCounts.merge(r.getMakeupForId(), 1L, Long::sum);
    }
    long known = 0,
        taught = 0,
        active = 0,
        completed = 0,
        cancelled = 0,
        unfinished = 0,
        overdue = 0,
        unknown = 0,
        unknownCompleted = 0,
        invalidMakeup = 0,
        futureCompleted = 0;
    for (var r : rows) {
      if (r.getStatus() == ClassSession.Status.CANCELLED) {
        cancelled++;
        continue;
      }
      active++;
      boolean done = r.getStatus() == ClassSession.Status.COMPLETED;
      if (done) completed++;
      else {
        unfinished++;
        if (r.getSessionDate().isBefore(today)) overdue++;
      }
      var m = minutes(r.getStartTime(), r.getEndTime());
      boolean bad = false;
      if (r.getKind() == ClassSession.Kind.MAKEUP) {
        var source = byId.get(r.getMakeupForId());
        bad =
            source == null
                || source.getStatus() != ClassSession.Status.CANCELLED
                || source.getKind() != ClassSession.Kind.REGULAR
                || !Objects.equals(source.getMakeupSessionId(), r.getId())
                || makeupCounts.getOrDefault(r.getMakeupForId(), 0L) != 1;
        if (bad) invalidMakeup++;
      }
      if (done && r.getSessionDate().isAfter(today)) {
        bad = true;
        futureCompleted++;
      }
      if (m == null || bad) {
        unknown++;
        if (done) unknownCompleted++;
        continue;
      }
      known = Math.addExact(known, m);
      if (done) taught = Math.addExact(taught, m);
    }
    Integer expected = c.getExpectedMinutes();
    Long scheduled = unknown == 0 ? known : null;
    Long taughtTotal = unknownCompleted == 0 ? taught : null;
    var warnings = new ArrayList<DurationDto.Warning>();
    if (expected == null) warn(warnings, "PLAN_UNVERIFIED", "Kế hoạch lớp chưa xác minh số phút");
    if (unknown > 0)
      warn(
          warnings,
          "UNKNOWN_SESSION_MINUTES",
          unknown + " buổi hoạt động chưa đủ căn cứ thời lượng");
    if (invalidMakeup > 0)
      warn(
          warnings,
          "INVALID_MAKEUP_LINK",
          "Liên kết học bù cần đối soát; không cộng thành thời lượng đã biết");
    if (futureCompleted > 0)
      warn(warnings, "FUTURE_COMPLETED", "Có buổi hoàn tất ở tương lai cần đối soát");
    Long difference = expected == null || scheduled == null ? null : scheduled - expected;
    Long remaining =
        expected == null || taughtTotal == null ? null : Math.max(0L, expected - taughtTotal);
    if (difference != null && difference < 0)
      warn(
          warnings,
          "SCHEDULED_SHORTFALL",
          "Lịch đã xếp thiếu " + (-difference) + " phút so với kế hoạch");
    if (difference != null && difference > 0)
      warn(
          warnings, "SCHEDULED_EXCESS", "Lịch đã xếp vượt " + difference + " phút so với kế hoạch");
    if (remaining != null && remaining > 0)
      warn(warnings, "TAUGHT_SHORTFALL", "Còn thiếu " + remaining + " phút đã học so với kế hoạch");
    if (unfinished > 0)
      warn(
          warnings,
          "UNFINISHED_SESSIONS",
          unfinished + " buổi chưa xác nhận hoàn tất hoặc xử lý nghỉ");
    if (overdue > 0) warn(warnings, "OVERDUE_SESSIONS", overdue + " buổi quá ngày vẫn chưa xử lý");
    boolean ready =
        expected != null
            && unknown == 0
            && taughtTotal != null
            && taughtTotal >= expected
            && unfinished == 0;
    return new DurationDto.Progress(
        c.getId(),
        c.getCode(),
        c.getDurationRevision(),
        expected,
        scheduled,
        taughtTotal,
        known,
        taught,
        active,
        completed,
        cancelled,
        unfinished,
        overdue,
        unknown,
        unknownCompleted,
        invalidMakeup,
        unknown > 0 ? "UNKNOWN" : "KNOWN",
        difference,
        remaining,
        ready,
        List.copyOf(warnings));
  }

  private void warn(List<DurationDto.Warning> list, String code, String message) {
    list.add(new DurationDto.Warning(code, message));
  }

  public void assertReadyToClose(SchoolClass c) {
    var p = get(c.getId());
    if (!p.timeReadyForClosure())
      throw ApiException.conflict(
          "Chưa đủ điều kiện thời lượng để đóng lớp: "
              + p.warnings().stream()
                  .map(DurationDto.Warning::message)
                  .reduce((a, b) -> a + "; " + b)
                  .orElse("Cần đối soát kế hoạch và buổi học"));
  }
}
