package com.example.auth.assistant;

import com.example.auth.common.*;
import com.example.auth.entity.*;
import java.security.Principal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
@RequestMapping("/api/v1/auth/assistant-query")
public class OrganizationController {
  private final PersonnelController personnel;
  private final NamedParameterJdbcTemplate jdbc;

  private String where(
      Account actor,
      PersonnelController.Scope s,
      String role,
      Boolean enabled,
      String keyword,
      Map<String, Object> p) {
    if (actor.getRole() != Role.SUPER_ADMIN && actor.getRole() != Role.ADMIN)
      throw ApiException.forbidden();
    if (role != null
        && !Set.of("SUPER_ADMIN", "ADMIN", "STAFF", "TEACHER", "STUDENT").contains(role))
      throw ApiException.bad("Invalid role");
    if (actor.getRole() == Role.ADMIN && role != null && !Set.of("STAFF", "TEACHER").contains(role))
      throw ApiException.forbidden();
    String w =
        s.mode() == PersonnelController.Mode.WHOLE_SYSTEM
            ? "1=1"
            : s.branchIds().isEmpty()
                ? "1=0"
                : "exists(select 1 from account_branches b where b.account_id=a.id and"
                    + " b.status=true and b.branch_id in (:branches))";
    p.put("branches", s.branchIds());
    if (actor.getRole() == Role.ADMIN) w += " and a.role in ('STAFF','TEACHER')";
    if (role != null) {
      p.put("role", role);
      w += " and a.role=:role";
    }
    if (enabled != null) {
      p.put("enabled", enabled);
      w += " and a.status=:enabled";
    }
    if (keyword != null && !keyword.isBlank()) {
      if (keyword.length() > 120) throw ApiException.bad("Keyword too long");
      p.put(
          "keyword",
          "%"
              + keyword
                  .toLowerCase(Locale.ROOT)
                  .replace("!", "!!")
                  .replace("%", "!%")
                  .replace("_", "!_")
              + "%");
      w +=
          " and (lower(a.username) like :keyword escape '!' or lower(a.personnel_code) like"
              + " :keyword escape '!' or lower(coalesce(ad.full_name,st.full_name,te.full_name,''))"
              + " like :keyword escape '!')";
    }
    return w;
  }

  private static final String FROM =
      "from accounts a left join admins ad on ad.account_id=a.id and a.role='ADMIN' left join"
          + " staffs st on st.account_id=a.id and a.role='STAFF' left join teachers te on"
          + " te.account_id=a.id and a.role='TEACHER'";

  @GetMapping("/management-accounts")
  public Object accounts(
      Principal principal,
      @RequestParam(defaultValue = "ACTIVE_BRANCH") PersonnelController.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) String accountRole,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long accountId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    if (page < 0 || size < 1 || size > 20 || (long) page * size > Integer.MAX_VALUE)
      throw ApiException.bad("Invalid page");
    var actor = personnel.actor(principal);
    var s = personnel.scope(actor, scopeMode, branchId);
    var p = new HashMap<String, Object>();
    String w = where(actor, s, accountRole, enabled, keyword, p);
    if (accountId != null) {
      if (accountId <= 0) throw ApiException.bad("Invalid accountId");
      p.put("account", accountId);
      w += " and a.id=:account";
    }
    long n = jdbc.queryForObject("select count(*) " + FROM + " where " + w, p, Long.class);
    p.put("limit", size);
    p.put("offset", page * size);
    var rows =
        jdbc.query(
            "select"
                + " a.id,a.username,a.role,a.status,coalesce(ad.full_name,st.full_name,te.full_name,a.username)"
                + " as full_name,a.personnel_code "
                + FROM
                + " where "
                + w
                + " order by a.id limit :limit offset :offset",
            p,
            (rs, i) -> {
              var row = new LinkedHashMap<String, Object>();
              row.put("accountId", rs.getLong(1));
              row.put("id", rs.getLong(1));
              row.put("code", rs.getString(6));
              row.put("username", rs.getString(2));
              row.put("accountRole", rs.getString(3));
              row.put("accountStatus", rs.getBoolean(4));
              row.put("fullName", rs.getString(5));
              return row;
            });
    for (var row : rows) {
      var args = new HashMap<String, Object>();
      args.put("account", row.get("accountId"));
      args.put("branches", s.branchIds());
      row.put(
          "branchIds",
          jdbc.queryForList(
              "select branch_id from account_branches where account_id=:account and status=true"
                  + (s.mode() == PersonnelController.Mode.WHOLE_SYSTEM
                      ? ""
                      : " and branch_id in (:branches)")
                  + " order by branch_id",
              args,
              Long.class));
    }
    return personnel.envelope(
        s,
        "find_management_accounts",
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

  @GetMapping("/account-statistics")
  public Object statistics(
      Principal principal,
      @RequestParam(defaultValue = "ACTIVE_BRANCH") PersonnelController.Mode scopeMode,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) String accountRole,
      @RequestParam(required = false) Boolean enabled) {
    var actor = personnel.actor(principal);
    var s = personnel.scope(actor, scopeMode, branchId);
    var p = new HashMap<String, Object>();
    String w = where(actor, s, accountRole, enabled, null, p);
    var rows =
        jdbc.query(
            "select a.role,a.status,count(distinct a.id) "
                + FROM
                + " where "
                + w
                + " group by a.role,a.status order by a.role,a.status",
            p,
            (rs, i) ->
                Map.of(
                    "accountRole",
                    rs.getString(1),
                    "accountStatus",
                    rs.getBoolean(2),
                    "value",
                    rs.getLong(3)));
    return personnel.envelope(
        s,
        "get_account_statistics",
        Map.of(
            "content",
            rows,
            "totalElements",
            rows.size(),
            "page",
            0,
            "size",
            20,
            "totalPages",
            rows.isEmpty() ? 0 : 1),
        rows.isEmpty(),
        null);
  }
}
