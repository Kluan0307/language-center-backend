package com.example.academic.assistant;

import com.example.academic.common.*;
import com.example.academic.security.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
public class CareInsights {
  private final AcademicReadService read;
  private final ReadQuery query;
  private final NamedParameterJdbcTemplate jdbc;
  private final ClassificationFilters classification;

  @Getter
  @Setter
  public static class Request {
    private ReadQuery.Mode scopeMode = ReadQuery.Mode.ACTIVE_BRANCH;
    private Long branchId, assigneeId, leadId, supportId, studentId;
    private String keyword, status, followUp, groupBy = "NONE", metric = "BRANCHES_ACTIVE";
    private String programCode, audienceCode;
    private boolean openOnly = true;
    private int page = 0, size = 5;
  }

  private ReadQuery.Scope scope(Request r) {
    query.page(r.page, r.size);
    for (Long id : Arrays.asList(r.assigneeId, r.leadId, r.supportId, r.studentId)) query.id(id);
    return query.scope(r.scopeMode, r.branchId);
  }

  private String eq(Map<String, Object> p, String k, Object v, String field) {
    if (v == null) return "";
    p.put(k, v);
    return " and " + field + "=:" + k;
  }

  private String leadWhere(ReadQuery.Scope s, Request r, Map<String, Object> p) {
    String w =
        read.scope(s, "l.branch_id")
            + read.keyword(p, r.keyword, "l.full_name")
            + eq(p, "lead", r.leadId, "l.id");
    if (Users.current().role() == Role.STAFF) {
      if (r.assigneeId != null && !r.assigneeId.equals(Users.current().accountId()))
        throw ApiException.forbidden();
      r.assigneeId = Users.current().accountId();
    }
    w += eq(p, "assignee", r.assigneeId, "l.assigned_to");
    if (r.openOnly) w += " and l.status in ('NEW','CONTACTED','CONSULTING')";
    if (r.status != null) {
      if (!Set.of("NEW", "CONTACTED", "CONSULTING", "CONVERTED", "LOST").contains(r.status))
        throw ApiException.bad("Invalid lead status");
      w += eq(p, "status", r.status, "l.status");
    }
    var now = LocalDateTime.now(BusinessTime.ZONE);
    p.put("now", now);
    p.put("day", now.toLocalDate().atStartOfDay());
    p.put("tomorrow", now.toLocalDate().plusDays(1).atStartOfDay());
    if (r.followUp != null)
      w +=
          switch (r.followUp) {
            case "OVERDUE" -> " and l.next_contact_at<:now";
            case "TODAY" -> " and l.next_contact_at>=:day and l.next_contact_at<:tomorrow";
            case "UPCOMING" -> " and l.next_contact_at>=:tomorrow";
            case "UNSCHEDULED" -> " and l.next_contact_at is null";
            default -> throw ApiException.bad("Invalid follow-up");
          };
    return w;
  }

  public ReadQuery.Result leads(Request r) {
    var s = scope(r);
    var p = read.params(s);
    String w = leadWhere(s, r, p);
    return read.page(
        s,
        "search_leads",
        "l.id,l.full_name,l.branch_id,l.status,l.assigned_to,l.assigned_to_name,l.next_contact_at,l.last_contacted_at",
        "from leads l",
        w,
        "case when l.next_contact_at<:now then 0 when l.next_contact_at<:tomorrow then 1 when"
            + " l.next_contact_at is null then 2 else 3 end,l.next_contact_at,l.id",
        p,
        r.page,
        r.size);
  }

  public ReadQuery.Result leadStats(Request r) {
    var s = scope(r);
    var p = read.params(s);
    String w = leadWhere(s, r, p);
    return stats(s, "get_care_statistics", "from leads l", w, p, r, "l.branch_id", "l.assigned_to");
  }

