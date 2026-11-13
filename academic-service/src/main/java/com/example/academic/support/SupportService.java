package com.example.academic.support;
import com.example.academic.common.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;
@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class SupportService {
 @org.springframework.beans.factory.annotation.Value("${services.finance.url}") private String financeUrl;
 private final RemoteApi remote;
 public record FinanceItem(Long branchId,String verification) {}
 public record FinanceDetail(FinanceItem tuition) {}
 public record OrderProof(String id,Long branchId,Long studentId) {}
 private final StudentAccess access; private final StudentLearningService learning; private final SupportRepository requests; private final BranchScope scope; private final EntityManager em; private final JsonMapper json;
 public record Create(@NotNull UUID requestId,@NotNull SupportRequest.Category category,@NotBlank @Size(min=5,max=120) String subject,@NotBlank @Size(min=10,max=2000) String description,String resourceType,@Positive Long resourceId,@Size(max=36) String resourceKey) {
   public Create(UUID requestId,SupportRequest.Category category,String subject,String description,String resourceType,Long resourceId){this(requestId,category,subject,description,resourceType,resourceId,null);}
 }
 public record Message(@NotNull UUID requestId,@Min(0) long version,@NotBlank @Size(max=2000) String message) {}
 public record StaffMessage(@NotNull UUID requestId,@Min(0) long version,@NotBlank @Size(max=2000) String message,boolean internalNote) {}
 public record Close(@NotNull UUID requestId,@Min(0) long version) {}
 public record Change(@NotNull UUID requestId,@Min(0) long version,@NotNull SupportRequest.Status status,@NotBlank @Size(max=500) String reason) {}
 public record Row(Long id,Long studentId,String studentName,Long branchId,SupportRequest.Category category,String subject,SupportRequest.Status status,String resourceType,Long resourceId,String resourceKey,LocalDateTime createdAt,LocalDateTime updatedAt,long version) {}
 public record PublicMessage(Long id,String sender,String message,LocalDateTime createdAt) {}
 public record StaffView(Long id,String sender,String message,boolean internalNote,String actorName,LocalDateTime createdAt) {}
 public record StudentDetail(Row request,List<PublicMessage> messages) {}
 public record StaffDetail(Row request,List<StaffView> messages,List<SupportStatusEvent> statusHistory) {}
 private LocalDateTime now(){return LocalDateTime.now(BusinessTime.ZONE);}
 private void operator(){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.STAFF).contains(Users.current().role()))throw ApiException.forbidden();}
 private SupportRequest require(Long id,boolean student,boolean lock){if(student)access.owner();else operator();var r=(lock?requests.lock(id):requests.findById(id)).orElseThrow(ApiException::missing);if(lock)em.refresh(r);if(student){if(!r.getStudentId().equals(Users.current().profileId()))throw ApiException.missing();}else scope.check(r.getBranchId());return r;}
 private Row row(SupportRequest r,boolean student){var s=em.find(Student.class,r.getStudentId());return new Row(r.getId(),student?null:r.getStudentId(),student?null:s==null?null:s.getFullName(),r.getBranchId(),r.getCategory(),r.getSubject(),r.getStatus(),r.getResourceType(),r.getResourceId(),r.getResourceKey(),r.getCreatedAt(),r.getUpdatedAt(),r.getVersion());}
 @Transactional(readOnly=true) public PageResponse<Row> list(boolean student,int page,int size,SupportRequest.Status status,SupportRequest.Category category,Long branch){return search(student,page,size,status,category,branch,null,null,null);}
 @Transactional(readOnly=true) public PageResponse<Row> search(boolean student,int page,int size,SupportRequest.Status status,SupportRequest.Category category,Long branch,Long studentId,String keyword,String queue){return search(student,page,size,status,category,branch,studentId,keyword,queue,false);}
 @Transactional(readOnly=true) public PageResponse<Row> search(boolean student,int page,int size,SupportRequest.Status status,SupportRequest.Category category,Long branch,Long studentId,String keyword,String queue,boolean allAssigned){
   if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw ApiException.bad("Invalid page");
   if(studentId!=null&&studentId<=0||keyword!=null&&keyword.length()>120||queue!=null&&!queue.equals("ACTIVE"))throw ApiException.bad("Invalid filters");
   Long owner=student?access.owner().getId():null;java.util.Set<Long> branches=null;if(!student){operator();branches=scope.selection(branch,allAssigned);}else if(allAssigned)throw ApiException.forbidden();
   String where=student?"r.studentId=:owner":"1=1";var params=new HashMap<String,Object>();
   if(student)params.put("owner",owner);
   if(!student&&branches!=null){if(branches.isEmpty())where+=" and 1=0";else{where+=" and r.branchId in :branches";params.put("branches",branches);}}
   if(!student&&studentId!=null){where+=" and r.studentId=:student";params.put("student",studentId);}
   if(status!=null){where+=" and r.status=:status";params.put("status",status);}
   if(category!=null){where+=" and r.category=:category";params.put("category",category);}
   if(queue!=null)where+=" and r.status in ('OPEN','IN_PROGRESS')";
   if(keyword!=null&&!keyword.isBlank()){
     where+=" and (lower(r.subject) like :keyword escape '!' or exists(select s.id from Student s where s.id=r.studentId and (lower(s.fullName) like :keyword escape '!' or lower(s.phone) like :keyword escape '!')) or lower(r.resourceKey) like :resourceKeyword escape '!')";
     String code=keyword.trim().toLowerCase(Locale.ROOT);if(code.startsWith("gd-"))code=code.substring(3);
     params.put("resourceKeyword","%"+code.replace("!","!!").replace("%","!%").replace("_","!_")+"%");
     params.put("keyword","%"+keyword.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");
   }
   String priority="case when r.status='OPEN' then 0 when r.status='IN_PROGRESS' then 1 when r.status='RESOLVED' then 2 else 3 end";
   var q=em.createQuery("select r from SupportRequest r where "+where+" order by "+priority+",r.updatedAt desc,r.id desc",SupportRequest.class);
   var c=em.createQuery("select count(r) from SupportRequest r where "+where,Long.class);params.forEach((k,v)->{q.setParameter(k,v);c.setParameter(k,v);});long total=c.getSingleResult();
   var rows=q.setFirstResult(page*size).setMaxResults(size).getResultList();
   if(!rows.isEmpty())em.createQuery("select s from Student s where s.id in :ids",Student.class).setParameter("ids",rows.stream().map(SupportRequest::getStudentId).distinct().toList()).getResultList();
   return new PageResponse<>(rows.stream().map(r->row(r,student)).toList(),page,size,total,(int)Math.ceil((double)total/size));
 }
 private List<SupportMessage> messages(Long id,boolean student){return em.createQuery("select m from SupportMessage m where m.request.id=:id"+(student?" and m.internalNote=false":"")+" order by m.id",SupportMessage.class).setParameter("id",id).getResultList();}
 @Transactional(readOnly=true) public StudentDetail studentDetail(Long id){var r=require(id,true,false);return new StudentDetail(row(r,true),messages(id,true).stream().map(m->new PublicMessage(m.getId(),m.getSender(),m.getMessage(),m.getCreatedAt())).toList());}
 @Transactional(readOnly=true) public StaffDetail staffDetail(Long id){var r=require(id,false,false);return new StaffDetail(row(r,false),messages(id,false).stream().map(m->new StaffView(m.getId(),m.getSender(),m.getMessage(),m.isInternalNote(),m.getActorName(),m.getCreatedAt())).toList(),em.createQuery("select e from SupportStatusEvent e where e.requestId=:id order by e.id",SupportStatusEvent.class).setParameter("id",id).getResultList());}
 private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private SupportOperation replay(UUID uuid,Object intent){var op=em.find(SupportOperation.class,uuid.toString());if(op==null)return null;if(!op.getActorId().equals(Users.current().accountId()))throw ApiException.missing();if(!op.getFingerprint().equals(hash(intent)))throw ApiException.conflict("Request UUID already used for another intent");return op;}
 private void operation(UUID id,Object intent,SupportRequest r,String action){var o=new SupportOperation();o.setId(id.toString());o.setRequest(r);o.setActorId(Users.current().accountId());o.setFingerprint(hash(intent));o.setAction(action);o.setCreatedAt(now());em.persist(o);em.flush();}
 private void message(SupportRequest r,String text,boolean internal,boolean student){var m=new SupportMessage();m.setRequest(r);m.setMessage(text.trim());m.setInternalNote(internal);m.setSender(student?"STUDENT":"CENTER");m.setActorId(Users.current().accountId());m.setActorName(Users.current().username());m.setCreatedAt(now());em.persist(m);r.setUpdatedAt(now());}
 private void statusEvent(SupportRequest r,SupportRequest.Status from,String reason){var e=new SupportStatusEvent();e.setRequestId(r.getId());e.setPreviousStatus(from);e.setStatus(r.getStatus());e.setActorName(Users.current().username());e.setActorId(Users.current().accountId());e.setReason(reason);e.setCreatedAt(now());em.persist(e);}
 private void version(SupportRequest r,long v){if(r.getVersion()!=v)throw ApiException.conflict("Support request changed; reload");if(r.getStatus()==SupportRequest.Status.CLOSED)throw ApiException.conflict("Closed requests cannot be changed");}
 public StudentDetail create(Create c){var s=access.owner();Object intent=Arrays.asList("CREATE",c.category(),c.subject().trim(),c.description().trim(),c.resourceType(),c.resourceId());if(c.resourceKey()!=null)intent=Arrays.asList("CREATE_KEY",intent,c.resourceKey());var old=replay(c.requestId(),intent);if(old!=null)return studentDetail(old.getRequest().getId());if(c.resourceKey()!=null){
   if(!"PAYMENT_ORDER".equals(c.resourceType())||c.resourceId()!=null||!c.resourceKey().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw ApiException.bad("Invalid payment order reference");
 }else if("PAYMENT_ORDER".equals(c.resourceType())||(c.resourceType()==null)!=(c.resourceId()==null))throw ApiException.bad("Resource type and id must be supplied together");Long branch=s.getBranchId();if(c.resourceType()!=null)switch(c.resourceType()){case "PAYMENT_ORDER"->{var proof=remote.get(financeUrl+"/api/v1/finance/student/me/payment-orders/"+c.resourceKey(),OrderProof.class);if(proof.branchId()==null||!Objects.equals(proof.studentId(),s.getId())||!c.resourceKey().equalsIgnoreCase(proof.id()))throw ApiException.missing();branch=proof.branchId();}case "PROFILE"->{if(!c.resourceId().equals(s.getId()))throw ApiException.missing();}case "ENROLLMENT"->branch=access.enrollment(c.resourceId()).getSchoolClass().getBranchId();case "SESSION"->branch=learning.session(c.resourceId()).branch().id();case "TUITION","RECEIPT"->{var proof=remote.get(financeUrl+"/api/v1/finance/student/me/"+(c.resourceType().equals("TUITION")?"tuitions/":"receipts/")+c.resourceId(),FinanceDetail.class);if(proof.tuition()==null||proof.tuition().branchId()==null||!"VERIFIED".equals(proof.tuition().verification()))throw ApiException.conflict("Financial link requires verification; contact the center");branch=proof.tuition().branchId();}default->throw ApiException.bad("Unsupported linked resource");}if(c.subject().trim().length()<5||c.description().trim().length()<10)throw ApiException.bad("Subject or description too short");var r=new SupportRequest();r.setStudentId(s.getId());r.setBranchId(branch);r.setSubject(c.subject().trim());r.setCategory(c.category());r.setResourceType(c.resourceType());r.setResourceId(c.resourceId());r.setResourceKey(c.resourceKey());r.setCreatedAt(now());r.setUpdatedAt(now());requests.saveAndFlush(r);statusEvent(r,null,"Học viên tạo yêu cầu");message(r,c.description(),false,true);operation(c.requestId(),intent,r,"CREATE");return studentDetail(r.getId());}
 public Object reply(Long id,UUID uuid,long expected,String text,boolean internal,boolean student){require(id,student,false);Object intent=Arrays.asList("MESSAGE",id,expected,text.trim(),internal);var old=replay(uuid,intent);if(old!=null)return student?studentDetail(id):staffDetail(id);var r=require(id,student,true);old=replay(uuid,intent);if(old!=null)return student?studentDetail(id):staffDetail(id);version(r,expected);var previous=r.getStatus();message(r,text,internal,student);if(student&&r.getStatus()==SupportRequest.Status.RESOLVED)r.setStatus(SupportRequest.Status.OPEN);if(!student&&!internal&&r.getStatus()==SupportRequest.Status.OPEN)r.setStatus(SupportRequest.Status.IN_PROGRESS);if(previous!=r.getStatus())statusEvent(r,previous,student?"Học viên phản hồi lại":"Trung tâm bắt đầu xử lý");operation(uuid,intent,r,"MESSAGE");if(!student&&!internal)com.example.academic.notification.StudentEvents.add(em,"academic:support:"+uuid,"SUPPORT","SUPPORT_REPLY","Trung tâm đã phản hồi","Yêu cầu hỗ trợ của bạn có phản hồi mới.",r.getStudentId(),null,null,null,r.getBranchId(),"SUPPORT",id.toString(),null,null);return student?studentDetail(id):staffDetail(id);}
 public Object change(Long id,UUID uuid,long expected,SupportRequest.Status next,String reason,boolean student){require(id,student,false);Object intent=Arrays.asList("STATE",id,expected,next,reason);var old=replay(uuid,intent);if(old!=null)return student?studentDetail(id):staffDetail(id);var r=require(id,student,true);old=replay(uuid,intent);if(old!=null)return student?studentDetail(id):staffDetail(id);version(r,expected);boolean allowed=student?next==SupportRequest.Status.CLOSED:switch(r.getStatus()){case OPEN->next==SupportRequest.Status.IN_PROGRESS;case IN_PROGRESS->next==SupportRequest.Status.RESOLVED;case RESOLVED->next==SupportRequest.Status.IN_PROGRESS||next==SupportRequest.Status.CLOSED;default->false;};if(!allowed)throw ApiException.conflict("Invalid support status transition");var previous=r.getStatus();r.setStatus(next);statusEvent(r,previous,reason);message(r,reason,!student,student);operation(uuid,intent,r,"STATE");if(!student)com.example.academic.notification.StudentEvents.add(em,"academic:support:"+uuid,"SUPPORT","SUPPORT_STATUS","Cập nhật yêu cầu hỗ trợ","Trạng thái yêu cầu hỗ trợ đã được cập nhật.",r.getStudentId(),null,null,null,r.getBranchId(),"SUPPORT",id.toString(),null,null);return student?studentDetail(id):staffDetail(id);}
 @Transactional(readOnly=true) public Object recover(UUID uuid,boolean student){if(student)access.owner();else operator();var op=em.find(SupportOperation.class,uuid.toString());if(op==null||!op.getActorId().equals(Users.current().accountId()))throw ApiException.missing();return student?studentDetail(op.getRequest().getId()):staffDetail(op.getRequest().getId());}
}
