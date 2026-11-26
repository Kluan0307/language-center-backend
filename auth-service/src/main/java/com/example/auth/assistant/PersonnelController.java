package com.example.auth.assistant;

import com.example.auth.common.*;
import com.example.auth.entity.*;
import com.example.auth.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import java.security.Principal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Read-only projections. Personnel scope always comes from current account assignments. */
@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
@RequestMapping("/api/v1/auth/assistant-query")
public class PersonnelController {
  public enum Kind {
    TEACHER,
    STAFF
  }

  public enum Mode {
    ACTIVE_BRANCH,
    ALL_ASSIGNED,
    WHOLE_SYSTEM
  }

  private final AccountRepository accounts;
  private final EntityManager em;

  record Scope(Mode mode, List<Long> branchIds, String label) {}

  Account actor(Principal principal) {
    var a = accounts.findByUsername(principal.getName()).orElseThrow(ApiException::missing);
    if (!Boolean.TRUE.equals(a.getStatus())
        || a.isMustChangePassword()
        || a.getRole() == Role.STUDENT) throw ApiException.forbidden();
    return a;
  }

  Scope scope(Account a, Mode mode, Long branch) {
    if (branch != null && branch <= 0 || mode != Mode.ACTIVE_BRANCH && branch != null)
      throw ApiException.bad("Invalid scope");
    if (mode == Mode.WHOLE_SYSTEM) {
      if (a.getRole() != Role.SUPER_ADMIN) throw ApiException.forbidden();
      return new Scope(mode, List.of(), "Toàn hệ thống");
    }
    if (mode == Mode.ALL_ASSIGNED) {
      if (a.getRole() != Role.ADMIN) throw ApiException.forbidden();
      return new Scope(mode, a.activeBranchIds(), "Các chi nhánh được phân công");
    }
    Long selected = branch == null ? a.getBranchId() : branch;
    if (selected == null
        || a.getRole() != Role.SUPER_ADMIN && !a.activeBranchIds().contains(selected))
      throw ApiException.missing();
    return new Scope(mode, List.of(selected), "Chi nhánh " + selected);
  }

  private String where(Scope scope) {
    return scope.mode() == Mode.WHOLE_SYSTEM
        ? "1=1"
        : "exists(select b.id from AccountBranch b where b.account=p.account and b.status=true and"
            + " b.branchId in :branches)";
  }

  Map<String, Object> envelope(
      Scope scope, String source, Object data, boolean empty, Integer next) {
    var result = new LinkedHashMap<String, Object>();
    result.put("requestId", UUID.randomUUID().toString());
    result.put("status", empty ? "NO_RESULTS" : "ANSWER");
    result.put("scope", scope);
    result.put("observedAt", OffsetDateTime.now(BusinessTime.ZONE).toString());
    result.put("source", source);
    result.put("data", data);
    result.put("warnings", List.of());
    result.put("hasMore", next != null);
    result.put("nextPage", next);
    return result;
  }

  private List<Long> visibleBranches(Account target, Scope scope) {
    return target.activeBranchIds().stream()
        .filter(id -> scope.mode() == Mode.WHOLE_SYSTEM || scope.branchIds().contains(id))
        .toList();
  }

  private Map<String, Object> projection(
      Object entity, Account caller, Scope scope, boolean detail) {
    var row = new LinkedHashMap<String, Object>();
    Account account;
    long id;
    String name, work;
    if (entity instanceof Teacher t) {
      account = t.getAccount();
      id = t.getId();
      name = t.getFullName();
      work = t.getWorkStatus();
      if (detail) {
        row.put("qualification", t.getQualification());
        row.put("certificate", t.getCertificate());
        row.put("specialization", t.getSpecialization());
        row.put("experience", t.getExperience());
      }
    } else {
      var s = (Staff) entity;
      account = s.getAccount();
      id = s.getId();
      name = s.getFullName();
      work = s.getWorkStatus();
      if (detail) row.put("position", s.getPosition());
    }
    row.put("id", id);
    row.put("kind", entity instanceof Teacher ? "TEACHER" : "STAFF");
    row.put("code", account.getPersonnelCode());
    row.put("fullName", name);
    row.put("workStatus", work);
    if(detail){var profile=(EmploymentProfile)entity;row.put("hireDate",profile.getHireDate());row.put("endDate",profile.getEndDate());}
    row.put("accountStatus", account.getStatus());
    row.put("branchIds", visibleBranches(account, scope));
    return row;
  }

