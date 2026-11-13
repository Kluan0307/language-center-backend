package com.example.finance.payment;

import com.example.finance.common.*;
import com.example.finance.security.*;
import com.example.finance.tuition.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
  private final PaymentRepository repository;
  @org.springframework.beans.factory.annotation.Autowired private jakarta.persistence.EntityManager em;
  private final TuitionService tuitions;
  private final BranchScope scope;
  private final com.example.finance.ledger.FinanceLedger ledger;

  public PageResponse<PaymentDto> list(
      int page, int size, String keyword, Long branchId, Long tuitionId) {
    return search(page,size,keyword,branchId,tuitionId,null,null);
  }

  public PageResponse<PaymentDto> search(int page,int size,String keyword,Long branchId,Long tuitionId,Long studentId,Long classId) {
    return search(page,size,keyword,branchId,tuitionId,studentId,classId,false);
  }

  public PageResponse<PaymentDto> search(int page,int size,String keyword,Long branchId,Long tuitionId,Long studentId,Long classId,boolean allAssigned) {
    var selected=scope.selection(branchId,allAssigned);
    if(keyword!=null&&keyword.length()>120)throw ApiException.bad("Keyword too long");
    for(var id:java.util.Arrays.asList(tuitionId,studentId,classId))if(id!=null&&id<=0)throw ApiException.bad("Invalid filter id");
    if (tuitionId != null) tuitions.require(tuitionId);
    var rows =
        repository.findAll(
            Query.<Payment>all().and((r,q,b)->selected==null?b.conjunction():selected.isEmpty()?b.disjunction():r.get("branchId").in(selected))
                .and(Query.eq("tuitionId", tuitionId))
                .and((r,q,b)->{
                  var sub=q.subquery(Long.class);var t=sub.from(Tuition.class);
                  var filters=new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                  filters.add(b.equal(t.get("id"),r.get("tuitionId")));filters.add(b.equal(t.get("branchId"),r.get("branchId")));
                  if(studentId!=null)filters.add(b.equal(t.get("studentId"),studentId));
                  if(classId!=null)filters.add(b.equal(t.get("classId"),classId));
                  sub.select(t.get("id")).where(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
                  var linked=b.exists(sub);
                  if(keyword==null||keyword.isBlank())return studentId==null&&classId==null?b.conjunction():linked;
                  var text=Query.<Payment>keyword(keyword,"receiptNo","referenceCode").toPredicate(r,q,b);
                  String pattern="%"+keyword.trim().toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
                  var names=q.subquery(Long.class);var n=names.from(Tuition.class);
                  names.select(n.get("id")).where(b.equal(n.get("id"),r.get("tuitionId")),b.equal(n.get("branchId"),r.get("branchId")),b.or(b.like(b.lower(n.get("studentName")),pattern,'!'),b.like(b.lower(n.get("className")),pattern,'!')));
                  var match=b.or(text,b.exists(names));
                  return studentId==null&&classId==null?match:b.and(linked,match);
                }),
            Query.page(page, size));
    var totals = ledger.totals(rows.getContent().stream().map(Payment::getId).toList());
    var context=context(rows.getContent());
    return PageResponse.of(rows.map(p -> enrich(p,ledger.dto(p, totals),context)));
  }

  public PaymentDto get(Long id) {
    Payment p = repository.findById(id).orElseThrow(ApiException::missing);
    scope.check(p.getBranchId());
    return enrich(p,ledger.dto(p),context(java.util.List.of(p)));
  }

  private record Context(java.util.Map<Long,Tuition> tuition,java.util.Map<Long,com.example.finance.online.OnlineOrder> orders) {}
  private Context context(java.util.List<Payment> rows){
    if(rows.isEmpty())return new Context(java.util.Map.of(),java.util.Map.of());
    var bills=em.createQuery("select t from Tuition t where t.id in :ids",Tuition.class).setParameter("ids",rows.stream().map(Payment::getTuitionId).distinct().toList()).getResultList().stream().collect(java.util.stream.Collectors.toMap(Tuition::getId,t->t));
    var online=em.createQuery("select o from OnlineOrder o where o.receiptId in :ids",com.example.finance.online.OnlineOrder.class).setParameter("ids",rows.stream().map(Payment::getId).toList()).getResultList().stream().collect(java.util.stream.Collectors.toMap(com.example.finance.online.OnlineOrder::getReceiptId,o->o,(a,b)->a));
    return new Context(bills,online);
  }
  private PaymentDto enrich(Payment p,PaymentDto dto,Context context){
    var bill=context.tuition().get(p.getTuitionId());if(bill!=null&&!java.util.Objects.equals(bill.getBranchId(),p.getBranchId()))bill=null;
    var order=context.orders().get(p.getId());
    boolean online=order!=null&&java.util.Objects.equals(order.getBranchId(),p.getBranchId())&&java.util.Objects.equals(order.getTuitionId(),p.getTuitionId());
    String source=online?"ONLINE":p.getRequestKey()!=null&&p.getRequestKey().toUpperCase(java.util.Locale.ROOT).startsWith("ONLINE-")?"REVIEW_REQUIRED":"DIRECT";
    return dto.withContext(source,online?order.getId():null,bill);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PaymentDto create(PaymentDto.Request r) {
    if(r.requestKey()!=null&&r.requestKey().toUpperCase(java.util.Locale.ROOT).startsWith("ONLINE-"))throw ApiException.bad("Reserved payment request prefix");
    if (r.paymentMethod() != Payment.PaymentMethod.CASH
        && r.paymentMethod() != Payment.PaymentMethod.BANK_TRANSFER)
      throw ApiException.bad("Use CASH or BANK_TRANSFER");
    if (Users.current().role() == Role.TEACHER) throw ApiException.forbidden();
    Tuition t = tuitions.lock(r.tuitionId());
    var prior = repository.findByBranchIdAndRequestKey(t.getBranchId(), r.requestKey());
    if (prior.isPresent()) {
      Payment p = prior.get();
      if (!p.getTuitionId().equals(r.tuitionId())
          || p.getAmount().compareTo(r.amount()) != 0
          || p.getPaymentMethod() != r.paymentMethod()
          || !java.util.Objects.equals(p.getReferenceCode(), r.referenceCode())
          || !java.util.Objects.equals(p.getNote(), r.note()))
        throw ApiException.conflict("requestKey was used for a different payment");
      return ledger.dto(p).withContext("DIRECT",null,t);
    }
    if (t.getStatus() == Tuition.Status.CANCELLED)
      throw ApiException.conflict("Tuition is cancelled");
    ledger.requireSynced(t);
    BigDecimal collected = ledger.net(t.getId());
    if (r.amount().signum() <= 0 || r.amount().compareTo(t.getAmount().subtract(collected)) > 0)
      throw ApiException.conflict("Payment exceeds outstanding balance");
    Payment p = new Payment();
    p.setTuitionId(t.getId());
    p.setBranchId(t.getBranchId());
    p.setAmount(r.amount());
    p.setPaymentMethod(r.paymentMethod());
    p.setReferenceCode(r.referenceCode());
    p.setRequestKey(r.requestKey());
    p.setReceiptNo("PT-" + UUID.randomUUID());
    p.setRecordedBy(Users.current().accountId());
    p.setRecordedByName(Users.current().username());
    p.setNote(r.note());
    repository.saveAndFlush(p);
    ledger.setNet(t, collected.add(r.amount()));
    if(t.getStudentId()!=null)com.example.finance.notification.StudentEvents.add(em,"finance:receipt:"+p.getId(),"PAYMENT","RECEIPT_CREATED","Phiếu thu đã được ghi nhận","Trung tâm đã ghi nhận phiếu thu. Mở phiếu để xem số tiền và lịch sử.",t.getStudentId(),null,null,null,t.getBranchId(),"RECEIPT",p.getId().toString(),null,null);
    return ledger.dto(p).withContext("DIRECT",null,t);
  }
}
