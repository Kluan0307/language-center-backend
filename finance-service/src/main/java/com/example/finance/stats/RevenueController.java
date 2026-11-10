package com.example.finance.stats;

import com.example.finance.common.*;
import com.example.finance.ledger.*;
import com.example.finance.payment.*;
import com.example.finance.security.*;
import com.example.finance.tuition.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
@Transactional(readOnly = true)
public class RevenueController {
  private final EntityManager em;
  private final BranchScope scope;
  private final TuitionRepository tuitions;
  private final PaymentRepository payments;
  private final PaymentCorrectionRepository corrections;

  public record Group(String key, BigDecimal amount) {}

  public record Revenue(
      BigDecimal collected,
      BigDecimal outstanding,
      List<Group> byMonth,
      List<Group> byCourse,
      List<Group> byBranch,
      BigDecimal grossCollected,
      BigDecimal voided,
      BigDecimal refunded,
      BigDecimal credit) {}

  private record Access(Long branch, List<Long> allowed) {}

  private Access access(Long requested, boolean allAssigned) {
    if (allAssigned && requested != null)
      throw ApiException.bad("Không kết hợp allAssigned và branchId");
    var u = Users.current();
    var allowed = allAssigned && u.role() != Role.SUPER_ADMIN ? u.branchIds() : null;
    if (allowed != null && allowed.isEmpty()) throw ApiException.forbidden();
    return new Access(allAssigned ? null : scope.filter(requested), allowed);
  }

  private String clause(Access a, String alias, Map<String, Object> params) {
    var s = "";
    if (a.branch() != null) {
      s += " and " + alias + ".branchId=:branch";
      params.put("branch", a.branch());
    }
    if (a.allowed() != null) {
      s += " and " + alias + ".branchId in :allowed";
      params.put("allowed", a.allowed());
    }
    return s;
  }

  public Revenue overview(Long branchId, LocalDate from, LocalDate to) {
    return overview(branchId, from, to, false);
  }

  @GetMapping("/overview")
  public Revenue overview(
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "false") boolean allAssigned) {
    if (from != null && to != null && from.isAfter(to))
      throw ApiException.bad("Invalid date range");
    var access = access(branchId, allAssigned);
    var params = new HashMap<String, Object>();
    String where =
        " from Payment p,Tuition t where p.tuitionId=t.id and p.branchId=t.branchId and"
            + " p.status='PAID'"
            + clause(access, "p", params);
    String reversed =
        " from PaymentCorrection c,Tuition t where c.tuitionId=t.id and c.branchId=t.branchId"
            + clause(access, "c", params);
    if (from != null) {
      where += " and p.paidAt>=:from";
      reversed += " and c.createdAt>=:from";
      params.put("from", from.atStartOfDay());
    }
    if (to != null) {
      where += " and p.paidAt<:to";
      reversed += " and c.createdAt<:to";
      params.put("to", to.plusDays(1).atStartOfDay());
    }
    var gross = total("p.amount", where, params);
    var voided = total("c.amount", reversed + " and c.type='VOID'", params);
    var refunded = total("c.amount", reversed + " and c.type='REFUND'", params);
    var now = new HashMap<String, Object>();
    var current = " from Tuition t where t.status<>'CANCELLED'" + clause(access, "t", now);
    var debt =
        total(
            "case when t.amount>coalesce(t.paidAmount,0) then t.amount-coalesce(t.paidAmount,0)"
                + " else 0 end",
            current,
            now);
    var credit =
        total(
            "case when coalesce(t.paidAmount,0)>t.amount then coalesce(t.paidAmount,0)-t.amount"
                + " else 0 end",
            current,
            now);
    return new Revenue(
        gross.subtract(voided).subtract(refunded),
        debt,
        combine(
            groups("concat(year(p.paidAt),'-',month(p.paidAt))", "p.amount", where, params),
            groups(
                "concat(year(c.createdAt),'-',month(c.createdAt))", "c.amount", reversed, params),
            true),
        combine(
            groups("t.courseId", "p.amount", where, params),
            groups("t.courseId", "c.amount", reversed, params),
            false),
        combine(
            groups("p.branchId", "p.amount", where, params),
            groups("c.branchId", "c.amount", reversed, params),
            false),
        gross,
        voided,
        refunded,
        credit);
  }

  private BigDecimal total(String expression, String where, Map<String, Object> params) {
    var q = em.createQuery("select coalesce(sum(" + expression + "),0)" + where, BigDecimal.class);
    params.forEach(q::setParameter);
    return q.getSingleResult();
  }

  private List<Group> groups(
      String field, String amount, String where, Map<String, Object> params) {
    var q =
        em.createQuery(
            "select " + field + ",sum(" + amount + ")" + where + " group by " + field,
            Object[].class);
    params.forEach(q::setParameter);
    return q.getResultList().stream()
        .map(r -> new Group(String.valueOf(r[0]), (BigDecimal) r[1]))
        .toList();
  }

  private List<Group> combine(List<Group> gross, List<Group> removed, boolean month) {
    var rows = new TreeMap<String, BigDecimal>();
    for (var g : gross) rows.merge(key(g.key(), month), g.amount(), BigDecimal::add);
    for (var g : removed) rows.merge(key(g.key(), month), g.amount().negate(), BigDecimal::add);
    return rows.entrySet().stream().map(e -> new Group(e.getKey(), e.getValue())).toList();
  }

  private String key(String key, boolean month) {
    if (!month) return key;
    var p = key.split("-");
    return String.format(Locale.ROOT, "%04d-%02d", Integer.parseInt(p[0]), Integer.parseInt(p[1]));
  }

  public record Reconciliation(
      Long tuitionId,
      String studentName,
      Long branchId,
      BigDecimal cachedPaid,
      BigDecimal ledgerPaid,
      BigDecimal difference,
      long legacyPayments) {}

  @GetMapping("/reconciliation")
  public PageResponse<Reconciliation> reconciliation(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long branchId,
      @RequestParam(defaultValue = "false") boolean allAssigned) {
    var a = access(branchId, allAssigned);
    var filter = Query.<Tuition>branch(a.branch());
    if (a.allowed() != null) filter = filter.and((r, q, b) -> r.get("branchId").in(a.allowed()));
    var rows = tuitions.findAll(filter, Query.page(page, size));
    var ids = rows.getContent().stream().map(Tuition::getId).toList();
    var net = new HashMap<Long, BigDecimal>();
    var legacy = new HashMap<Long, Long>();
    if (!ids.isEmpty()) {
      for (var row : payments.receiptTotals(ids)) net.put((Long) row[0], (BigDecimal) row[1]);
      for (var row : corrections.tuitionTotals(ids))
        net.merge((Long) row[0], ((BigDecimal) row[1]).negate(), BigDecimal::add);
      for (var row : payments.legacyCounts(ids)) legacy.put((Long) row[0], (Long) row[1]);
    }
    return PageResponse.of(
        rows.map(
            t -> {
              var n = net.getOrDefault(t.getId(), BigDecimal.ZERO);
              return new Reconciliation(
                  t.getId(),
                  t.getStudentName(),
                  t.getBranchId(),
                  t.paid(),
                  n,
                  t.paid().subtract(n),
                  legacy.getOrDefault(t.getId(), 0L));
            }));
  }
}
