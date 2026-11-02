package com.example.academic.stats;

import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.lead.Lead;
import com.example.academic.schoolclass.SchoolClass;
import com.example.academic.security.*;
import com.example.academic.student.Student;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {
  private final StatsRepository repository;
  private final BranchScope scope;
  private final BranchRepository branches;

  public StatsDto.Response overview(Long requested, boolean group) {
    Long branch = scope.filter(requested);
    Branch selected =
        branch == null ? null : branches.findById(branch).orElseThrow(ApiException::missing);
    if (group && Users.current().role() != Role.SUPER_ADMIN) throw ApiException.forbidden();
    var overall = calculate(branch, selected == null ? null : selected.getName());
    List<StatsDto.Overview> grouped =
        group
            ? branches
                .findAll(
                    Query.<Branch>eq("id", branch), org.springframework.data.domain.Sort.by("id"))
                .stream()
                .map(b -> calculate(b.getId(), b.getName()))
                .toList()
            : List.of();
    return new StatsDto.Response(overall, grouped);
  }

  private BigDecimal percent(long numerator, long denominator) {
    return denominator == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }

  private <E extends Enum<E>> Map<String, Long> counters(E[] values) {
    Map<String, Long> result = new LinkedHashMap<>();
    for (E e : values) result.put(e.name(), 0L);
    return result;
  }

  private StatsDto.Overview calculate(Long branch, String name) {
    var students = repository.students(branch);
    var classes = repository.classes(branch);
    var leads = repository.leads(branch);
    Map<String, Long> studentStatus = counters(Student.Status.values()),
        levels = new TreeMap<>(),
        classStatus = counters(SchoolClass.Status.values()),
        leadStatus = counters(Lead.Status.values());
    LocalDate start = LocalDate.now().withDayOfMonth(1), end = start.plusMonths(1);
    long monthly = 0, occupied = 0, capacity = 0;
    for (Student s : students) {
      studentStatus.merge(s.getStatus().name(), 1L, Long::sum);
      levels.merge(
          s.getCurrentLevel() == null || s.getCurrentLevel().isBlank()
              ? "UNSPECIFIED"
              : s.getCurrentLevel(),
          1L,
          Long::sum);
      if (!s.getRegisteredAt().isBefore(start) && s.getRegisteredAt().isBefore(end)) monthly++;
    }
    for (SchoolClass c : classes) {
      classStatus.merge(c.effectiveStatus().name(), 1L, Long::sum);
      occupied += c.getCurrentSize();
      capacity += c.getMaxSize();
    }
    for (Lead l : leads) leadStatus.merge(l.getStatus().name(), 1L, Long::sum);
    return new StatsDto.Overview(
        branch,
        name,
        studentStatus,
        levels,
        monthly,
        classStatus,
        percent(occupied, capacity),
        leadStatus,
        percent(leadStatus.get("CONVERTED"), leads.size()));
  }
}
