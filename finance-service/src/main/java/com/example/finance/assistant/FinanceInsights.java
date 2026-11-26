package com.example.finance.assistant;

import com.example.finance.common.*;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
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
public class FinanceInsights {
  private final ReadQuery query;
  private final NamedParameterJdbcTemplate jdbc;

  @Getter
  @Setter
  public static class Request {
    private ReadQuery.Mode scopeMode = ReadQuery.Mode.ACTIVE_BRANCH;
    private Long branchId, studentId, classId, tuitionId, receiptId;
    private String receiptNo, receiptState = "ALL", status, groupBy = "NONE";
    private LocalDate from, to, dueFrom, dueTo, compareFrom, compareTo;
    private Boolean overdue;
    private boolean outstandingOnly;
    private int page = 0, size = 5;
  }

  private ReadQuery.Scope scope(Request r) {
    query.page(r.page, r.size);
    for (Long id : Arrays.asList(r.studentId, r.classId, r.tuitionId, r.receiptId)) query.id(id);
    return query.scope(r.scopeMode, r.branchId);
  }

  private String scoped(ReadQuery.Scope s, String field, Map<String, Object> p) {
    p.put("branches", s.branchIds());
    return s.mode() == ReadQuery.Mode.WHOLE_SYSTEM
        ? "1=1"
        : s.branchIds().isEmpty() ? "1=0" : field + " in (:branches)";
  }

  private String eq(Map<String, Object> p, String key, Object value, String field) {
    if (value == null) return "";
    p.put(key, value);
    return " and " + field + "=:" + key;
  }

