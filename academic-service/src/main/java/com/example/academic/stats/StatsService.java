package com.example.academic.stats;

import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.lead.Lead;
import com.example.academic.security.*;
import com.example.academic.student.Student;
import java.math.*;
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
    return overview(requested, group, false);
  }

  public StatsDto.Response overview(Long requested, boolean group, boolean allAssigned) {
    if (allAssigned && requested != null)
      throw ApiException.bad("Không kết hợp allAssigned và branchId");
    if (allAssigned && Users.current().role() != Role.SUPER_ADMIN) {
      if (Users.current().role() != Role.ADMIN) throw ApiException.forbidden();
      return assignedOverview(group);
    }
    Long branch = scope.filter(requested);
    Branch selected =
        branch == null ? null : branches.findById(branch).orElseThrow(ApiException::missing);

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

  private StatsDto.Response assignedOverview(boolean group) {
    var ids = Users.current().branchIds();
    var selected = branches.findAllById(ids);
    Map<String, Long> students = counters(Student.Status.values()),
        levels = new LinkedHashMap<>(),
        classes = new LinkedHashMap<>(),
        leads = counters(Lead.Status.values());
    long monthly = 0, occupied = 0, capacity = 0;
    List<StatsDto.Overview> rows = new ArrayList<>();
    for (var b : selected) {
      var r = calculate(b.getId(), b.getName());
      rows.add(r);
      merge(students, r.studentsByStatus());
      merge(levels, r.studentsByLevel());
      merge(classes, r.classesByStatus());
      merge(leads, r.leadsByStatus());
      monthly += r.newStudentsThisMonth();
      var occupancy = repository.occupancy(b.getId());
      occupied += occupancy[0];
      capacity += occupancy[1];
    }
    return new StatsDto.Response(
        new StatsDto.Overview(
            null,
            "Các chi nhánh được phân công",
            students,
            levels,
            monthly,
            classes,
            percent(occupied, capacity),
            leads,
            percent(
                leads.getOrDefault("CONVERTED", 0L),
                leads.values().stream().mapToLong(Long::longValue).sum())),
        group ? rows : List.of());
  }

  private void merge(Map<String, Long> target, Map<String, Long> source) {
    source.forEach((k, v) -> target.merge(k, v, Long::sum));
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
    Map<String, Long> studentStatus = counters(Student.Status.values());
    studentStatus.putAll(repository.studentsByStatus(branch));
    Map<String, Long> levels = repository.studentsByLevel(branch);
    Map<String, Long> classStatus = new LinkedHashMap<>();
    for (String status : List.of("UPCOMING", "ONGOING", "FINISHED")) classStatus.put(status, 0L);
    classStatus.putAll(repository.classesByStatus(branch));
    Map<String, Long> leadStatus = counters(Lead.Status.values());
    leadStatus.putAll(repository.leadsByStatus(branch));
    long monthly = repository.monthlyStudents(branch);
    long[] occupancy = repository.occupancy(branch);
    long occupied = occupancy[0], capacity = occupancy[1];
    return new StatsDto.Overview(
        branch,
        name,
        studentStatus,
        levels,
        monthly,
        classStatus,
        percent(occupied, capacity),
        leadStatus,
        percent(
            leadStatus.get("CONVERTED"),
            leadStatus.values().stream().mapToLong(Long::longValue).sum()));
  }
}
