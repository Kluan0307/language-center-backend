package com.example.teaching.grading;
import com.example.teaching.common.*;
import com.example.teaching.evaluation.EvaluationLock;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.results.*;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
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
public class GradingCompletionService {
 private final EntityManager em;private final JsonMapper json;private final AcademicDirectory academic;
 private final GradingOutcomeService outcomes;private final GradingWriteGuard guard;
 private final ResultBarrierService barriers;private final EvaluationLock locks;
 private final FinalResultRepository results;private final FinalResultService finals;private final RemoteApi remote;
 @org.springframework.beans.factory.annotation.Value("${services.academic.url}") private String academicUrl;
 public record Command(@NotNull UUID requestId,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String fingerprint,@NotBlank @Size(min=3,max=500) String reason) {}
 public record Preview(Long classId,String fingerprint,List<GradingCalculation.Row> students,List<String> blockers,FinalResultDto.ClassResults current) {}
 private void manager(){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN).contains(Users.current().role()))throw ApiException.forbidden();}
 private String hash(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public Preview preview(Long id){manager();var c=academic.schoolClass(id);var barrier=barriers.lock(id,c.branchId());var state=locks.lockForResult(id,c.branchId());var p=outcomes.preview(id);var release=guard.latest(id);var blockers=new ArrayList<String>();
  if(Boolean.TRUE.equals(c.closed()))blockers.add("Lớp đã đóng; cần mở lại trước khi chốt phiên bản mới");
  var proof=remote.get(academicUrl+"/api/v1/academic/classes/"+id+"/result-source",GradingOutcomeService.Source.class);
  if(!Objects.equals(proof.token(),barrier.getToken())||proof.sourceRevision()!=barrier.getSourceRevision())blockers.add("Nguồn Academic và Teaching chưa dùng chung phiên bản điều phối kết quả");
  if(em.createQuery("select count(a) from GradingActivity a where a.classId=:id and a.assessmentDate>:today",Long.class).setParameter("id",id).setParameter("today",java.time.LocalDate.now(BusinessTime.ZONE)).getSingleResult()>0)blockers.add("Còn bài đánh giá tương lai; kiểm tra lịch trước khi chốt kết thúc khóa");
  if(release==null||!release.getState().equals("PUBLISHED"))blockers.add("Phải duyệt và công bố kết quả trước khi chốt hoàn thành");else if(!release.getFingerprint().equals(p.fingerprint()))blockers.add("Nguồn đã đổi sau công bố; cần kiểm tra và công bố lại");
  var context=academic.evaluationContext(id,true);var active=context.enrollments().stream().filter(l->Set.of("STUDYING","COMPLETED").contains(l.studyStatus())).map(l->l.enrollmentId()).toList();var rows=p.report().students().stream().filter(r->active.contains(r.enrollmentId())).toList();
  if(rows.isEmpty())blockers.add("Không có đăng ký đang học/hoàn thành cần chốt");
  for(var row:rows)if(!Set.of("PASS","FAIL","RECORDED","NEEDS_SUPPORT","DEVELOPING","MEETS_EXPECTATIONS").contains(row.completionStatus()))blockers.add("Chưa đủ điều kiện kết thúc đăng ký #"+row.enrollmentId());
  if(barrier.getConfirmedCount()>0)blockers.add("Lớp đã có kết quả chính thức; mở lại nếu cần thay phiên bản");
  String fp=hash(Arrays.asList(p.fingerprint(),release==null?null:release.getId(),barrier.getSourceRevision(),barrier.getResultRevision(),state.getSourceRevision(),rows));
  return new Preview(id,fp,rows,List.copyOf(blockers),finals.classResults(id));
 }
 public FinalResultDto.ClassResults confirm(Long id,Command command){manager();var c=academic.schoolClass(id);var barrier=barriers.lock(id,c.branchId());String intent=hash(List.of(id,command.fingerprint(),command.reason().trim()));var old=em.find(GradingCompletionBatch.class,command.requestId().toString());if(old!=null){if(!old.getIntent().equals(intent)||!old.getClassId().equals(id))throw ApiException.conflict("Mã yêu cầu đã dùng cho ý định khác");return finals.classResults(id);}
  var p=preview(id);if(!p.blockers().isEmpty())throw ApiException.conflict(String.join("; ",p.blockers()));if(!p.fingerprint().equals(command.fingerprint()))throw ApiException.conflict("Nguồn hoàn thành đã thay đổi; xem trước lại");barrier=barriers.lock(id,c.branchId());var release=guard.latest(id);var state=locks.lockForResult(id,c.branchId());var binding=em.find(GradingBinding.class,id);
  for(var row:p.students()){
   var previous=results.findByEnrollmentIdOrderByRevisionDesc(row.enrollmentId());var r=new FinalResult();r.setClassId(id);r.setBranchId(c.branchId());r.setEnrollmentId(row.enrollmentId());r.setStudentId(row.studentId());r.setRevision(previous.isEmpty()?1:previous.getFirst().getRevision()+1);r.setPolicyRevision(binding.getRevision());r.setAcademicSourceRevision(barrier.getSourceRevision());r.setTeachingSourceRevision(state.getSourceRevision());r.setGradingReleaseId(release.getId());r.setRequestId(UUID.nameUUIDFromBytes((command.requestId()+":"+row.enrollmentId()).getBytes(StandardCharsets.UTF_8)).toString());r.setFingerprint(p.fingerprint());r.setOutcome(Set.of("PASS","FAIL").contains(row.academicStatus())?row.academicStatus():"ASSESSED");r.setState("CONFIRMED");r.setReason(command.reason().trim());r.setConfirmedBy(Users.current().accountId());r.setConfirmedByName(Users.current().username());r.setConfirmedAt(LocalDateTime.now(BusinessTime.ZONE));r.setSnapshot(json.writeValueAsString(Map.of("gradingReleaseId",release.getId(),"row",row,"previewFingerprint",p.fingerprint())));results.saveAndFlush(r);barrier.setConfirmedCount(barrier.getConfirmedCount()+1);
   com.example.teaching.notification.StudentEvents.add(em,"grading:completion:"+r.getId(),"GRADES","RESULT_CONFIRMED","Kết quả cuối khóa đã xác nhận","Mở hồ sơ học tập để xem kết quả hiện hành.",row.studentId(),row.enrollmentId(),id,binding.getCourseId(),c.branchId(),"GRADES",row.enrollmentId().toString(),null,null);
  }
  barrier.setResultRevision(barrier.getResultRevision()+1);var batch=new GradingCompletionBatch();batch.setRequestId(command.requestId().toString());batch.setClassId(id);batch.setReleaseId(release.getId());batch.setIntent(intent);batch.setActorId(Users.current().accountId());batch.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));batch.setReason(command.reason().trim());batch.setSnapshot(json.writeValueAsString(p));em.persist(batch);em.flush();return finals.classResults(id);
 }
}
