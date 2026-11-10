package com.example.academic.schoolclass;

import com.example.academic.common.Query;
import com.example.academic.security.*;
import com.example.academic.session.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic/teachers")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class TeacherResponsibilitiesController {
  private final SchoolClassRepository classes;
  private final ClassSessionRepository sessions;
  private final BranchScope scope;

  public record Responsibility(
      String kind,
      Long id,
      Long branchId,
      Long classId,
      String classCode,
      String label,
      LocalDate date,
      String status,
      Long version) {}

  @GetMapping("/{teacherId}/responsibilities")
  @Transactional(readOnly = true)
  public List<Responsibility> list(
      @PathVariable Long teacherId, @RequestParam(required = false) Long branchId) {
    if (branchId == null && Users.current().role() != Role.SUPER_ADMIN)
      throw com.example.academic.common.ApiException.bad("Chọn chi nhánh");
    if (branchId != null) {
      scope.check(branchId);
      if (branchId <= 0) throw com.example.academic.common.ApiException.bad("Invalid branchId");
    }
    if (teacherId <= 0) throw com.example.academic.common.ApiException.bad("Invalid teacherId");
    var out = new ArrayList<Responsibility>();
    for (var c :
        classes.findAll(
            Query.<SchoolClass>eq("branchId", branchId).and(Query.eq("teacherId", teacherId))))
      if (c.effectiveStatus() != SchoolClass.Status.FINISHED)
        out.add(
            new Responsibility(
                "CLASS",
                c.getId(),
                c.getBranchId(),
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getStartDate(),
                c.effectiveStatus().name(),
                null));
    for (var s :
        sessions.findAll(
            Query.<ClassSession>eq("schoolClass.branchId", branchId)
                .and(Query.eq("teacherId", teacherId))
                .and(Query.eq("status", ClassSession.Status.SCHEDULED)))) {
      var c = s.getSchoolClass();
      out.add(
          new Responsibility(
              "SESSION",
              s.getId(),
              c.getBranchId(),
              c.getId(),
              c.getCode(),
              c.getName(),
              s.getSessionDate(),
              s.getStatus().name(),
              s.getVersion()));
    }
    out.sort(Comparator.comparing(Responsibility::kind).thenComparing(Responsibility::id));
    return out;
  }
}
