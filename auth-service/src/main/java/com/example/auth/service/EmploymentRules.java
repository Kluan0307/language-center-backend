package com.example.auth.service;

import com.example.auth.common.*;
import java.time.LocalDate;
import java.util.Set;

public final class EmploymentRules {
  private EmploymentRules() {}
  public static void validate(String status, LocalDate hire, LocalDate end) {
    var today = LocalDate.now(BusinessTime.ZONE);
    if (status == null || !Set.of("ACTIVE", "INACTIVE").contains(status))
      throw ApiException.bad("Trạng thái làm việc không hợp lệ");
    if (hire != null && hire.isAfter(today)) throw ApiException.bad("Ngày vào làm không được ở tương lai");
    if ("ACTIVE".equals(status) && end != null) throw ApiException.bad("Nhân sự đang làm việc không có ngày kết thúc");
    if ("INACTIVE".equals(status) && (end == null || end.isAfter(today)))
      throw ApiException.bad("Nhập ngày kết thúc không vượt quá hôm nay");
    if (hire != null && end != null && end.isBefore(hire))
      throw ApiException.bad("Ngày kết thúc không được trước ngày vào làm");
  }
}
