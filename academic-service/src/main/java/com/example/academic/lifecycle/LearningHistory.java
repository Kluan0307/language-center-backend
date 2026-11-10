package com.example.academic.lifecycle;

import com.example.academic.common.*;
import com.example.academic.enrollment.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningHistory {
  private final LearningEventRepository events;
  private final EnrollmentPeriodRepository periods;
  private final StudentRepository students;
  private final EnrollmentRepository enrollments;
  private final BranchScope scope;

  public static LocalDate today() {
    return LocalDate.now(BusinessTime.ZONE);
  }

  public static String reason(String value) {
    if (value == null || value.trim().length() < 3 || value.trim().length() > 500)
      throw ApiException.bad("Nhập lý do từ 3 đến 500 ký tự");
    return value.trim();
  }

  public void record(
      Student s,
      Enrollment e,
      Long related,
      String action,
      String old,
      String next,
      String reason,
      LocalDate effective,
      LocalDate expected) {
    var event = new LearningEvent();
    event.setBranchId(s.getBranchId());
    event.setStudentId(s.getId());
    event.setEnrollmentId(e == null ? null : e.getId());
    event.setRelatedEnrollmentId(related);
    event.setAction(action);
    event.setOldStatus(old);
    event.setNewStatus(next);
    event.setReason(reason(reason));
    event.setEffectiveDate(effective);
    event.setExpectedReturnDate(expected);
    event.setActorId(Users.current().accountId());
    event.setActorName(Users.current().username());
    events.save(event);
  }

  public record Event(
      Long id,
      Long enrollmentId,
      Long relatedEnrollmentId,
      String action,
      String oldStatus,
      String newStatus,
      String reason,
      LocalDate effectiveDate,
      LocalDate expectedReturnDate,
      String actorName,
      LocalDateTime occurredAt) {
    static Event of(LearningEvent e) {
      return new Event(
          e.getId(),
          e.getEnrollmentId(),
          e.getRelatedEnrollmentId(),
          e.getAction(),
          e.getOldStatus(),
          e.getNewStatus(),
          e.getReason(),
          e.getEffectiveDate(),
          e.getExpectedReturnDate(),
          e.getActorName(),
          e.getOccurredAt());
    }
  }

  public record Period(Long id, LocalDate activeFrom, LocalDate activeUntil) {}

  public record EnrollmentHistory(List<Period> periods, List<Event> events) {}

  @Transactional(readOnly = true)
  public List<Event> student(Long id) {
    var s = students.findById(id).orElseThrow(ApiException::missing);
    scope.check(s.getBranchId());
    return events.findByStudentIdOrderByIdDesc(id).stream().map(Event::of).toList();
  }

  @Transactional(readOnly = true)
  public EnrollmentHistory enrollment(Long id) {
    var e = enrollments.findById(id).orElseThrow(ApiException::missing);
    scope.check(e.getSchoolClass().getBranchId());
    return new EnrollmentHistory(
        periods.findByEnrollmentId(id).stream()
            .sorted(
                Comparator.comparing(EnrollmentPeriod::getActiveFrom)
                    .thenComparing(EnrollmentPeriod::getId))
            .map(p -> new Period(p.getId(), p.getActiveFrom(), p.getActiveUntil()))
            .toList(),
        events.findByEnrollmentIdOrderByIdDesc(id).stream().map(Event::of).toList());
  }
}