  private void range(LocalDate from, LocalDate to) {
    if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 365)
      throw ApiException.bad("Cần khoảng ngày hợp lệ, tối đa 366 ngày");
  }

  private String dates(Map<String, Object> p, LocalDate from, LocalDate to, String field) {
    if (from == null && to == null) return "";
    range(from, to);
    p.put("from", from.atStartOfDay());
    p.put("to", to.plusDays(1).atStartOfDay());
    return " and " + field + ">=:from and " + field + "<:to";
  }

  private String filters(ReadQuery.Scope s, Request r, Map<String, Object> p) {
    return scoped(s, "t.branch_id", p)
        + eq(p, "student", r.studentId, "t.student_id")
        + eq(p, "class", r.classId, "t.class_id")
        + eq(p, "tuition", r.tuitionId, "t.id");
  }

  private void vnd(ReadQuery.Scope s, Request r, Map<String, Object> p) {
    if (jdbc.queryForObject(
            "select count(*) from tuition t where " + filters(s, r, p) + " and t.currency<>'VND'",
            p,
            Long.class)
        > 0) throw ApiException.conflict("Chưa hỗ trợ tổng hợp nhiều tiền tệ; không tự quy đổi");
  }

  private static final String BALANCE =
      "case when t.amount>coalesce(t.paid_amount,0) then t.amount-coalesce(t.paid_amount,0) else 0"
          + " end";
  private static final String CREDIT =
      "case when coalesce(t.paid_amount,0)>t.amount then coalesce(t.paid_amount,0)-t.amount else 0"
          + " end";

  private String billWhere(ReadQuery.Scope s, Request r, Map<String, Object> p) {
    String w = filters(s, r, p);
    if (r.outstandingOnly) w += " and t.status<>'CANCELLED' and (" + BALANCE + ")>0";
    p.put("today", LocalDate.now(BusinessTime.ZONE));
    if (r.overdue != null)
      w +=
          r.overdue
              ? " and t.status<>'CANCELLED' and t.due_date<:today and (" + BALANCE + ")>0"
              : " and (t.status='CANCELLED' or t.due_date>=:today or (" + BALANCE + ")=0)";
    if (r.status != null) {
      if (!Set.of("UNPAID", "PARTIAL", "PAID", "PENDING", "CANCELLED", "OVERDUE")
          .contains(r.status)) throw ApiException.bad("Invalid status");
      if (r.status.equals("CANCELLED")) w += " and t.status='CANCELLED'";
      else if (r.status.equals("OVERDUE"))
        w += " and t.status<>'CANCELLED' and t.due_date<:today and (" + BALANCE + ")>0";
      else if (r.status.equals("PENDING")) w += " and t.status='PENDING'";
      else
        w +=
            " and t.status<>'CANCELLED' and (case when ("
                + BALANCE
                + ")=0 then 'PAID' when coalesce(t.paid_amount,0)>0 then 'PARTIAL' else 'UNPAID'"
                + " end)='"
                + r.status
                + "'";
    }
    if (r.dueFrom != null || r.dueTo != null) {
      range(r.dueFrom, r.dueTo);
      p.put("dueFrom", r.dueFrom);
      p.put("dueTo", r.dueTo);
      w += " and t.due_date between :dueFrom and :dueTo";
    }
    return w;
  }

  private ReadQuery.Result page(
      ReadQuery.Scope s,
      String source,
      String select,
      String from,
      String where,
      String order,
      Map<String, Object> p,
      Request r) {
    long count = jdbc.queryForObject("select count(*) " + from + " where " + where, p, Long.class);
    p.put("limit", r.size);
    p.put("offset", r.page * r.size);
    var rows =
        jdbc.query(
            "select "
                + select
                + " "
                + from
                + " where "
                + where
                + " order by "
                + order
                + " limit :limit offset :offset",
            p,
            (rs, n) -> {
              var row = new LinkedHashMap<String, Object>();
              var meta = rs.getMetaData();
              for (int i = 1; i <= meta.getColumnCount(); i++) {
                String[] parts = meta.getColumnLabel(i).toLowerCase(Locale.ROOT).split("_");
                String key = parts[0];
                for (int j = 1; j < parts.length; j++)
                  key += Character.toUpperCase(parts[j].charAt(0)) + parts[j].substring(1);
                Object v = rs.getObject(i);
                if (v instanceof java.sql.Date d) v = d.toLocalDate();
                if (v instanceof java.sql.Timestamp t) v = t.toLocalDateTime();
                row.put(key, v);
              }
              if (source.equals("search_tuitions") || source.equals("search_receipts"))
                row.put(
                    "path",
                    (source.equals("search_tuitions")
                            ? "/tuition?tuitionId="
                            : "/payments?paymentId=")
                        + row.get("id")
                        + "&branchId="
                        + row.get("branchId"));
              return row;
            });
    return query.result(
        s,
        source,
        new PageResponse<>(rows, r.page, r.size, count, (int) Math.ceil((double) count / r.size)));
  }

  public ReadQuery.Result tuitions(Request r) {
    var s = scope(r);
    var p = new HashMap<String, Object>();
    String w = billWhere(s, r, p);
    return page(
        s,
        "search_tuitions",
        "t.id,t.enrollment_id as"
            + " billing_enrollment_id,t.student_id,t.student_name,t.class_id,t.class_name,t.branch_id,t.amount,coalesce(t.paid_amount,0)"
            + " as paid_amount,"
            + BALANCE
            + " as balance,"
            + CREDIT
            + " as credit,t.currency,t.due_date,case when t.status='CANCELLED' then 'CANCELLED'"
            + " when ("
            + BALANCE
            + ")=0 then 'PAID' when coalesce(t.paid_amount,0)>0 then 'PARTIAL' else 'UNPAID' end as"
            + " status,case when t.status<>'CANCELLED' and t.due_date<:today and ("
            + BALANCE
            + ")>0 then true else false end as overdue",
        "from tuition t",
        w,
        "t.due_date,t.id",
        p,
        r);
  }

  public ReadQuery.Result receipts(Request r) {
    var s = scope(r);
    var p = new HashMap<String, Object>();
    String w =
        filters(s, r, p)
            + " and p.branch_id=t.branch_id"
            + eq(p, "receipt", r.receiptId, "p.id")
            + eq(p, "receiptNo", r.receiptNo, "p.receipt_no");
    if (r.receiptNo != null && r.receiptNo.length() > 120)
      throw ApiException.bad("Receipt number too long");
    if (!Set.of("ALL", "VOIDED", "REFUNDED", "AVAILABLE", "LEGACY").contains(r.receiptState))
      throw ApiException.bad("Invalid receipt state");
    String removed =
        "coalesce((select sum(c.amount) from payment_corrections c where c.payment_id=p.id and"
            + " c.branch_id=p.branch_id and c.tuition_id=p.tuition_id and c.type='VOID'),0)";
    String refunded = removed.replace("'VOID'", "'REFUND'");
    if (Set.of("VOIDED", "REFUNDED").contains(r.receiptState)) {
      String type = r.receiptState.equals("VOIDED") ? "VOID" : "REFUND";
      w +=
          " and exists(select 1 from payment_corrections pc where pc.payment_id=p.id and"
              + " pc.tuition_id=p.tuition_id and pc.branch_id=p.branch_id and pc.type='"
              + type
              + "'"
              + dates(p, r.from, r.to, "pc.created_at")
              + ")";
    } else {
      w += dates(p, r.from, r.to, "p.paid_at");
      if (r.receiptState.equals("AVAILABLE"))
        w += " and p.status='PAID' and p.amount-" + removed + "-" + refunded + ">0";
      if (r.receiptState.equals("LEGACY")) w += " and p.status<>'PAID'";
    }
    var result =
        page(
            s,
            "search_receipts",
            "p.id,p.receipt_no,p.tuition_id,t.student_id,t.student_name,t.class_id,t.class_name,p.branch_id,p.amount,p.status,p.paid_at,"
                + removed
                + " as voided_amount,"
                + refunded
                + " as refunded_amount,case when p.status='PAID' then p.amount-"
                + removed
                + "-"
                + refunded
                + " else 0 end as available_amount",
            "from payments p join tuition t on t.id=p.tuition_id",
            w,
            "p.paid_at desc,p.id desc",
            p,
            r);
    if (Set.of("VOIDED", "REFUNDED").contains(r.receiptState))
      return new ReadQuery.Result(
          result.requestId(),
          result.status(),
          result.scope(),
          result.observedAt(),
          result.source(),
          result.data(),
          List.of(
              "Danh sách lọc theo ngày bút toán hủy/hoàn; số tiền hủy/hoàn trên từng phiếu là lũy"
                  + " kế. Muốn tổng tiền trong kỳ, dùng báo cáo dòng tiền."),
          result.nextPage(),
          result.hasMore());
    return result;
  }

  public ReadQuery.Result debt(Request r) {
    var s = scope(r);
    if (r.from != null || r.to != null)
      throw ApiException.bad("Công nợ là hiện trạng, không có snapshot lịch sử");
    var p = new HashMap<String, Object>();
    vnd(s, r, p);
    String w = billWhere(s, r, p) + " and t.status<>'CANCELLED'";
    return aggregates(
        s,
        "get_debt_statistics",
        "from tuition t",
        w,
        "count(*) as bill_count,coalesce(sum("
            + BALANCE
            + "),0) as outstanding,coalesce(sum("
            + CREDIT
            + "),0) as credit",
        p,
        r,
        "t.branch_id");
  }

  private ReadQuery.Result aggregates(
      ReadQuery.Scope s,
      String source,
      String from,
      String where,
      String columns,
      Map<String, Object> p,
      Request r,
      String branch) {
    if (!Set.of("NONE", "BRANCH").contains(r.groupBy)) throw ApiException.bad("Invalid group");
    if (r.groupBy.equals("BRANCH")) {
      if (com.example.finance.security.Users.current().role()
          == com.example.finance.security.Role.STAFF) throw ApiException.forbidden();
      String grouped =
          "from (select "
              + branch
              + " as branch_id,"
              + columns
              + " "
              + from
              + " where "
              + where
              + " group by "
              + branch
              + ") g";
      return page(s, source, "g.*", grouped, "1=1", "g.branch_id", p, r);
    }
    var data = jdbc.queryForMap("select " + columns + " " + from + " where " + where, p);
    var safe = new LinkedHashMap<String, Object>();
    data.forEach(
        (k, v) -> {
          String[] a = k.toLowerCase(Locale.ROOT).split("_");
          String key = a[0];
          for (int i = 1; i < a.length; i++)
            key += Character.toUpperCase(a[i].charAt(0)) + a[i].substring(1);
          safe.put(key, v);
        });
    safe.put("currency", "VND");
    safe.put("asOf", LocalDate.now(BusinessTime.ZONE));
    return query.result(s, source, safe);
  }

  private String flows(ReadQuery.Scope s, Request r, Map<String, Object> p) {
    vnd(s, r, p);
    range(r.from, r.to);
    String w = filters(s, r, p);
    String paid =
        w
            + " and p.branch_id=t.branch_id and p.status='PAID'"
            + dates(p, r.from, r.to, "p.paid_at");
    String reversed = w + " and c.branch_id=t.branch_id" + dates(p, r.from, r.to, "c.created_at");
    return "from (select p.branch_id,p.amount as gross_amount,0 as voided_amount,0 as"
        + " refunded_amount from payments p join tuition t on t.id=p.tuition_id where "
        + paid
        + " union all select c.branch_id,0,case when c.type='VOID' then c.amount else 0 end,case"
        + " when c.type='REFUND' then c.amount else 0 end from payment_corrections c join tuition t"
        + " on t.id=c.tuition_id where "
        + reversed
        + ") f";
  }

  private static final String FLOW_TOTALS =
      "coalesce(sum(gross_amount),0) as gross_collected,coalesce(sum(voided_amount),0) as"
          + " voided_amount,coalesce(sum(refunded_amount),0) as"
          + " refunded_amount,coalesce(sum(gross_amount-voided_amount-refunded_amount),0) as"
          + " net_collected";

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ReadQuery.Result cashFlow(Request r) {
    var s = scope(r);
    var p = new HashMap<String, Object>();
    var data =
        aggregates(s, "get_cash_flow", flows(s, r, p), "1=1", FLOW_TOTALS, p, r, "f.branch_id");
    return new ReadQuery.Result(
        data.requestId(),
        data.status(),
        data.scope(),
        data.observedAt(),
        data.source(),
        data.data(),
        List.of(
            "Dòng tiền theo ngày thu và ngày hủy/hoàn trong khoảng "
                + r.from
                + " đến "
                + r.to
                + " (hai đầu bao gồm); net có thể âm. Đây không phải doanh thu ghi nhận theo thời"
                + " gian học. Đơn online PENDING không phải phiếu thu."),
        data.nextPage(),
        data.hasMore());
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public ReadQuery.Result compare(Request r) {
    var s = scope(r);
    range(r.from, r.to);
    range(r.compareFrom, r.compareTo);
    if (ChronoUnit.DAYS.between(r.from, r.to) != ChronoUnit.DAYS.between(r.compareFrom, r.compareTo)
        || !(r.to.isBefore(r.compareFrom) || r.compareTo.isBefore(r.from)))
      throw ApiException.bad("Hai kỳ phải bằng độ dài và không chồng lấn");
    if (!r.groupBy.equals("NONE")) throw ApiException.bad("Comparison is scalar");
    var first = cashFlow(r);
    var later = new Request();
    later.setScopeMode(r.scopeMode);
    later.setBranchId(r.branchId);
    later.setStudentId(r.studentId);
    later.setClassId(r.classId);
    later.setFrom(r.compareFrom);
    later.setTo(r.compareTo);
    var second = cashFlow(later);
    var a = (Map<?, ?>) first.data();
    var b = (Map<?, ?>) second.data();
    BigDecimal v1 = new BigDecimal(String.valueOf(a.get("netCollected"))),
        v2 = new BigDecimal(String.valueOf(b.get("netCollected")));
    var result = new LinkedHashMap<String, Object>();
    result.put("firstNet", v1);
    result.put("secondNet", v2);
    result.put("difference", v2.subtract(v1));
    result.put("firstFrom", r.from);
    result.put("firstTo", r.to);
    result.put("secondFrom", r.compareFrom);
    result.put("secondTo", r.compareTo);
    result.put("currency", "VND");
    return query.result(s, "compare_cash_flow", result);
  }
}
