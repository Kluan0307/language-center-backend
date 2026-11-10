package com.example.academic.lead;

import com.example.academic.common.*;
import com.example.academic.security.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeadAssigneeDirectory {
  private final RemoteApi api;
  private final BranchScope scope;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  @Value("${services.auth.url}")
  private String base;

  public record Account(
      Long accountId,
      String username,
      String fullName,
      String role,
      Boolean status,
      List<Long> branchIds) {}

  public record Page(List<Account> content, int totalPages) {}

  public record Assignee(Long accountId, String username, String fullName, String role) {}

  public List<Assignee> list(Long branch) {
    if (Users.current().role() != Role.ADMIN && Users.current().role() != Role.SUPER_ADMIN)
      throw ApiException.forbidden();
    scope.check(branch);
    var out = new ArrayList<Assignee>();
    int page = 0, total;
    do {
      var r =
          api.get(
              base + "/api/v1/auth/accounts?branchId=" + branch + "&size=200&page=" + page,
              Page.class);
      if (r.content() == null) throw ApiException.bad("Danh sách phân công không hợp lệ");
      for (var x : r.content())
        if (Boolean.TRUE.equals(x.status())
            && List.of("ADMIN", "STAFF").contains(x.role())
            && x.branchIds() != null
            && x.branchIds().contains(branch))
          out.add(new Assignee(x.accountId(), x.username(), x.fullName(), x.role()));
      total = r.totalPages();
      page++;
    } while (page < total);
    return out;
  }

  public Assignee require(Long id, Long branch) {
    try {
      jdbc.queryForObject("select id from accounts where id=? for update", Long.class, id);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      throw ApiException.missing();
    }
    return list(branch).stream()
        .filter(x -> x.accountId().equals(id))
        .findFirst()
        .orElseThrow(
            () -> ApiException.bad("Người phụ trách phải là Admin/Staff hoạt động tại chi nhánh"));
  }
}
