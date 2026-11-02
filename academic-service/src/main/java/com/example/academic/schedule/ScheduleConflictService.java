package com.example.academic.schedule;

import com.example.academic.common.ApiException;
import com.example.academic.schoolclass.SchoolClass;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleConflictService {
  private final ScheduleRepository repository;

  public void validate(LocalTime start, LocalTime end) {
    if (!end.isAfter(start)) throw ApiException.bad("endTime phải sau startTime");
  }

  public static boolean datesOverlap(SchoolClass a, SchoolClass b) {
    return !a.getStartDate().isAfter(b.getEndDate()) && !a.getEndDate().isBefore(b.getStartDate());
  }

  public static boolean overlaps(Schedule a, Schedule b) {
    return a.getDayOfWeek() == b.getDayOfWeek()
        && a.getStartTime().isBefore(b.getEndTime())
        && a.getEndTime().isAfter(b.getStartTime());
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
        repository.findBySchoolClassBranchIdAndDayOfWeek(candidate.getBranchId(), day)) {
      SchoolClass other = s.getSchoolClass();
      if (Objects.equals(s.getId(), exclude)
          || ignoreOwnClass && Objects.equals(other.getId(), candidate.getId())) continue;
      if (!Objects.equals(other.getId(), candidate.getId())
          && other.effectiveStatus() == SchoolClass.Status.FINISHED) continue;
      if (!datesOverlap(candidate, other)
          || !start.isBefore(s.getEndTime())
          || !end.isAfter(s.getStartTime())) continue;
      List<String> reasons = new ArrayList<>();
      if (Objects.equals(other.getId(), candidate.getId()))
        reasons.add("Trùng lịch trong cùng lớp");
      else {
        if (Objects.equals(other.getTeacherId(), candidate.getTeacherId()))
          reasons.add("Trùng giáo viên");
        if (Objects.equals(other.getRoom().getId(), candidate.getRoom().getId()))
          reasons.add("Trùng phòng");
      }
      if (!reasons.isEmpty())
        result.add(
            new ScheduleDto.Conflict(
                s.getId(),
                other.getId(),
                other.getCode(),
                other.getName(),
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
        if (overlaps(a, schedules.get(j))) throw ApiException.conflict("Trùng lịch trong cùng lớp");
      enforce(c, a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), null, true);
    }
  }
}
