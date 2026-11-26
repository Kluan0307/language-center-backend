package com.example.academic.security;

import com.example.academic.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherDirectory {
  private final RemoteApi api;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  public void lock(Long profileId) {
    try {
      jdbc.queryForObject(
          "select a.id from accounts a join teachers t on t.account_id=a.id where t.id=? for"
              + " update",
          Long.class,
          profileId);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      throw ApiException.missing();
    }
  }

  @Value("${services.auth.url}")
  private String base;

  public Teacher require(Long id, Long branch) {
    Teacher teacher =
        api.get(base + "/api/v1/auth/teachers/" + id + "?branchId=" + branch, Teacher.class);
    if (teacher.branchIds() == null || !teacher.branchIds().contains(branch))
      throw ApiException.missing();
    return teacher;
  }

  public record Teacher(
      Long profileId,
      Long accountId,
      String fullName,
      Long branchId,
      java.util.List<Long> branchIds,
      java.time.LocalDate hireDate,
      java.time.LocalDate endDate) {
    public Teacher(Long profileId, Long accountId, String fullName, Long branchId, java.util.List<Long> branchIds) {
      this(profileId,accountId,fullName,branchId,branchIds,null,null);
    }
    public void requireDuring(java.time.LocalDate from,java.time.LocalDate to) {
      if(from==null||to==null||to.isBefore(from))throw ApiException.bad("Khoảng ngày giảng dạy không hợp lệ");
      if(hireDate!=null&&from.isBefore(hireDate)||endDate!=null&&to.isAfter(endDate))
        throw ApiException.conflict("Lịch giảng dạy nằm ngoài thời gian làm việc của giáo viên");
    }
  }
}
