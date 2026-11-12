package com.example.finance.tuition;

import com.example.finance.common.*;
import com.example.finance.security.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TuitionService {
  private final TuitionRepository repository;
  private final BranchScope scope;
  private final RemoteApi api;
  private final EntityManager entityManager;
  private final com.example.finance.ledger.FinanceLedger ledger;
  private final com.example.finance.ledger.TuitionAdjustmentRepository adjustments;

  @Value("${services.academic.url}")
  private String academic;

  public record Billing(
      Long enrollmentId,
      Long branchId,
      Long studentId,
      String studentName,
      Long classId,
      String className,
      Long courseId,
      String courseName,
      BigDecimal tuitionFee,
      LocalDate dueDate) {}

  public Tuition require(Long id) {
    Tuition t = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(t.getBranchId());
    return t;
  }

  public Tuition lock(Long id) {
    Tuition t = repository.lock(id).orElseThrow(ApiException::missing);
    entityManager.refresh(t, LockModeType.PESSIMISTIC_WRITE);
    scope.check(t.getBranchId());
    return t;
  }

  public TuitionDto get(Long id) {
    return TuitionDto.of(require(id));
  }

  public PageResponse<TuitionDto> list(
      int page,
      int size,
      String keyword,
      Tuition.Status status,
      Long branchId,
      Long classId,
      Long studentId,
      Boolean overdue) {
    Specification<Tuition> query =
        Query.<Tuition>branch(scope.filter(branchId))
            .and(Query.keyword(keyword, "studentName", "className", "courseName"))
            .and(Query.eq("classId", classId))
            .and(Query.eq("studentId", studentId));
    if (status != null)
      query =
          query.and(
              (r, q, b) ->
                  switch (status) {
                    case PAID ->
                        b.and(
                            b.lessThanOrEqualTo(r.get("amount"), r.get("paidAmount")),
                            b.notEqual(r.get("status"), Tuition.Status.CANCELLED));
                    case PARTIAL ->
                        b.and(
                            b.greaterThan(r.get("paidAmount"), BigDecimal.ZERO),
                            b.lessThan(r.get("paidAmount"), r.get("amount")),
                            b.notEqual(r.get("status"), Tuition.Status.CANCELLED));
                    case UNPAID, PENDING ->
                        b.and(
                            b.equal(r.get("paidAmount"), BigDecimal.ZERO),
                            b.greaterThan(r.get("amount"), BigDecimal.ZERO),
                            b.notEqual(r.get("status"), Tuition.Status.CANCELLED));
                    case OVERDUE ->
                        b.and(
                            b.lessThan(
                                r.get("dueDate"),
                                LocalDate.now(com.example.finance.common.BusinessTime.ZONE)),
                            b.greaterThan(r.get("amount"), r.get("paidAmount")),
                            b.notEqual(r.get("status"), Tuition.Status.CANCELLED));
                    case CANCELLED -> b.equal(r.get("status"), Tuition.Status.CANCELLED);
                  });
    if (Boolean.TRUE.equals(overdue))
      query =
          query.and(
              (r, q, b) ->
                  b.and(
                      b.lessThan(
                          r.get("dueDate"),
                          LocalDate.now(com.example.finance.common.BusinessTime.ZONE)),
                      b.greaterThan(r.get("amount"), r.get("paidAmount")),
                      b.notEqual(r.get("status"), Tuition.Status.CANCELLED)));
    return PageResponse.of(repository.findAll(query, Query.page(page, size)).map(TuitionDto::of));
  }

  public Billing billing(Long id) {
    return api.get(academic + "/api/v1/academic/enrollments/" + id + "/billing", Billing.class);
  }

  @Transactional
  public TuitionDto create(TuitionDto.Request r) {
    Billing b = billing(r.enrollmentId());
    scope.check(b.branchId());
    var existing = repository.findByEnrollmentId(b.enrollmentId());
    if (existing.isPresent()) {
      scope.check(existing.get().getBranchId());
      return TuitionDto.of(existing.get());
    }
    if (repository.existsByStudentIdAndClassIdAndEnrollmentIdIsNull(b.studentId(), b.classId()))
      throw ApiException.conflict(
          "Legacy tuition must be linked to its enrollment before retrying");
    if (b.tuitionFee() == null || b.tuitionFee().signum() < 0)
      throw ApiException.bad("Invalid enrollment price");
    Tuition t = new Tuition();
    t.setEnrollmentId(b.enrollmentId());
    t.setBranchId(b.branchId());
    t.setStudentId(b.studentId());
    t.setStudentName(b.studentName());
    t.setClassId(b.classId());
    t.setClassName(b.className());
    t.setCourseId(b.courseId());
    t.setCourseName(b.courseName());
    t.setOriginalAmount(b.tuitionFee());
    t.setAmount(b.tuitionFee());
    t.setDueDate(b.dueDate());
    t.setStatus(t.paymentStatus());
    repository.saveAndFlush(t);
    if(t.getStudentId()!=null)com.example.finance.notification.StudentEvents.add(entityManager,"finance:tuition:"+t.getId(),"TUITION","TUITION_CREATED","Có khoản học phí mới","Mở học phí để xem khoản thu và thời hạn thanh toán.",t.getStudentId(),null,null,null,t.getBranchId(),"TUITION",t.getId().toString(),null,null);
    return TuitionDto.of(t);
  }

  public TuitionDto existing(Long id) {
    Billing b = billing(id);
    Tuition t = repository.findByEnrollmentId(b.enrollmentId()).orElseThrow(ApiException::missing);
    scope.check(t.getBranchId());
    return TuitionDto.of(t);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TuitionDto adjust(Long id, TuitionDto.Adjustment r) {
    if (Users.current().role() != Role.ADMIN && Users.current().role() != Role.SUPER_ADMIN)
      throw ApiException.forbidden();
    Tuition t = lock(id);
    var reason = com.example.finance.ledger.FinanceLedger.reason(r.reason());
    var prior = adjustments.findByBranchIdAndRequestKey(t.getBranchId(), r.requestKey());
    if (prior.isPresent()) {
      var a = prior.get();
      if (!a.getTuitionId().equals(id)
          || a.getNewOriginal().compareTo(r.originalAmount()) != 0
          || a.getNewDiscount().compareTo(r.discountAmount()) != 0
          || !a.getNewDueDate().equals(r.dueDate())
          || !a.getReason().equals(reason)
          || !a.getExpectedVersion().equals(r.version()))
        throw ApiException.conflict("requestKey đã dùng cho điều chỉnh khác");
      return TuitionDto.of(t);
    }
    if (r.version() == null || r.version() != t.getVersion())
      throw ApiException.conflict("Học phí đã thay đổi; hãy tải lại");
    ledger.requireSynced(t);
    BigDecimal net = r.originalAmount().subtract(r.discountAmount());
    if (net.signum() < 0) throw ApiException.bad("Giảm trừ vượt học phí gốc");
    var a = new com.example.finance.ledger.TuitionAdjustment();
    a.setTuitionId(id);
    a.setBranchId(t.getBranchId());
    a.setOldOriginal(t.getOriginalAmount() == null ? t.getAmount() : t.getOriginalAmount());
    a.setOldDiscount(t.getDiscountAmount() == null ? BigDecimal.ZERO : t.getDiscountAmount());
    a.setOldAmount(t.getAmount());
    a.setOldDueDate(t.getDueDate());
    a.setNewOriginal(r.originalAmount());
    a.setNewDiscount(r.discountAmount());
    a.setNewAmount(net);
    a.setNewDueDate(r.dueDate());
    a.setPaidAtChange(t.paid());
    a.setReason(reason);
    a.setRequestKey(r.requestKey());
    a.setExpectedVersion(r.version());
    a.setRecordedBy(Users.current().accountId());
    a.setRecordedByName(Users.current().username());
    if (t.getStatus() != Tuition.Status.CANCELLED) adjustments.save(a);
    if (t.getStatus() == Tuition.Status.CANCELLED) throw ApiException.conflict("Cancelled tuition");
    t.setOriginalAmount(r.originalAmount());
    t.setDiscountAmount(r.discountAmount());
    t.setAmount(net);
    t.setDueDate(r.dueDate());
    t.setAdjustedBy(Users.current().accountId());
    t.setAdjustedByName(Users.current().username());
    t.setAdjustedAt(LocalDateTime.now(com.example.finance.common.BusinessTime.ZONE));
    t.setAdjustmentReason(reason);
    t.setStatus(t.paymentStatus());
    repository.flush();
    if(t.getStudentId()!=null)com.example.finance.notification.StudentEvents.add(entityManager,"finance:adjustment:"+a.getId(),"TUITION","TUITION_ADJUSTED","Học phí có cập nhật","Trung tâm đã cập nhật khoản học phí. Mở chi tiết để kiểm tra số dư hiện hành.",t.getStudentId(),null,null,null,t.getBranchId(),"TUITION",t.getId().toString(),null,null);
    return TuitionDto.of(t);
  }
}
