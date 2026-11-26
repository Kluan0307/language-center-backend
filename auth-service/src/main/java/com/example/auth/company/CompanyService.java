package com.example.auth.company;

import com.example.auth.common.*;
import com.example.auth.entity.*;
import com.example.auth.integration.BranchDirectory;
import com.example.auth.repository.AccountRepository;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {
  private final EntityManager em;
  private final AccountRepository accounts;
  private final BranchDirectory branches;
  private final NamedParameterJdbcTemplate jdbc;

  public Account actor(String username) {
    var a = accounts.findByUsername(username).orElseThrow(ApiException::missing);
    if (!Boolean.TRUE.equals(a.getStatus())
        || a.isMustChangePassword()
        || a.getRole() == Role.STUDENT) throw ApiException.forbidden();
    return a;
  }

  private Account editor(String username) {
    var a = actor(username);
    if (a.getRole() != Role.SUPER_ADMIN) throw ApiException.forbidden();
    return a;
  }

  private <T> T require(Class<T> type, Long id) {
    if (id == null) throw ApiException.bad("Thiếu định danh");
    var row = em.find(type, id);
    if (row == null) throw ApiException.missing();
    return row;
  }

  private void version(long actual, Long expected) {
    if (expected == null || actual != expected)
      throw ApiException.conflict("Dữ liệu đã thay đổi; hãy tải lại");
  }

  public record UnitRequest(
      @NotBlank @Size(max = 120) String name,
      Long parentId,
      Long branchId,
      boolean active,
      Long version) {}

  public record TitleRequest(
      @NotBlank @Size(max = 120) String name, boolean active, Long version) {}

  public record AssignmentRequest(
      @NotNull Long accountId,
      @NotNull Long unitId,
      @NotNull Long titleId,
      Long managerAccountId,
      boolean active,
      Long version) {}

  @Transactional(readOnly = true)
  public Map<String, Object> organization(String username) {
    actor(username);
    return Map.of(
        "units",
        em.createQuery("from OrganizationUnit order by id", OrganizationUnit.class).getResultList(),
        "titles",
        em.createQuery("from JobTitle order by id", JobTitle.class).getResultList(),
        "assignments",
        em.createQuery("from OrganizationAssignment order by id", OrganizationAssignment.class)
            .getResultList(),
        "branches",
        jdbc.queryForList(
            "select id,code,name,address,phone,status from branches order by code", Map.of()));
  }

  public OrganizationUnit unit(Long id, UnitRequest r, String username) {
    var a = editor(username);
    var row = id == null ? new OrganizationUnit() : require(OrganizationUnit.class, id);
    if (id != null) {
      em.lock(row, LockModeType.PESSIMISTIC_WRITE);
      version(row.getVersion(), r.version());
    }
    if (r.parentId() != null) {
      var seen = new HashSet<Long>();
      Long parent = r.parentId();
      while (parent != null) {
        if (Objects.equals(parent, id) || !seen.add(parent))
          throw ApiException.bad("Cơ cấu không được tạo vòng lặp");
        var p = require(OrganizationUnit.class, parent);
        if (!p.isActive() && r.active()) throw ApiException.bad("Đơn vị cha đã ngừng hoạt động");
        parent = p.getParentId();
      }
    }
    if (r.branchId() != null) branches.requireActive(r.branchId());
    if (!r.active()
        && id != null
        && (em.createQuery(
                        "select count(u) from OrganizationUnit u where u.parentId=:id and"
                            + " u.active=true",
                        Long.class)
                    .setParameter("id", id)
                    .getSingleResult()
                > 0
            || em.createQuery(
                        "select count(x) from OrganizationAssignment x where x.unitId=:id and"
                            + " x.active=true",
                        Long.class)
                    .setParameter("id", id)
                    .getSingleResult()
                > 0)) throw ApiException.conflict("Ngừng phân công và đơn vị con trước");
    row.setName(r.name().trim());
    row.setParentId(r.parentId());
    row.setBranchId(r.branchId());
    row.setActive(r.active());
    if (id == null) em.persist(row);
    audit(a, "ORG_UNIT_UPDATED");
    em.flush();
    return row;
  }

  public JobTitle title(Long id, TitleRequest r, String username) {
    var a = editor(username);
    var row = id == null ? new JobTitle() : require(JobTitle.class, id);
    if (id != null) {
      em.lock(row, LockModeType.PESSIMISTIC_WRITE);
      version(row.getVersion(), r.version());
    }
    if (!r.active()
        && id != null
        && em.createQuery(
                    "select count(x) from OrganizationAssignment x where x.titleId=:id and"
                        + " x.active=true",
                    Long.class)
                .setParameter("id", id)
                .getSingleResult()
            > 0) throw ApiException.conflict("Ngừng phân công chức vụ trước");
    row.setName(r.name().trim());
    row.setActive(r.active());
    if (id == null) em.persist(row);
    audit(a, "JOB_TITLE_UPDATED");
    em.flush();
    return row;
  }

  private Account personnel(Long id) {
    var a = accounts.findById(id).orElseThrow(ApiException::missing);
    if (a.getRole() == Role.STUDENT || !Boolean.TRUE.equals(a.getStatus()))
      throw ApiException.bad("Chọn tài khoản nhân sự đang hoạt động");
    if (a.getRole() != Role.SUPER_ADMIN) {
      String type =
          a.getRole() == Role.ADMIN ? "Admin" : a.getRole() == Role.STAFF ? "Staff" : "Teacher";
      long count =
          em.createQuery(
                  "select count(p) from "
                      + type
                      + " p where p.account.id=:id and p.workStatus='ACTIVE' and p.endDate is null",
                  Long.class)
              .setParameter("id", id)
              .getSingleResult();
      if (count != 1) throw ApiException.bad("Nhân sự đã ngừng làm việc hoặc thiếu hồ sơ");
    }
    return a;
  }

  public OrganizationAssignment assign(Long id, AssignmentRequest r, String username) {
    var a = editor(username);
    var target =
        r.active()
            ? personnel(r.accountId())
            : accounts.findById(r.accountId()).orElseThrow(ApiException::missing);
    var unit = require(OrganizationUnit.class, r.unitId());
    var title = require(JobTitle.class, r.titleId());
    if (r.active() && (!unit.isActive() || !title.isActive()))
      throw ApiException.bad("Đơn vị/chức vụ đã ngừng hoạt động");
    if (r.active()
        && unit.getBranchId() != null
        && target.getRole() != Role.SUPER_ADMIN
        && !target.activeBranchIds().contains(unit.getBranchId()))
      throw ApiException.bad("Nhân sự chưa được phân công chi nhánh của đơn vị");
    if (r.active() && r.managerAccountId() != null) {
      personnel(r.managerAccountId());
      var visited = new HashSet<Long>();
      var pending = new ArrayDeque<Long>();
      pending.add(r.managerAccountId());
      while (!pending.isEmpty()) {
        Long manager = pending.removeFirst();
        if (manager.equals(r.accountId()))
          throw ApiException.bad("Quan hệ quản lý không được tạo vòng lặp");
        if (visited.add(manager))
          em.createQuery(
                  "select x.managerAccountId from OrganizationAssignment x where x.active=true and"
                      + " x.accountId=:id and x.managerAccountId is not null",
                  Long.class)
              .setParameter("id", manager)
              .getResultList()
              .forEach(pending::addLast);
      }
      if (r.managerAccountId().equals(r.accountId()))
        throw ApiException.bad("Không thể tự quản lý chính mình");
    }
    var row = id == null ? new OrganizationAssignment() : require(OrganizationAssignment.class, id);
    if (id != null) {
      em.lock(row, LockModeType.PESSIMISTIC_WRITE);
      version(row.getVersion(), r.version());
    }
    Long duplicate =
        em.createQuery(
                "select count(x) from OrganizationAssignment x where x.accountId=:a and x.unitId=:u"
                    + " and x.titleId=:t and (:id is null or x.id<>:id)",
                Long.class)
            .setParameter("a", r.accountId())
            .setParameter("u", r.unitId())
            .setParameter("t", r.titleId())
            .setParameter("id", id)
            .getSingleResult();
    if (duplicate > 0) throw ApiException.conflict("Phân công chức vụ đã tồn tại");
    row.setAccountId(r.accountId());
    row.setUnitId(r.unitId());
    row.setTitleId(r.titleId());
    row.setManagerAccountId(r.managerAccountId());
    row.setActive(r.active());
    if (id == null) em.persist(row);
    audit(a, "ORG_ASSIGNMENT_UPDATED");
    em.flush();
    return row;
  }

  private void audit(Account a, String action) {
    a.recordSecurityEvent(action, a.getId(), "Cập nhật cơ cấu công ty");
  }

  @Transactional(readOnly = true)
  public PageResponse<Map<String, Object>> directory(
      String username, String keyword, Long branch, Long unit, Long title, int page, int size) {
    actor(username);
    if (page < 0
        || size < 1
        || size > 100
        || (long) page * size > Integer.MAX_VALUE
        || keyword != null && keyword.length() > 120) throw ApiException.bad("Bộ lọc không hợp lệ");
    var p = new HashMap<String, Object>();
    String from =
        " from accounts a left join admins ad on ad.account_id=a.id and a.role='ADMIN' left join"
            + " staffs st on st.account_id=a.id and a.role='STAFF' left join teachers te on"
            + " te.account_id=a.id and a.role='TEACHER' left join super_admins su on"
            + " su.account_id=a.id and a.role='SUPER_ADMIN'";
    String w =
        " where a.role in ('SUPER_ADMIN','ADMIN','STAFF','TEACHER') and a.status=true and"
            + " (a.role='SUPER_ADMIN' or"
            + " coalesce(ad.work_status,st.work_status,te.work_status)='ACTIVE')";
    if (keyword != null && !keyword.isBlank()) {
      p.put(
          "q",
          "%"
              + keyword
                  .trim()
                  .toLowerCase(Locale.ROOT)
                  .replace("!", "!!")
                  .replace("%", "!%")
                  .replace("_", "!_")
              + "%");
      w +=
          " and (lower(coalesce(ad.full_name,st.full_name,te.full_name,su.full_name,'')) like :q"
              + " escape '!' or lower(a.personnel_code) like :q escape '!')";
    }
    if (branch != null) {
      p.put("branch", branch);
      w +=
          " and exists(select 1 from account_branches b where b.account_id=a.id and b.status=true"
              + " and b.branch_id=:branch)";
    }
    if (unit != null || title != null) {
      p.put("unit", unit);
      p.put("title", title);
      w +=
          " and exists(select 1 from organization_assignments x join organization_units u on"
              + " u.id=x.unit_id join job_titles jt on jt.id=x.title_id where x.account_id=a.id and"
              + " x.active=true and u.active=true and jt.active=true and (:unit is null or"
              + " x.unit_id=:unit) and (:title is null or x.title_id=:title))";
    }
    long n = jdbc.queryForObject("select count(*)" + from + w, p, Long.class);
    p.put("limit", size);
    p.put("offset", page * size);
    var rows =
        jdbc.queryForList(
            "select a.id as accountId,a.personnel_code as"
                + " personnelCode,coalesce(ad.full_name,st.full_name,te.full_name,su.full_name) as"
                + " fullName"
                + from
                + w
                + " order by a.id limit :limit offset :offset",
            p);
    if (!rows.isEmpty()) {
      var ids = rows.stream().map(r -> ((Number) r.get("accountId")).longValue()).toList();
      var memberships =
          jdbc.queryForList(
              "select account_id,branch_id from account_branches where status=true and account_id"
                  + " in (:ids)",
              Map.of("ids", ids));
      var jobs =
          jdbc.queryForList(
              "select x.account_id,u.name as unitName,t.name as titleName from"
                  + " organization_assignments x join organization_units u on u.id=x.unit_id and"
                  + " u.active=true join job_titles t on t.id=x.title_id and t.active=true where"
                  + " x.active=true and x.account_id in (:ids)",
              Map.of("ids", ids));
      for (var row : rows) {
        long id = ((Number) row.get("accountId")).longValue();
        row.put(
            "branchIds",
            memberships.stream()
                .filter(m -> ((Number) m.get("account_id")).longValue() == id)
                .map(m -> m.get("branch_id"))
                .toList());
        row.put(
            "positions",
            jobs.stream()
                .filter(m -> ((Number) m.get("account_id")).longValue() == id)
                .map(m -> Map.of("unit", m.get("unitName"), "title", m.get("titleName")))
                .toList());
      }
    }
    return new PageResponse<>(rows, page, size, n, (int) ((n + size - 1) / size));
  }
}
