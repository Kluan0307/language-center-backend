package com.example.academic.security;

import com.example.academic.common.ApiException;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class BranchScope {
  public Long filter(Long requested) {
    var u = Users.current();
    if (u.role() == Role.SUPER_ADMIN) return requested;
    if (u.branchId() == null) throw ApiException.forbidden();
    return u.branchId();
  }

  public Long create(Long requested) {
    var id = filter(requested);
    if (id == null) throw ApiException.bad("Phải chọn branchId");
    return id;
  }

  public void check(Long actual) {
    Long own = filter(null);
    if (own != null && !Objects.equals(own, actual)) throw ApiException.missing();
  }

  public void course(Long actual) {
    if (actual != null) check(actual);
    else filter(null);
  }

  public void teacher(Long teacherId, boolean attendanceWrite) {
    var u = Users.current();
    if (u.role() == Role.TEACHER && (u.profileId() == null || !u.profileId().equals(teacherId))) {
      if (attendanceWrite) throw ApiException.forbidden();
      throw ApiException.missing();
    }
  }
}
