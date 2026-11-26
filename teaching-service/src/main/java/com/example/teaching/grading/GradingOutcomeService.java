package com.example.teaching.grading;

import com.example.teaching.common.*;
import com.example.teaching.evaluation.AcademicEvaluationContext;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.results.ResultBarrierService;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static com.example.teaching.grading.GradingModels.*;

@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class GradingOutcomeService {
 private final EntityManager em;private final JsonMapper json;private final GradingService grading;
 private final AcademicDirectory academic;private final RemoteApi remote;
 private final ResultBarrierService barriers;private final GradingWriteGuard guard;
 private final com.example.teaching.results.FinalResultService finalResults;
 @Value("${services.academic.url}") private String academicUrl;
 public enum Action { SUBMIT,APPROVE,REJECT,PUBLISH,RETRACT,REOPEN }
 public record Command(@NotNull UUID requestId,@NotNull @PositiveOrZero Long expectedRevision,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String fingerprint,@NotBlank @Size(min=3,max=500) String reason) {}
 public record Source(Long classId,String token,long sourceRevision) {}
 public record Workflow(String state,long revision,Long submittedBy,Long actorId,LocalDateTime occurredAt,String reason,boolean sourceChanged) {}
 public record Preview(GradingCalculation.Report report,String fingerprint,Workflow workflow) {}
 public record StudentView(String state,Long revision,LocalDateTime publishedAt,String templateName,GradingCalculation.Row result,List<String> rules) {}
 private String hash(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private Workflow view(GradingRelease r,String fingerprint){return r==null?new Workflow("DRAFT",0,null,null,null,null,false):new Workflow(r.getState(),r.getRevision(),r.getSubmittedBy(),r.getActorId(),r.getOccurredAt(),r.getReason(),!r.getFingerprint().equals(fingerprint));}
 private List<GradingActivity> activities(Long id){return em.createQuery("select a from GradingActivity a where a.classId=:id order by a.assessmentDate,a.id",GradingActivity.class).setParameter("id",id).getResultList();}
 private List<GradingMark> marks(Long id){return em.createQuery("select m from GradingMark m where m.activityId in (select a.id from GradingActivity a where a.classId=:id) order by m.activityId,m.studentId,m.component",GradingMark.class).setParameter("id",id).getResultList();}
 @Transactional(readOnly=true) public Preview preview(Long classId){
  var binding=grading.binding(classId);if(binding==null)throw ApiException.conflict("Lớp chưa áp dụng quy chế");
  var context=academic.evaluationContext(classId,binding.spec().minAttendancePercent()!=null);
  if(context==null||!Objects.equals(context.classId(),classId)||!Objects.equals(context.branchId(),binding.branchId())||context.enrollments()==null||context.sessions()==null||context.attendance()==null)throw ApiException.bad("Nguồn học vụ chưa đầy đủ để tính kết quả");
  var source=remote.get(academicUrl+"/api/v1/academic/classes/"+classId+"/result-source",Source.class);if(source==null||source.classId()!=null&&!Objects.equals(source.classId(),classId)||source.token()==null)throw ApiException.bad("Nguồn phiên bản học vụ không hợp lệ");
  var all=activities(classId);var marks=marks(classId);var b=em.find(GradingBinding.class,classId);var today=LocalDate.now(BusinessTime.ZONE);
  var report=GradingCalculation.calculate(b,binding.spec(),context,all,marks,today);
  var latestDate=java.util.stream.Stream.concat(all.stream().map(GradingActivity::getAssessmentDate),context.sessions().stream().map(AcademicEvaluationContext.Session::sessionDate)).max(Comparator.naturalOrder()).orElse(today);
  var learners=context.enrollments().stream().sorted(Comparator.comparing(AcademicEvaluationContext.Learner::enrollmentId)).map(l->Arrays.asList(l.enrollmentId(),l.studentId(),l.studentName(),Set.of("STUDYING","COMPLETED").contains(l.studyStatus())?"ENROLLED":l.studyStatus(),(l.periods()==null?java.util.stream.Stream.<AcademicEvaluationContext.Period>empty():l.periods().stream()).sorted(Comparator.comparing(AcademicEvaluationContext.Period::activeFrom,Comparator.nullsFirst(Comparator.naturalOrder()))).map(p->Arrays.asList(p.activeFrom(),p.activeUntil()==null||p.activeUntil().isAfter(latestDate)?null:p.activeUntil())).toList())).toList();
  String fingerprint=hash(Arrays.asList(binding,all,marks,learners,context.sessions(),context.attendance(),context.duration(),source,report.calculationVersion(),report.students(),report.rules()));
  return new Preview(report,fingerprint,view(guard.latest(classId),fingerprint));
 }
 @Transactional(readOnly=true) public List<Workflow> history(Long id){grading.binding(id);return em.createQuery("select r from GradingRelease r where r.classId=:id order by r.revision desc",GradingRelease.class).setParameter("id",id).setMaxResults(100).getResultList().stream().map(r->view(r,r.getFingerprint())).toList();}
 public Preview action(Long id,Action action,Command command){
  var binding=grading.binding(id);if(binding==null)throw ApiException.conflict("Chưa có quy chế lớp");var barrier=barriers.lock(id,binding.branchId());
  boolean manager=Set.of(Role.SUPER_ADMIN,Role.ADMIN).contains(Users.current().role());if(action!=Action.SUBMIT&&!manager)throw ApiException.forbidden();
  if(command.reason()==null||command.reason().trim().length()<3||command.reason().trim().length()>500)throw ApiException.bad("Nhập lý do 3–500 ký tự");
  String intent=hash(List.of(id,action,command.expectedRevision(),command.fingerprint(),command.reason().trim()));
  var replay=em.createQuery("select r from GradingRelease r where r.requestId=:id",GradingRelease.class).setParameter("id",command.requestId().toString()).getResultStream().findFirst().orElse(null);
  if(replay!=null){if(!replay.getIntent().equals(intent))throw ApiException.conflict("Mã yêu cầu đã được dùng cho thao tác khác");return preview(id);}
  var current=preview(id);var old=guard.latest(id);String state=old==null?"DRAFT":old.getState();long revision=old==null?0:old.getRevision();
  if(revision!=command.expectedRevision()||!current.fingerprint().equals(command.fingerprint()))throw ApiException.conflict("Nguồn điểm hoặc trạng thái đã thay đổi; tải lại và kiểm tra trước khi xác nhận");
  if(barrier.getConfirmedCount()>0){
   if(action!=Action.REOPEN&&action!=Action.RETRACT)throw ApiException.conflict("Đã chốt kết quả chính thức; cần mở lại hoặc thu hồi có lý do");
   finalResults.reopen(id,new com.example.teaching.results.FinalResultDto.Reopen(UUID.nameUUIDFromBytes((command.requestId()+":final-reopen").getBytes(StandardCharsets.UTF_8)).toString(),barrier.getResultRevision(),command.reason()));
  }
  String next=switch(action){
   case SUBMIT->{if(!Set.of("DRAFT","REJECTED").contains(state))throw ApiException.conflict("Chỉ gửi duyệt từ bản nháp hoặc bản trả lại");if(current.report().students().isEmpty()||marks(id).isEmpty())throw ApiException.conflict("Chưa có kết quả học viên để gửi duyệt");
    for(var a:activities(id))if(!a.getAssessmentDate().isAfter(LocalDate.now(BusinessTime.ZONE))){var sheet=grading.sheet(id,a.getId());for(var student:sheet.students())for(var code:a.getComponents().split(","))if(sheet.records().stream().noneMatch(m->m.studentId().equals(student.studentId())&&m.component().equals(code)))throw ApiException.conflict("Còn ô chưa nhập ở bài "+a.getName()+"; nhập kết quả hoặc trạng thái vắng/miễn/chưa nộp");}
    yield "SUBMITTED";}
   case APPROVE->{if(!state.equals("SUBMITTED"))throw ApiException.conflict("Chưa có bản gửi duyệt");if(old.getSubmittedBy().equals(Users.current().accountId()))throw ApiException.forbidden();if(!old.getFingerprint().equals(current.fingerprint()))throw ApiException.conflict("Nguồn đã đổi sau gửi duyệt; trả lại/mở lại để tính lại");yield "APPROVED";}
   case REJECT->{if(!Set.of("SUBMITTED","APPROVED").contains(state))throw ApiException.conflict("Không có bản chờ duyệt để trả lại");yield "REJECTED";}
   case PUBLISH->{if(!state.equals("APPROVED"))throw ApiException.conflict("Phải duyệt trước khi công bố");if(!old.getFingerprint().equals(current.fingerprint()))throw ApiException.conflict("Nguồn đã thay đổi sau duyệt; phải duyệt lại");yield "PUBLISHED";}
   case RETRACT->{if(!state.equals("PUBLISHED"))throw ApiException.conflict("Không có công bố hiện hành");yield "RETRACTED";}
   case REOPEN->{if(!Set.of("SUBMITTED","APPROVED","PUBLISHED","RETRACTED").contains(state))throw ApiException.conflict("Bản hiện tại đã cho phép chỉnh sửa");yield "DRAFT";}
  };
  var r=new GradingRelease();r.setClassId(id);r.setRevision(revision+1);r.setState(next);r.setAction(action.name());r.setRequestId(command.requestId().toString());r.setIntent(intent);r.setFingerprint(current.fingerprint());r.setSnapshot(action==Action.SUBMIT||old==null?json.writeValueAsString(current.report()):old.getSnapshot());r.setActorId(Users.current().accountId());r.setSubmittedBy(action==Action.SUBMIT?Users.current().accountId():old.getSubmittedBy());r.setReason(command.reason().trim());r.setOccurredAt(LocalDateTime.now(BusinessTime.ZONE));em.persist(r);em.flush();
  if(action==Action.PUBLISH||action==Action.RETRACT||action==Action.REOPEN&&state.equals("PUBLISHED")){
   var report=json.readValue(r.getSnapshot(),GradingCalculation.Report.class);for(var row:report.students())com.example.teaching.notification.StudentEvents.add(em,"grading:release:"+r.getId()+":"+row.enrollmentId(),"GRADES",action==Action.PUBLISH?"GRADES_PUBLISHED":"GRADES_RETRACTED",action==Action.PUBLISH?"Kết quả đánh giá đã công bố":"Kết quả đánh giá đang cập nhật","Mở mục điểm số để xem công bố hiện hành.",row.studentId(),row.enrollmentId(),id,binding.courseId(),binding.branchId(),"GRADES",row.enrollmentId().toString(),null,null);
  }
  return new Preview(current.report(),current.fingerprint(),view(r,current.fingerprint()));
 }
 @Transactional(readOnly=true) public StudentView student(Long enrollment){
  if(Users.current().role()!=Role.STUDENT)throw ApiException.forbidden();var source=remote.get(academicUrl+"/api/v1/academic/student/me/enrollments/"+enrollment+"/result-source",Source.class);
  return published(source.classId(),enrollment,Users.current().profileId());
 }
 public StudentView published(Long classId,Long enrollment,Long studentId){
  var release=guard.latest(classId);if(release==null||!release.getState().equals("PUBLISHED"))return new StudentView(release==null?"PENDING":"UPDATING",null,null,null,null,List.of());
  var report=json.readValue(release.getSnapshot(),GradingCalculation.Report.class);var row=report.students().stream().filter(x->x.enrollmentId().equals(enrollment)&&x.studentId().equals(studentId)).findFirst().orElse(null);
  if(row==null)return new StudentView("PENDING",null,null,null,null,List.of());
  return new StudentView("PUBLISHED",release.getRevision(),release.getOccurredAt(),report.schemeName(),row,report.rules());
 }
}
