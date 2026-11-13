package com.example.academic.assistant;

import com.example.academic.common.*;
import com.example.academic.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Trusted query context. No identity, credentials or arbitrary URLs come from a model. */
@Component
public class ReadQuery {
  public enum Mode {
    ACTIVE_BRANCH,
    ALL_ASSIGNED,
    WHOLE_SYSTEM
  }

  public record Scope(Mode mode, List<Long> branchIds, String label) {
    public boolean contains(Long branch) {
      return mode == Mode.WHOLE_SYSTEM || branchIds.contains(branch);
    }
  }

  public record Result(
      String requestId,
      String status,
      Scope scope,
      OffsetDateTime observedAt,
      String source,
      Object data,
      List<String> warnings,
      Integer nextPage,
      boolean hasMore) {}

  public Scope scope(Mode mode, Long branchId) {
    var u = Users.current();
    if (!Set.of(Role.SUPER_ADMIN, Role.ADMIN, Role.STAFF, Role.TEACHER).contains(u.role()))
      throw ApiException.forbidden();
    if (mode == null) throw ApiException.bad("scopeMode bắt buộc");
    if (branchId != null && branchId <= 0) throw ApiException.bad("branchId phải dương");
    if (mode != Mode.ACTIVE_BRANCH && branchId != null)
      throw ApiException.bad("Không kết hợp branchId với phạm vi tổng hợp");
    if (mode == Mode.WHOLE_SYSTEM) {
      if (u.role() != Role.SUPER_ADMIN) throw ApiException.forbidden();
      return new Scope(mode, List.of(), "Toàn hệ thống");
    }
    if (mode == Mode.ALL_ASSIGNED) {
      if (u.role() != Role.ADMIN) throw ApiException.forbidden();
      return new Scope(
          mode,
          u.branchIds().stream().distinct().sorted().toList(),
          "Các chi nhánh được phân công");
    }
    Long selected = branchId == null ? u.branchId() : branchId;
    if (selected == null)
      throw ApiException.bad("Chọn chi nhánh hoặc yêu cầu WHOLE_SYSTEM rõ ràng");
    if (u.role() != Role.SUPER_ADMIN && !u.branchIds().contains(selected))
      throw ApiException.missing();
    return new Scope(mode, List.of(selected), "Chi nhánh " + selected);
  }

  public void page(int page, int size) {
    if (page < 0 || size < 1 || size > 20 || (long) page * size > Integer.MAX_VALUE)
      throw ApiException.bad("page >= 0, size từ 1 đến 20");
  }

  public void id(Long id) {
    if (id != null && id <= 0) throw ApiException.bad("ID phải dương");
  }

  public void keyword(String keyword) {
    if (keyword != null && keyword.length() > 120)
      throw ApiException.bad("Từ khóa tối đa 120 ký tự");
  }

  public String pattern(String text) {
    keyword(text);
    return "%"
        + text.trim()
            .toLowerCase(Locale.ROOT)
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
        + "%";
  }

  public Result result(Scope scope, String source, Object data) {
    Object records = data instanceof Map<?, ?> map ? map.get("records") : data;
    boolean empty = records instanceof PageResponse<?> p && p.totalElements() == 0;
    Integer next =
        records instanceof PageResponse<?> p && (long) p.page() + 1 < p.totalPages()
            ? p.page() + 1
            : null;
    return new Result(
        UUID.randomUUID().toString(),
        empty ? "NO_RESULTS" : "ANSWER",
        scope,
        OffsetDateTime.now(BusinessTime.ZONE),
        source,
        data,
        List.of(),
        next,
        next != null);
  }
}
