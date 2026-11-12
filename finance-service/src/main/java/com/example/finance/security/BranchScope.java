package com.example.finance.security;

import com.example.finance.common.ApiException;
import org.springframework.stereotype.Component;

@Component
public class BranchScope {
  public Long filter(Long requested) {
    var u = Users.current();
    if (u.role() == Role.STUDENT) throw ApiException.forbidden();
    if (requested != null && requested <= 0) throw ApiException.bad("Invalid branchId");
    if (u.role() == Role.SUPER_ADMIN) return requested;
    Long selected = requested == null ? u.branchId() : requested;
    if (selected == null || !u.branchIds().contains(selected)) throw ApiException.missing();
    return selected;
  }

  public Long create(Long requested) {
    var id = filter(requested);
    if (id == null) throw ApiException.bad("Phải chọn branchId");
    return id;
  }

  public void check(Long actual) {
    var u = Users.current();
    if (u.role() != Role.SUPER_ADMIN && !u.branchIds().contains(actual))
      throw ApiException.missing();
  }

  public void finance(Long actual) {
    if (actual != null) check(actual);
    else filter(null);
  }
}
