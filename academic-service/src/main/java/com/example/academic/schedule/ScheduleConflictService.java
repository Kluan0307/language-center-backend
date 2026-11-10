package com.example.academic.schedule;

import com.example.academic.common.ApiException;
import com.example.academic.enrollment.*;
import com.example.academic.schoolclass.SchoolClass;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleConflictService {
  private final ScheduleRepository repository;
  private final EnrollmentRepository enrollments;

  public void validate(LocalTime start, LocalTime end) {
    if (com.example.academic.duration.DurationProgressService.minutes(start,end)==null) throw ApiException.bad("Giờ học phải cùng ngày và chính xác đến phút, kết thúc sau bắt đầu");
  }

  public static boolean datesOverlap(SchoolClass a, SchoolClass b) {
    return !a.getStartDate().isAfter(b.getEndDate()) && !a.getEndDate().isBefore(b.getStartDate());
  }

  public static boolean overlaps(Schedule a, Schedule b) {
    return a.getDayOfWeek() == b.getDayOfWeek()
        && a.getStartTime().isBefore(b.getEndTime())
        && a.getEndTime().isAfter(b.getStartTime());
  }

  public static boolean occursDuringOverlap(SchoolClass a, SchoolClass b, DayOfWeek day) {
    LocalDate first =
        a.getStartDate().isAfter(b.getStartDate()) ? a.getStartDate() : b.getStartDate();
    LocalDate last = a.getEndDate().isBefore(b.getEndDate()) ? a.getEndDate() : b.getEndDate();
    return !first.with(java.time.temporal.TemporalAdjusters.nextOrSame(day)).isAfter(last);
  }

  public List<ScheduleDto.Conflict> conflicts(
      SchoolClass candidate,
      DayOfWeek day,
      LocalTime start,
      LocalTime end,
      Long exclude,
      boolean ignoreOwnClass) {
    validate(start, end);
    List<ScheduleDto.Conflict> result = new ArrayList<>();
    for (Schedule s :
        repository.conflictCandidates(candidate.getBranchId(), candidate.getTeacherId(), day)) {
      SchoolClass other = s.getSchoolClass();
      if (Objects.equals(s.getId(), exclude)
          || ignoreOwnClass && Objects.equals(other.getId(), candidate.getId())) continue;
      if (!Objects.equals(other.getId(), candidate.getId())
          && other.effectiveStatus() == SchoolClass.Status.FINISHED) continue;
      if (Objects.equals(other.getId(), candidate.getId())) {
        result.add(
            new ScheduleDto.Conflict(
                s.getId(),
                other.getId(),
                other.getCode(),
                other.getName(),
                "Only one session per class/day is supported",
                day,
                s.getStartTime(),
                s.getEndTime()));
        continue;
      }
      if (!datesOverlap(candidate, other)
          || !start.isBefore(s.getEndTime())
          || !end.isAfter(s.getStartTime())) continue;
      LocalDate first =
          candidate.getStartDate().isAfter(other.getStartDate())
              ? candidate.getStartDate()
              : other.getStartDate();
      LocalDate last =
          candidate.getEndDate().isBefore(other.getEndDate())
              ? candidate.getEndDate()
              : other.getEndDate();
      if (first.with(java.time.temporal.TemporalAdjusters.nextOrSame(day)).isAfter(last)) continue;
      List<String> reasons = new ArrayList<>();
      if (Objects.equals(other.getId(), candidate.getId()))
        reasons.add("Trùng lịch trong cùng lớp");
      else {
        if (Objects.equals(other.getTeacherId(), candidate.getTeacherId()))
          reasons.add("Trùng giáo viên");
        if (Objects.equals(other.getRoom().getId(), candidate.getRoom().getId()))
          reasons.add("Trùng phòng");
        if (candidate.getId() != null) {
          Set<Long> students = new HashSet<>();
          for (Enrollment e :
              enrollments.findBySchoolClassIdAndStudyStatus(
                  candidate.getId(), Enrollment.StudyStatus.STUDYING))
            students.add(e.getStudent().getId());
          if (enrollments
              .findBySchoolClassIdAndStudyStatus(other.getId(), Enrollment.StudyStatus.STUDYING)
              .stream()
              .anyMatch(e -> students.contains(e.getStudent().getId())))
            reasons.add("Student timetable conflict");
        }
      }
      if (!reasons.isEmpty())
        result.add(
            new ScheduleDto.Conflict(
                s.getId(),
                other.getId(),
                Objects.equals(other.getBranchId(), candidate.getBranchId())
                    ? other.getCode()
                    : "OTHER_BRANCH",
                Objects.equals(other.getBranchId(), candidate.getBranchId())
                    ? other.getName()
                    : "Lịch tại chi nhánh khác",
                String.join(", ", reasons),
                day,
                s.getStartTime(),
                s.getEndTime()));
    }
    return result;
  }

  public void enforce(
      SchoolClass c,
      DayOfWeek day,
      LocalTime start,
      LocalTime end,
      Long exclude,
      boolean ignoreOwnClass) {
    var conflicts = conflicts(c, day, start, end, exclude, ignoreOwnClass);
    if (!conflicts.isEmpty())
      throw ApiException.conflict(
          conflicts.stream()
              .map(
                  x ->
                      x.reason()
                          + ": "
                          + x.classCode()
                          + " ("
                          + x.className()
                          + ") "
                          + x.dayOfWeek()
                          + " "
                          + x.startTime()
                          + "-"
                          + x.endTime())
              .distinct()
              .collect(java.util.stream.Collectors.joining("; ")));
  }

  public void recheck(SchoolClass c, List<Schedule> schedules) {
    for (int i = 0; i < schedules.size(); i++) {
      Schedule a = schedules.get(i);
      validate(a.getStartTime(), a.getEndTime());
      for (int j = i + 1; j < schedules.size(); j++)
        if (a.getDayOfWeek() == schedules.get(j).getDayOfWeek())
          throw ApiException.conflict("Only one session per class/day is supported");
      enforce(c, a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), null, true);
    }
  }
}