  private List<?> query(
      Account a,
      Scope s,
      Kind kind,
      String keyword,
      Long id,
      boolean activeOnly,
      int page,
      int size,
      boolean count,
      List<Long> total) {
    if (kind == Kind.STAFF && a.getRole() != Role.SUPER_ADMIN && a.getRole() != Role.ADMIN
        || a.getRole() == Role.TEACHER) throw ApiException.forbidden();
    if (a.getRole() == Role.STAFF && !activeOnly) throw ApiException.forbidden();
    if (page < 0
        || size < 1
        || size > 20
        || (long) page * size > Integer.MAX_VALUE
        || keyword != null && keyword.length() > 120
        || id != null && id <= 0) throw ApiException.bad("Invalid query");
    String entity = kind == Kind.TEACHER ? "Teacher" : "Staff";
    String condition =
        where(s)
            + " and p.account.role=:personnelRole"
            + (activeOnly ? " and p.account.status=true and p.workStatus='ACTIVE'" : "")
            + (id == null ? "" : " and p.id=:id")
            + (keyword == null || keyword.isBlank()
                ? ""
                : " and (lower(p.fullName) like :keyword escape '!' or"
                      + " lower(p.account.personnelCode) like :keyword escape '!')");
    var params = new HashMap<String, Object>();
    params.put("personnelRole", kind == Kind.TEACHER ? Role.TEACHER : Role.STAFF);
    if (s.mode() != Mode.WHOLE_SYSTEM) params.put("branches", s.branchIds());
    if (id != null) params.put("id", id);
    if (keyword != null && !keyword.isBlank())
      params.put(
          "keyword",
          "%"
              + keyword
                  .toLowerCase(Locale.ROOT)
                  .trim()
                  .replace("!", "!!")
                  .replace("%", "!%")
                  .replace("_", "!_")
              + "%");
    var q =
        em.createQuery(
            "select count(distinct p.id) from " + entity + " p where " + condition, Long.class);
    params.forEach(q::setParameter);
    total.add(q.getSingleResult());
    if (count) return List.of();
    var list =
        em.createQuery("select p from " + entity + " p where " + condition + " order by p.id");
    params.forEach(list::setParameter);
    return list.setFirstResult(page * size).setMaxResults(size).getResultList();
  }

  @GetMapping("/personnel")
  public Object search(
      Principal principal,
      @RequestParam(defaultValue = "ACTIVE_BRANCH") Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "TEACHER") Kind kind,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long personnelId,
      @RequestParam(defaultValue = "true") boolean activeOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    var a = actor(principal);
    var s = scope(a, scopeMode, branchId);
    var total = new ArrayList<Long>();
    var rows =
        query(a, s, kind, keyword, personnelId, activeOnly, page, size, false, total).stream()
            .map(p -> projection(p, a, s, false))
            .toList();
    long n = total.getFirst();
    return envelope(
        s,
        "find_personnel",
        Map.of(
            "content",
            rows,
            "page",
            page,
            "size",
            size,
            "totalElements",
            n,
            "totalPages",
            (n + size - 1) / size),
        n == 0,
        ((long) page + 1) * size < n ? page + 1 : null);
  }

  @GetMapping("/personnel-statistics")
  public Object statistics(
      Principal principal,
      @RequestParam(defaultValue = "ACTIVE_BRANCH") Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "TEACHER") Kind kind,
      @RequestParam(defaultValue = "true") boolean activeOnly) {
    var a = actor(principal);
    var s = scope(a, scopeMode, branchId);
    var total = new ArrayList<Long>();
    query(a, s, kind, null, null, activeOnly, 0, 1, true, total);
    return envelope(
        s,
        "get_personnel_statistics",
        Map.of(
            "kind",
            kind,
            "activeOnly",
            activeOnly,
            "metric",
            "DISTINCT_PERSONNEL",
            "value",
            total.getFirst()),
        false,
        null);
  }

  @GetMapping("/personnel-profile")
  public Object profile(
      Principal principal,
      @RequestParam(defaultValue = "ACTIVE_BRANCH") Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "TEACHER") Kind kind,
      @RequestParam Long personnelId) {
    var a = actor(principal);
    if (a.getRole() == Role.STAFF || a.getRole() == Role.TEACHER) throw ApiException.forbidden();
    var s = scope(a, scopeMode, branchId);
    var total = new ArrayList<Long>();
    var rows = query(a, s, kind, null, personnelId, false, 0, 1, false, total);
    if (rows.isEmpty()) throw ApiException.missing();
    return envelope(
        s, "get_personnel_profile", projection(rows.getFirst(), a, s, true), false, null);
  }

  @GetMapping("/my-personnel-profile")
  public Object mine(
      Principal principal,
      @RequestParam(defaultValue = "ACTIVE_BRANCH") Mode scopeMode,
      @RequestParam(required = false) Long branchId) {
    var a = actor(principal);
    if (a.getRole() != Role.TEACHER) throw ApiException.forbidden();
    var s = scope(a, scopeMode, branchId);
    var rows =
        em.createQuery("select t from Teacher t where t.account.id=:account", Teacher.class)
            .setParameter("account", a.getId())
            .getResultList();
    if (rows.isEmpty()) throw ApiException.missing();
    var data = projection(rows.getFirst(), a, s, true);
    data.put("branchIds", a.activeBranchIds());
    return envelope(s, "get_my_personnel_profile", data, false, null);
  }
}
