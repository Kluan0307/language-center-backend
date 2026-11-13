package com.example.finance.online;
import com.example.finance.common.*;
import com.example.finance.ledger.FinanceLedger;
import com.example.finance.security.*;
import com.example.finance.student.StudentFinanceService;
import com.example.finance.tuition.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class OnlineOrderService {
 private final OnlineOrderRepository orders; private final TuitionRepository tuition; private final StudentFinanceService finance; private final FinanceLedger ledger; private final PaymentProviderConfig config; private final VnpayProvider provider; private final BranchScope scope; private final EntityManager em;
 public record Create(@NotNull UUID requestId,@NotNull @Positive Long tuitionId,@NotBlank @Pattern(regexp="[0-9]{1,10}(\\.[0-9]{1,2})?") String amount,@NotBlank @Pattern(regexp="VND") String currency) {}
 public record View(String id,Long tuitionId,String state,String amount,String currency,String provider,String environment,OffsetDateTime createdAt,OffsetDateTime expiresAt,Long receiptId,String message,boolean reviewRequired,long version,String checkoutUrl,String displayCode,String className,String courseName,Long studentId,String studentName,Long classId,Long branchId) {}
 public record Settings(boolean enabled,String provider,String environment,String message,String checkoutOrigin) {}
 private LocalDateTime now(){return LocalDateTime.now(BusinessTime.ZONE);}
 private void student(){var u=Users.current();if(u.role()!=Role.STUDENT||u.profileId()==null)throw ApiException.forbidden();}
 private void operator(){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.STAFF).contains(Users.current().role()))throw ApiException.forbidden();}
 private StudentFinanceService.TuitionView optionalLabel(OnlineOrder o){
   try{return finance.detail(o.getTuitionId()).tuition();}
   catch(ApiException ex){if(ex.getStatus().is5xxServerError())return null;throw ex;}
 }
 private Map<Long,StudentFinanceService.TuitionView> optionalLabels(List<Long> ids){
   try{return finance.labels(ids);}
   catch(ApiException ex){if(ex.getStatus().is5xxServerError())return Map.of();throw ex;}
 }
 private String fingerprint(Create c){return VnpayProvider.sha(c.tuitionId()+"|"+new BigDecimal(c.amount()).setScale(2).toPlainString()+"|"+c.currency());}
 private boolean expired(OnlineOrder o){return Set.of("CREATED","PENDING").contains(o.getState())&&now().isAfter(o.getExpiresAt());}
 public View view(OnlineOrder o,boolean student){return view(o,student,null,true);}
 private View view(OnlineOrder o,boolean student,StudentFinanceService.TuitionView label,boolean checkout){
   String state=expired(o)?"EXPIRED":o.getState();
   String message=switch(state){
     case "SUCCEEDED"->"Thanh toán đã được ghi nhận. Bạn có thể xem phiếu thu.";
     case "FAILED"->"Ngân hàng báo giao dịch không thành công. Kiểm tra học phí trước khi thử lại.";
     case "EXPIRED"->"Phiên thanh toán đã hết hạn. Nếu đã bị trừ tiền, hãy liên hệ trung tâm trước khi thử lại.";
     case "REVIEW_REQUIRED"->"Trung tâm đang kiểm tra giao dịch. Vui lòng không thanh toán lại nếu đã bị trừ tiền.";
     default->"Phiên thanh toán chưa có kết quả xác nhận. Bạn có thể tiếp tục nếu chưa thanh toán.";
   };
   String url=checkout&&student&&state.equals("PENDING")&&o.getReviewCode()==null&&config.isEnabled()&&o.getMerchantCode().equals(config.getMerchantCode())?provider.checkout(o):null;
   var snapshot=student?null:em.find(Tuition.class,o.getTuitionId());
   if(snapshot!=null&&(!Objects.equals(snapshot.getBranchId(),o.getBranchId())||!Objects.equals(snapshot.getStudentId(),o.getStudentId())))snapshot=null;
   return new View(o.getId(),o.getTuitionId(),state,StudentFinanceService.money(o.getAmount()),o.getCurrency(),o.getProvider(),o.getEnvironment(),o.getCreatedAt().atZone(BusinessTime.ZONE).toOffsetDateTime(),o.getExpiresAt().atZone(BusinessTime.ZONE).toOffsetDateTime(),o.getReceiptId(),message,o.getReviewCode()!=null,o.getVersion(),url,"GD-"+o.getId().substring(0,8).toUpperCase(Locale.ROOT),student?(label==null?null:label.enrollmentLabel()):snapshot==null?null:snapshot.getClassName(),student?(label==null?null:label.courseName()):snapshot==null?null:snapshot.getCourseName(),o.getStudentId(),snapshot==null?null:snapshot.getStudentName(),snapshot==null?null:snapshot.getClassId(),o.getBranchId());
 }
 public record PaymentContext(Long tuitionId,String action,String reason,String message,String outstanding,String currency,View order,boolean sandbox) {}
 private boolean owned(OnlineOrder o){return Objects.equals(o.getAccountId(),Users.current().accountId())&&Objects.equals(o.getStudentId(),Users.current().profileId());}
 private long held(Long id){return em.createQuery("select count(p) from ProviderTransaction p,OnlineOrder o where p.orderId=o.id and o.tuitionId=:tuition and p.state='REVIEW_REQUIRED'",Long.class).setParameter("tuition",id).getSingleResult();}
 private PaymentContext context(StudentFinanceService.TuitionView t,String action,String reason,String message,OnlineOrder o){
   return new PaymentContext(t.id(),action,reason,message,t.outstanding(),t.currency(),o!=null&&owned(o)?view(o,true,t,false):null,!config.protocolTest());
 }
 @Transactional(readOnly=true) public PaymentContext context(Long id){
   student();var t=finance.detail(id).tuition();
   var all=orders.findByTuitionIdOrderByCreatedAtDesc(id);
   return context(t,all,held(id)>0);
 }
 @Transactional(readOnly=true) public List<PaymentContext> contexts(List<Long> ids){
   student();if(ids==null||ids.isEmpty()||ids.size()>20||ids.stream().anyMatch(id->id==null||id<=0))throw ApiException.bad("Invalid tuition ids");
   var unique=ids.stream().distinct().toList();var labels=finance.labels(unique);
   if(!labels.keySet().containsAll(unique))throw ApiException.missing();
   var all=em.createQuery("select o from OnlineOrder o where o.tuitionId in :ids order by o.createdAt desc,o.id desc",OnlineOrder.class).setParameter("ids",unique).getResultList();
   var heldIds=new HashSet<>(em.createQuery("select distinct o.tuitionId from ProviderTransaction p,OnlineOrder o where p.orderId=o.id and o.tuitionId in :ids and p.state='REVIEW_REQUIRED'",Long.class).setParameter("ids",unique).getResultList());
   return unique.stream().map(id->context(labels.get(id),all.stream().filter(o->o.getTuitionId().equals(id)).toList(),heldIds.contains(id))).toList();
 }
 private PaymentContext context(StudentFinanceService.TuitionView t,List<OnlineOrder> all,boolean hasHeld){
   // Review wins over expiry: expiring a checkout does not resolve received money.
   for(var o:all)if(o.getReviewCode()!=null||o.getState().equals("REVIEW_REQUIRED"))return context(t,"SUPPORT","PAYMENT_REVIEW_REQUIRED","Trung tâm đang kiểm tra giao dịch của khoản này.",o);
   if(hasHeld)return context(t,"SUPPORT","PAYMENT_REVIEW_REQUIRED","Khoản này có giao dịch cần trung tâm kiểm tra.",null);
   if(!t.verification().equals("VERIFIED"))return context(t,"SUPPORT","TUITION_UNVERIFIED","Trung tâm cần xác minh học phí trước khi thanh toán.",null);
   if(t.cancelled())return context(t,"NONE","TUITION_CANCELLED","Khoản học phí đã hủy.",null);
   if(!"VND".equals(t.currency()))return context(t,"SUPPORT","UNSUPPORTED_CURRENCY","Vui lòng liên hệ trung tâm về đơn vị tiền tệ.",null);
   if(t.outstanding()==null||new BigDecimal(t.outstanding()).signum()<=0)return context(t,"NONE","TUITION_PAID","Bạn đã đóng đủ học phí của khoản này.",null);
   for(var o:all)if(!expired(o)&&Set.of("CREATED","PENDING").contains(o.getState())){
     if(!owned(o))return context(t,"SUPPORT","PAYMENT_OTHER_ACCOUNT","Khoản này có phiên thanh toán cần trung tâm kiểm tra.",null);
     if(!o.getMerchantCode().equals(config.getMerchantCode()))return context(t,"SUPPORT","PAYMENT_MERCHANT_CHANGED","Cấu hình đã thay đổi. Trung tâm cần kiểm tra phiên thanh toán cũ.",o);
     if(o.getAmount().compareTo(new BigDecimal(t.outstanding()))>0)return context(t,"SUPPORT","PAYMENT_BALANCE_CHANGED","Số dư đã thay đổi. Vui lòng liên hệ trung tâm nếu đã thanh toán.",o);
     if(!config.isEnabled())return context(t,"UNAVAILABLE","PAYMENT_DISABLED","Thanh toán trực tuyến đang tạm ngừng. Bạn có thể liên hệ trung tâm.",o);
     return context(t,"RESUME","PAYMENT_PENDING","Bạn có một phiên thanh toán còn hiệu lực.",o);
   }
   if(!config.isEnabled())return context(t,"UNAVAILABLE","PAYMENT_DISABLED","Thanh toán trực tuyến đang tạm ngừng. Bạn có thể đóng tại trung tâm.",null);
   return context(t,"PAY",null,"Chọn số tiền và xác nhận để thanh toán qua VNPAY.",null);
 }
 @Transactional(readOnly=true) public Settings settings(){student();return new Settings(config.isEnabled(),"VNPAY",config.protocolTest()?"PROTOCOL_TEST":"SANDBOX",config.isEnabled()?config.protocolTest()?"Mô phỏng giao thức cục bộ — không phải thanh toán VNPAY thật":"VNPAY sandbox — giao dịch thử nghiệm":"Thanh toán trực tuyến chưa được cấu hình; vui lòng liên hệ trung tâm.",config.isEnabled()?java.net.URI.create(config.getCheckoutUrl()).getScheme()+"://"+java.net.URI.create(config.getCheckoutUrl()).getRawAuthority():null);}
 public View create(Create c,String clientIp){student();String fingerprint=fingerprint(c);var old=orders.findByAccountIdAndRequestId(Users.current().accountId(),c.requestId().toString()).orElse(null);if(old!=null){if(!old.getFingerprint().equals(fingerprint))throw PaymentFlowException.conflict("PAYMENT_INTENT_MISMATCH","Yêu cầu này đã gắn với một nội dung thanh toán khác");return own(old.getId());}var proof=finance.detail(c.tuitionId()).tuition();if(!proof.verification().equals("VERIFIED")||proof.cancelled())throw PaymentFlowException.conflict("TUITION_NOT_PAYABLE","Học phí cần được xác minh và đang hiệu lực");if(!config.isEnabled())throw new PaymentFlowException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"PAYMENT_DISABLED","Thanh toán trực tuyến đang tạm ngừng",null);var amount=new BigDecimal(c.amount());if(amount.signum()<=0||amount.movePointRight(2).toBigIntegerExact().toString().length()>12||!"VND".equals(c.currency()))throw new PaymentFlowException(org.springframework.http.HttpStatus.BAD_REQUEST,"PAYMENT_INVALID_AMOUNT","Số tiền thanh toán không hợp lệ",null);var cached=em.find(Tuition.class,c.tuitionId());if(cached!=null)em.detach(cached);var t=tuition.lock(c.tuitionId()).orElseThrow(ApiException::missing);em.refresh(t);if(!Objects.equals(t.getStudentId(),Users.current().profileId())||!Objects.equals(t.getEnrollmentId(),proof.billingRootId())||!Objects.equals(t.getBranchId(),proof.branchId()))throw ApiException.missing();old=orders.findByAccountIdAndRequestId(Users.current().accountId(),c.requestId().toString()).orElse(null);if(old!=null){if(!old.getFingerprint().equals(fingerprint))throw PaymentFlowException.conflict("PAYMENT_INTENT_MISMATCH","Yêu cầu này đã gắn với một nội dung thanh toán khác");return own(old.getId());}if(t.getStatus()==Tuition.Status.CANCELLED||!"VND".equals(t.getCurrency()))throw PaymentFlowException.conflict("TUITION_NOT_PAYABLE","Khoản học phí không thể thanh toán");try{ledger.requireSynced(t);}catch(ApiException ex){if(ex.getStatus()==org.springframework.http.HttpStatus.CONFLICT)throw PaymentFlowException.conflict("PAYMENT_REVIEW_REQUIRED","Sổ thu đang cần trung tâm kiểm tra. Vui lòng chưa thanh toán tiếp");throw ex;}if(amount.compareTo(t.getAmount().subtract(ledger.net(t.getId())))>0)throw PaymentFlowException.conflict("PAYMENT_BALANCE_CHANGED","Số còn phải đóng đã thay đổi. Vui lòng kiểm tra lại");for(var pending:orders.findByTuitionIdOrderByCreatedAtDesc(t.getId())){if(pending.getReviewCode()!=null||pending.getState().equals("REVIEW_REQUIRED"))throw new PaymentFlowException(org.springframework.http.HttpStatus.CONFLICT,"PAYMENT_REVIEW_REQUIRED","Trung tâm cần kiểm tra giao dịch trước",owned(pending)?pending.getId():null);if(expired(pending)){pending.setState("EXPIRED");pending.setUpdatedAt(now());var expiry=new PaymentOutbox();expiry.setEventKey("PAYMENT_EXPIRED:"+pending.getId());expiry.setType("PAYMENT_EXPIRED");expiry.setOrderId(pending.getId());expiry.setStudentId(pending.getStudentId());expiry.setCreatedAt(now());em.persist(expiry);}else if(Set.of("CREATED","PENDING","REVIEW_REQUIRED").contains(pending.getState())||pending.getReviewCode()!=null)throw new PaymentFlowException(org.springframework.http.HttpStatus.CONFLICT,"PAYMENT_ALREADY_PENDING","Bạn đã có phiên thanh toán đang chờ",owned(pending)?pending.getId():null);}long held=em.createQuery("select count(p) from ProviderTransaction p,OnlineOrder o where p.orderId=o.id and o.tuitionId=:tuition and p.state='REVIEW_REQUIRED'",Long.class).setParameter("tuition",t.getId()).getSingleResult();if(held>0)throw PaymentFlowException.conflict("PAYMENT_REVIEW_REQUIRED","Trung tâm cần kiểm tra giao dịch trước khi thanh toán tiếp");em.flush();if(clientIp==null||!clientIp.matches("[0-9a-fA-F:.]{3,45}"))throw ApiException.bad("Invalid client address");var o=new OnlineOrder();o.setId(UUID.randomUUID().toString());o.setAccountId(Users.current().accountId());o.setStudentId(Users.current().profileId());o.setTuitionId(t.getId());o.setBillingRootId(proof.billingRootId());o.setBranchId(t.getBranchId());o.setRequestId(c.requestId().toString());o.setFingerprint(fingerprint);o.setMerchantRef(UUID.randomUUID().toString().replace("-",""));o.setMerchantCode(config.getMerchantCode());o.setAmount(amount.setScale(2));o.setEnvironment(config.protocolTest()?"PROTOCOL_TEST":"SANDBOX");o.setClientIp(clientIp);o.setCreatedAt(now().withNano(0));o.setUpdatedAt(o.getCreatedAt());o.setExpiresAt(o.getCreatedAt().plusMinutes(config.getTtlMinutes()));o.setState("PENDING");orders.saveAndFlush(o);return view(o,true,proof,true);}
 @Transactional(readOnly=true) public View own(String id){student();var o=orders.findById(id).orElseThrow(ApiException::missing);if(!Objects.equals(o.getAccountId(),Users.current().accountId())||!Objects.equals(o.getStudentId(),Users.current().profileId()))throw ApiException.missing();return view(o,true,optionalLabel(o),true);}
 @Transactional(readOnly=true) public View recover(UUID request){student();var o=orders.findByAccountIdAndRequestId(Users.current().accountId(),request.toString()).orElseThrow(ApiException::missing);return own(o.getId());}
 @Transactional(readOnly=true) public OnlineOrder operatorOrder(String id){operator();var o=orders.findById(id).orElseThrow(ApiException::missing);scope.check(o.getBranchId());return o;}
 @Transactional(readOnly=true) public View operatorDetail(String id){return view(operatorOrder(id),false);}
 @Transactional(readOnly=true) public PageResponse<View> list(boolean student,int page,int size,String state,Long tuitionId,Long branch){return list(student,page,size,state,tuitionId,branch,false);}
 @Transactional(readOnly=true) public PageResponse<View> list(boolean student,int page,int size,String state,Long tuitionId,Long branch,boolean summary){return search(student,page,size,state,tuitionId,branch,summary,null,null,null,null);}
 @Transactional(readOnly=true) public PageResponse<View> search(boolean student,int page,int size,String state,Long tuitionId,Long branch,boolean summary,String keyword,Long studentId,Long classId,Long receiptId){return search(student,page,size,state,tuitionId,branch,summary,keyword,studentId,classId,receiptId,false);}
 @Transactional(readOnly=true) public PageResponse<View> search(boolean student,int page,int size,String state,Long tuitionId,Long branch,boolean summary,String keyword,Long studentId,Long classId,Long receiptId,boolean allAssigned){
   if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw ApiException.bad("Invalid page");
   if(state!=null&&!Set.of("CREATED","PENDING","SUCCEEDED","FAILED","EXPIRED","REVIEW_REQUIRED").contains(state))throw ApiException.bad("Invalid state");
   java.util.Set<Long> branches=null;
   if(student){student();if(allAssigned)throw ApiException.forbidden();if(tuitionId!=null)finance.detail(tuitionId);}else{operator();branches=scope.selection(branch,allAssigned);}
   if(keyword!=null&&keyword.length()>120)throw ApiException.bad("Keyword too long");
   for(var id:Arrays.asList(studentId,classId,receiptId,tuitionId))if(id!=null&&id<=0)throw ApiException.bad("Invalid filter id");
   String where=student?"o.accountId=:account and o.studentId=:student":"1=1";
   var params=new HashMap<String,Object>();
   if(student){params.put("account",Users.current().accountId());params.put("student",Users.current().profileId());}
   if(!student&&branches!=null){if(branches.isEmpty())where+=" and 1=0";else{where+=" and o.branchId in :branches";params.put("branches",branches);}}
   if(tuitionId!=null){where+=" and o.tuitionId=:tuition";params.put("tuition",tuitionId);}
   if(!student){
     if(studentId!=null){where+=" and o.studentId=:studentFilter";params.put("studentFilter",studentId);}
     if(receiptId!=null){where+=" and o.receiptId=:receiptFilter";params.put("receiptFilter",receiptId);}
     if(classId!=null){where+=" and exists(select t.id from Tuition t where t.id=o.tuitionId and t.branchId=o.branchId and t.studentId=o.studentId and t.classId=:classFilter)";params.put("classFilter",classId);}
     if(keyword!=null&&!keyword.isBlank()){
       String pattern="%"+keyword.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
       String code=keyword.trim().toLowerCase(Locale.ROOT);if(code.startsWith("gd-"))code=code.substring(3);
       params.put("orderKeyword","%"+code.replace("!","!!").replace("%","!%").replace("_","!_")+"%");
       where+=" and (lower(o.id) like :orderKeyword escape '!' or lower(o.merchantRef) like :keyword escape '!' or exists(select p.id from ProviderTransaction p where p.orderId=o.id and lower(p.transactionNo) like :keyword escape '!') or exists(select t.id from Tuition t where t.id=o.tuitionId and t.branchId=o.branchId and t.studentId=o.studentId and (lower(t.studentName) like :keyword escape '!' or lower(t.className) like :keyword escape '!' or lower(t.courseName) like :keyword escape '!')))";params.put("keyword",pattern);
     }
   }
   if(state!=null){
     params.put("now",now());
     String effective="(case when o.state in ('CREATED','PENDING') and o.expiresAt<:now then 'EXPIRED' else o.state end)";
     where+=" and ("+effective+"=:state"+(state.equals("REVIEW_REQUIRED")?" or o.reviewCode is not null":"")+")";params.put("state",state);
   }
   var q=em.createQuery("select o from OnlineOrder o where "+where+" order by o.createdAt desc,o.id desc",OnlineOrder.class);
   var count=em.createQuery("select count(o) from OnlineOrder o where "+where,Long.class);
   params.forEach((k,v)->{q.setParameter(k,v);count.setParameter(k,v);});
   var rows=q.setFirstResult(page*size).setMaxResults(size).getResultList();long total=count.getSingleResult();
   var labels=student?optionalLabels(rows.stream().map(OnlineOrder::getTuitionId).distinct().toList()):Map.<Long,StudentFinanceService.TuitionView>of();
   if(!student&&!rows.isEmpty())em.createQuery("select t from Tuition t where t.id in :ids",Tuition.class).setParameter("ids",rows.stream().map(OnlineOrder::getTuitionId).distinct().toList()).getResultList();
   return new PageResponse<>(rows.stream().map(o->view(o,student,labels.get(o.getTuitionId()),!summary)).toList(),page,size,total,(int)Math.ceil((double)total/size));
 }
 public record EventView(Long id,String source,String merchantRef,String transactionNo,String amount,String outcome,String status,String resultCode,String orderId,LocalDateTime receivedAt,LocalDateTime processedAt) {}
 @Transactional(readOnly=true) public PageResponse<EventView> events(int page,int size){operator();if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw ApiException.bad("Invalid page");boolean all=Users.current().role()==Role.SUPER_ADMIN;var ids=Users.current().branchIds();if(!all&&ids.isEmpty())return new PageResponse<>(List.of(),page,size,0,0);String where=all?"1=1":"e.branchId in :branches";var query=em.createQuery("select e from ProviderEvent e where "+where+" order by e.id desc",ProviderEvent.class);var count=em.createQuery("select count(e) from ProviderEvent e where "+where,Long.class);if(!all){query.setParameter("branches",ids);count.setParameter("branches",ids);}long total=count.getSingleResult();return new PageResponse<>(query.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(e->new EventView(e.getId(),e.getSource(),e.getMerchantRef(),e.getTransactionNo(),StudentFinanceService.money(e.getAmount()),e.getOutcome(),e.getStatus(),e.getResultCode(),e.getOrderId(),e.getReceivedAt(),e.getProcessedAt())).toList(),page,size,total,(int)Math.ceil((double)total/size));}
}
