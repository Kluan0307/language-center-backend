package com.example.academic.results;

import com.example.academic.attendance.AttendanceRepository;
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
public class LearningProfileService {
  private final StudentService students;
  private final EnrollmentRepository enrollments;
  private final EnrollmentPeriodRepository periods;
  private final AttendanceRepository attendance;

  public record Period(LocalDate activeFrom, LocalDate activeUntil) {}

  public record Study(
      EnrollmentDto enrollment,
      List<Period> periods,
      Map<String, Long> recordedAttendance,
      String resultVerification) {}

  public record Profile(StudentDto student, List<Study> studies, LocalDateTime observedAt) {}

  @Transactional(readOnly = true)
  public Profile get(Long id) {
    if (!Set.of(Role.SUPER_ADMIN, Role.ADMIN, Role.STAFF).contains(Users.current().role()))
      throw ApiException.forbidden();
    var student = students.require(id);
    Map<Long, List<Period>> byPeriod = new HashMap<>();
    for (var p : periods.findByEnrollmentStudentId(id))
      byPeriod
          .computeIfAbsent(p.getEnrollment().getId(), k -> new ArrayList<>())
          .add(new Period(p.getActiveFrom(), p.getActiveUntil()));
    Map<Long, Map<String, Long>> counts = new HashMap<>();
    for (var r : attendance.profileCounts(id))
      counts
          .computeIfAbsent(r.getEnrollmentId(), k -> new HashMap<>())
          .put(r.getStatus(), r.getTotal());
    var studies =
        enrollments.findByStudentId(id).stream()
            .filter(
                e ->
                    Users.current().role() == Role.SUPER_ADMIN
                        || Users.current().branchIds().contains(e.getSchoolClass().getBranchId()))
            .map(
                e ->
                    new Study(
                        EnrollmentDto.of(e),
                        List.copyOf(byPeriod.getOrDefault(e.getId(), List.of())),
                        Map.copyOf(counts.getOrDefault(e.getId(), Map.of())),
                        e.getCompletionResultId() != null
                            ? "LINKED"
                            : e.getStudyStatus() == Enrollment.StudyStatus.COMPLETED
                                ? "LEGACY_UNVERIFIED"
                                : "NOT_CONFIRMED"))
            .toList();
    return new Profile(StudentDto.of(student), studies, LocalDateTime.now(BusinessTime.ZONE));
  }
}
