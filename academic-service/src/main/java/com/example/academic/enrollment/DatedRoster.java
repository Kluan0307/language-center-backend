package com.example.academic.enrollment;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatedRoster {
  private final EnrollmentPeriodRepository periods;
  private final EnrollmentRepository enrollments;

  public record Student(Long id,String fullName,Long enrollmentId,String source){public Student(Long id,String name){this(id,name,null,"UNRESOLVED");}}

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public List<Student> students(Long classId, LocalDate date) {
    var eligible = new TreeMap<Long, Student>();
    var tracked = new HashSet<Long>();
    for (var p : periods.findByEnrollmentSchoolClassId(classId)) {
      var e = p.getEnrollment();
      tracked.add(e.getId());
      if (p.includes(date))
        eligible.merge(e.getStudent().getId(),new Student(e.getStudent().getId(),e.getStudent().getFullName(),e.getId(),"ENROLLMENT_PERIOD"),(a,b)->java.util.Objects.equals(a.enrollmentId(),b.enrollmentId())?a:new Student(a.id(),a.fullName(),null,"AMBIGUOUS"));
    }
    for (var e : enrollments.findBySchoolClassId(classId))
      if (!tracked.contains(e.getId())
          && e.getStudyStatus() == Enrollment.StudyStatus.STUDYING
          && !date.isBefore(e.getEnrolledAt().toLocalDate())
          && !date.isBefore(e.getSchoolClass().getStartDate()))
        eligible.put(
            e.getStudent().getId(),
            new Student(e.getStudent().getId(),e.getStudent().getFullName(),null,"LEGACY_FALLBACK"));
    return List.copyOf(eligible.values());
  }
}
