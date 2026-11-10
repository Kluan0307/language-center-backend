package com.example.auth.integration;

import com.example.auth.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchDirectory {
  private final RemoteApi api;

  @Value("${services.academic.url}")
  private String base;

  public void requireActive(Long id) {
    if (id == null || id <= 0) throw ApiException.bad("Invalid branchId");
    Branch branch = api.get(base + "/api/v1/academic/branches/" + id, Branch.class);
    if (!"ACTIVE".equals(branch.status())) throw ApiException.bad("Branch is inactive");
  }

  public void requireNoTeachingClasses(Long teacherId, java.util.List<Long> removed) {
    if (!teachingResponsibilities(teacherId, removed).isEmpty())
      throw ApiException.conflict(
          "Giáo viên còn lớp hoặc buổi chưa xử lý tại chi nhánh bị thu hồi");
  }

  public void requireNoActiveLeads(Long accountId, java.util.List<Long> removed) {
    for (var branch : removed) {
      var page =
          api.get(
              base
                  + "/api/v1/academic/leads?size=1&active=true&assignedTo="
                  + accountId
                  + "&branchId="
                  + branch,
              ClassPage.class);
      if (page.totalElements() > 0)
        throw ApiException.conflict(
            "Cần phân công lại Lead đang chăm sóc trước khi thu hồi chi nhánh");
    }
  }

  public record ClassPage(long totalElements) {}

  public java.util.List<com.example.auth.dto.TeacherProfileDto.Responsibility>
      teachingResponsibilities(Long teacherId, java.util.List<Long> branchIds) {
    var result = new java.util.ArrayList<com.example.auth.dto.TeacherProfileDto.Responsibility>();
    for (var branch :
        branchIds == null ? java.util.Collections.<Long>singletonList(null) : branchIds) {
      var rows =
          api.get(
              base
                  + "/api/v1/academic/teachers/"
                  + teacherId
                  + "/responsibilities"
                  + (branch == null ? "" : "?branchId=" + branch),
              com.example.auth.dto.TeacherProfileDto.Responsibility[].class);
      if (rows == null)
        throw new ApiException(
            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
            "Không kiểm tra được trách nhiệm giảng dạy");
      result.addAll(java.util.List.of(rows));
    }
    return result;
  }

  public record Branch(Long id, String status) {}
}
