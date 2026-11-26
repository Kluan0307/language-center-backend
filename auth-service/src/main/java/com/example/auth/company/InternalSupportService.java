package com.example.auth.company;

import com.example.auth.common.*;
import com.example.auth.entity.*;
import com.example.auth.integration.BranchDirectory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalSupportService {
  private final EntityManager em;
  private final CompanyService company;
  private final BranchDirectory branches;

  public record Create(
      @NotBlank @Size(max = 30) String category,
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 3000) String description,
      Long branchId) {}

  public record Reply(
      @NotNull Long version, @NotBlank @Size(max = 2000) String message, String status) {}

  private LocalDateTime now() {
    return LocalDateTime.now(BusinessTime.ZONE);
  }

  private boolean manager(Account a, InternalTicket t) {
    return a.getRole() == Role.SUPER_ADMIN
        || a.getRole() == Role.ADMIN
            && t.getBranchId() != null
            && a.activeBranchIds().contains(t.getBranchId());
  }

  private InternalTicket require(Long id, Account a, boolean lock) {
    var t =
        lock
            ? em.find(InternalTicket.class, id, LockModeType.PESSIMISTIC_WRITE)
            : em.find(InternalTicket.class, id);
    if (t == null || !a.getId().equals(t.getOwnerId()) && !manager(a, t))
      throw ApiException.missing();
    return t;
  }

  public InternalTicket create(Create r, String username) {
    var a = company.actor(username);
    if (!Set.of("ACCOUNT", "USAGE", "BUG", "FEEDBACK").contains(r.category()))
      throw ApiException.bad("Nhóm yêu cầu không hợp lệ");
    if (r.branchId() != null) {
      if (a.getRole() != Role.SUPER_ADMIN && !a.activeBranchIds().contains(r.branchId()))
        throw ApiException.forbidden();
      branches.requireActive(r.branchId());
    }
    var t = new InternalTicket();
    t.setOwnerId(a.getId());
    t.setBranchId(r.branchId());
    t.setCategory(r.category());
    t.setTitle(r.title().trim());
    t.setDescription(r.description().trim());
    t.setCreatedAt(now());
    t.setUpdatedAt(now());
    em.persist(t);
    em.flush();
    return t;
  }

  @Transactional(readOnly = true)
  public Object detail(Long id, String username) {
    var a = company.actor(username);
    var t = require(id, a, false);
    return Map.of(
        "ticket",
        t,
        "canManage",
        manager(a, t),
        "replies",
        em.createQuery("from InternalReply where ticketId=:id order by id", InternalReply.class)
            .setParameter("id", id)
            .getResultList());
  }

  @Transactional(readOnly = true)
  public PageResponse<InternalTicket> list(
      String username, boolean queue, String status, int page, int size) {
    var a = company.actor(username);
    if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
      throw ApiException.bad("Phân trang không hợp lệ");
    String where = "t.ownerId=:owner";
    var p = new HashMap<String, Object>();
    p.put("owner", a.getId());
    if (queue) {
      p.clear();
      if (a.getRole() == Role.SUPER_ADMIN) where = "1=1";
      else if (a.getRole() == Role.ADMIN) {
        if (a.activeBranchIds().isEmpty()) where = "1=0";
        else {
          where = "t.branchId in :branches";
          p.put("branches", a.activeBranchIds());
        }
      } else throw ApiException.forbidden();
    }
    if (status != null && !status.isBlank()) {
      if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED").contains(status))
        throw ApiException.bad("Trạng thái không hợp lệ");
      where += " and t.status=:status";
      p.put("status", status);
    }
    var count = em.createQuery("select count(t) from InternalTicket t where " + where, Long.class);
    var q =
        em.createQuery(
            "select t from InternalTicket t where "
                + where
                + " order by t.updatedAt desc,t.id desc",
            InternalTicket.class);
    p.forEach(
        (k, v) -> {
          q.setParameter(k, v);
          count.setParameter(k, v);
        });
    long n = count.getSingleResult();
    return new PageResponse<>(
        q.setFirstResult(page * size).setMaxResults(size).getResultList(),
        page,
        size,
        n,
        (int) ((n + size - 1) / size));
  }

  public Object reply(Long id, Reply r, String username) {
    var a = company.actor(username);
    var t = require(id, a, true);
    if (r.version() == null || r.version() != t.getVersion())
      throw ApiException.conflict("Yêu cầu đã thay đổi; hãy tải lại");
    String next = r.status() == null ? t.getStatus() : r.status();
    if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED").contains(next))
      throw ApiException.bad("Trạng thái không hợp lệ");
    if (!next.equals(t.getStatus()) && !manager(a, t)) throw ApiException.forbidden();
    if (t.getStatus().equals("RESOLVED") && !manager(a, t))
      throw ApiException.conflict("Yêu cầu đã giải quyết; hãy tạo yêu cầu mới");
    var reply = new InternalReply();
    reply.setTicketId(id);
    reply.setActorId(a.getId());
    reply.setMessage(r.message().trim());
    reply.setStatusAfter(next);
    reply.setCreatedAt(now());
    em.persist(reply);
    t.setStatus(next);
    t.setUpdatedAt(now());
    em.flush();
    return detail(id, username);
  }

  @Transactional(readOnly = true)
  public Object guides(String username) {
    var a = company.actor(username);
    var rows = new ArrayList<Map<String, String>>();
    rows.add(
        Map.of(
            "id",
            "account",
            "title",
            "Tài khoản và chi nhánh",
            "body",
            "Đổi mật khẩu tại Hồ sơ cá nhân. Chọn chi nhánh ở đầu sidebar; chỉ các chi nhánh được"
                + " phân công mới có thể truy cập."));
    rows.add(
        Map.of(
            "id",
            "assistant",
            "title",
            "Tra cứu bằng trợ lý",
            "body",
            "Hỏi tên hoặc mã học viên/giáo viên. Nếu trùng tên hãy chọn đúng người. Trợ lý chỉ trả"
                + " dữ liệu trong quyền của tài khoản."));
    rows.add(
        Map.of(
            "id",
            "schedule",
            "title",
            "Lịch học và lịch dạy",
            "body",
            a.getRole() == Role.TEACHER
                ? "Mở Lịch dạy của tôi để xem tuần học; chọn buổi để xem lớp và điểm danh."
                : "Mở Lịch học, chọn chi nhánh và tuần; kiểm tra giáo viên/phòng trước khi thay đổi"
                      + " lịch."));
    if (a.getRole() == Role.TEACHER)
      rows.add(
          Map.of(
              "id",
              "grades",
              "title",
              "Nhập và công bố điểm",
              "body",
              "Chọn lớp/bài kiểm tra được giao, nhập điểm rồi kiểm tra trước khi công bố. Điểm chưa"
                  + " công bố chưa hiển thị cho học viên."));
    else
      rows.add(
          Map.of(
              "id",
              "finance",
              "title",
              "Học phí và phiếu thu",
              "body",
              "Tra cứu học viên, kiểm tra khoản phải thu trước khi ghi nhận thanh toán. Đơn online"
                  + " đang chờ chưa phải phiếu thu. Sai sót đã ghi sổ cần xử lý bằng nghiệp vụ điều"
                  + " chỉnh."));
    return rows;
  }
}