  private String supportWhere(ReadQuery.Scope s, Request r, Map<String, Object> p) {
    String w =
        read.scope(s, "sr.branch_id")
            + eq(p, "support", r.supportId, "sr.id")
            + eq(p, "student", r.studentId, "sr.student_id");
    if (r.openOnly) w += " and sr.status in ('OPEN','IN_PROGRESS')";
    if (r.status != null) {
      if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(r.status))
        throw ApiException.bad("Invalid support status");
      w += eq(p, "status", r.status, "sr.status");
    }
    return w;
  }

  public ReadQuery.Result support(Request r) {
    var s = scope(r);
    var p = read.params(s);
    String w = supportWhere(s, r, p);
    return read.page(
        s,
        "search_support_requests",
        "sr.id,sr.student_id,sr.branch_id,sr.category,sr.status,sr.created_at,sr.updated_at",
        "from student_support_requests sr",
        w,
        "sr.updated_at desc,sr.id",
        p,
        r.page,
        r.size);
  }

  public ReadQuery.Result supportStats(Request r) {
    var s = scope(r);
    var p = read.params(s);
    return stats(
        s,
        "get_support_statistics",
        "from student_support_requests sr",
        supportWhere(s, r, p),
        p,
        r,
        "sr.branch_id",
        null);
  }

  private ReadQuery.Result stats(
      ReadQuery.Scope s,
      String source,
      String from,
      String w,
      Map<String, Object> p,
      Request r,
      String branch,
      String assignee) {
    if (!Set.of("NONE", "BRANCH", "ASSIGNEE").contains(r.groupBy)
        || r.groupBy.equals("ASSIGNEE") && assignee == null)
      throw ApiException.bad("Invalid group");
    if (!r.groupBy.equals("NONE")) {
      if (Users.current().role() == Role.STAFF) throw ApiException.forbidden();
      String key = r.groupBy.equals("BRANCH") ? branch : assignee;
      String alias = r.groupBy.equals("BRANCH") ? "branch_id" : "assigned_to";
      return read.page(
          s,
          source,
          "g.*",
          "from (select "
              + key
              + " as "
              + alias
              + ",count(*) as item_count "
              + from
              + " where "
              + w
              + " group by "
              + key
              + ") g",
          "1=1",
          "g.item_count desc,g." + alias,
          p,
          r.page,
          r.size);
    }
    return query.result(
        s,
        source,
        Map.of(
            "value",
            jdbc.queryForObject("select count(*) " + from + " where " + w, p, Long.class),
            "asOf",
            LocalDate.now(BusinessTime.ZONE),
            "metric",
            source.equals("get_care_statistics") ? "MATCHING_LEADS" : "MATCHING_SUPPORT_REQUESTS"));
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ReadQuery.Result center(Request r) {
    classification.validate(r.programCode, r.audienceCode);
    if ((r.programCode != null || r.audienceCode != null) && !"COURSES_COUNT".equals(r.metric))
      throw ApiException.bad("Phân loại chỉ áp dụng cho thống kê khóa học");
    var s = scope(r);
    var p = read.params(s);
    String from, w;
    switch (r.metric) {
      case "BRANCHES_ACTIVE" -> {
        from = "from branches b";
        w = read.scope(s, "b.id") + " and b.status='ACTIVE'";
      }
      case "BRANCHES_WITHOUT_ADMIN" -> {
        from = "from branches b";
        w =
            read.scope(s, "b.id")
                + " and b.status='ACTIVE' and not exists(select 1 from account_branches ab join"
                + " accounts a on a.id=ab.account_id where ab.branch_id=b.id and ab.status=true and"
                + " a.role='ADMIN' and a.status=true)";
      }
      case "ROOMS_COUNT" -> {
        from = "from rooms b";
        w = read.scope(s, "b.branch_id");
        if (r.status != null) {
          if (!Set.of("AVAILABLE", "MAINTENANCE").contains(r.status))
            throw ApiException.bad("Invalid room status");
          w += eq(p, "status", r.status, "b.status");
        }
      }
      case "CLASSES_COUNT" -> {
        from = "from school_classes b";
        w = read.scope(s, "b.branch_id");
      }
      case "COURSES_COUNT" -> {
        from = "from courses b";
        w = "(b.branch_id is null or " + read.scope(s, "b.branch_id") + ")";
        w += classification.sql(p, "b", r.programCode, r.audienceCode);
        if (r.status != null) {
          if (!Set.of("OPEN", "CLOSED").contains(r.status))
            throw ApiException.bad("Invalid course status");
          w += eq(p, "status", r.status, "b.status");
        }
      }
      default -> throw ApiException.bad("Invalid metric");
    }
    if (r.metric.equals("BRANCHES_WITHOUT_ADMIN"))
      return read.page(
          s,
          "get_center_statistics",
          "b.id as branch_id,b.code,b.name",
          from,
          w,
          "b.id",
          p,
          r.page,
          r.size);
    return query.result(
        s,
        "get_center_statistics",
        Map.of(
            "metric",
            r.metric,
            "value",
            jdbc.queryForObject("select count(*) " + from + " where " + w, p, Long.class),
            "asOf",
            LocalDate.now(BusinessTime.ZONE)));
  }
}
