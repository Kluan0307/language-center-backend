package com.example.teaching.publication;
import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class GradePublicationService {
 private final ExamService exams;
 private final ExamRepository examRepository;
 private final ExamCandidateRepository candidates;
 private final GradeRepository grades;
 private final GradePublicationRepository publications;
 private final AcademicDirectory academic;
 private final EntityManager em;
 private final JsonMapper json;
 public record Command(@NotNull UUID requestId,@Min(0) long expectedRevision,@NotBlank @Size(max=64) String draftFingerprint,@NotBlank @Size(max=500) String reason) {}
 public record State(long revision,boolean published,LocalDateTime publishedAt,int candidates,int graded,String draftFingerprint,boolean draftChanged) {}
 private void operator(){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.TEACHER).contains(Users.current().role()))throw ApiException.forbidden();}
 private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private String draft(Exam e){var cs=candidates.findByExamIdOrderByStudentNameAsc(e.getId()).stream().sorted(Comparator.comparing(ExamCandidate::getStudentId)).map(c->Arrays.asList(c.getStudentId(),c.getEnrollmentId(),c.getSource())).toList();var gs=grades.findByExamId(e.getId()).stream().sorted(Comparator.comparing(Grade::getStudentId)).map(g->Arrays.asList(g.getStudentId(),g.getScore().stripTrailingZeros(),g.getStudentComment(),g.getVersion())).toList();return hash(Arrays.asList(e.getName(),e.getExamType(),e.getExamDate(),e.getMaxScore().stripTrailingZeros(),e.getWeight().stripTrailingZeros(),e.getRosterFrozenAt()!=null,cs,gs));}
 private State state(Exam e){var p=publications.findFirstByExamIdOrderByRevisionDesc(e.getId()).orElse(null);var cs=candidates.findByExamIdOrderByStudentNameAsc(e.getId());String fingerprint=draft(e);boolean visible=p!=null&&p.getAction().equals("PUBLISH");return new State(p==null?0:p.getRevision(),visible,visible?p.getPublishedAt():null,cs.size(),grades.findByExamId(e.getId()).size(),fingerprint,visible&&!fingerprint.equals(p.getDraftFingerprint()));}
 @Transactional(readOnly=true) public State state(Long id){operator();return state(exams.require(id));}
 @Transactional(readOnly=true) public State recover(Long id,UUID requestId){operator();var e=exams.require(id);var operation=publications.findByRequestId(requestId.toString()).orElseThrow(ApiException::missing);if(!operation.getExam().getId().equals(id))throw ApiException.missing();return state(e);}
 public State apply(Long id,Command c,boolean retract){operator();exams.require(id);var e=examRepository.lock(id).orElseThrow(ApiException::missing);em.refresh(e);exams.require(id);String action=retract?"RETRACT":"PUBLISH";String intent=hash(Arrays.asList(id,action,c.expectedRevision(),c.draftFingerprint(),c.reason().trim()));var replay=publications.findByRequestId(c.requestId().toString()).orElse(null);if(replay!=null){if(!replay.getFingerprint().equals(intent))throw ApiException.conflict("Request id already used for another intent");return state(e);}
  var old=publications.findFirstByExamIdOrderByRevisionDesc(id).orElse(null);long revision=old==null?0:old.getRevision();if(c.expectedRevision()!=revision||!draft(e).equals(c.draftFingerprint()))throw ApiException.conflict("Grades or publication changed; reload before confirming");
  if(retract&&(old==null||!old.getAction().equals("PUBLISH")))throw ApiException.conflict("No current publication");
  var cs=candidates.findByExamIdOrderByStudentNameAsc(id);
  if(!retract){if(e.getRosterFrozenAt()==null||e.getExamDate().isAfter(LocalDate.now(BusinessTime.ZONE))||cs.isEmpty())throw ApiException.conflict("Freeze a non-empty exam roster before publication");var verified=academic.studentsAt(e.getClassId(),e.getExamDate());for(var candidate:cs)if(candidate.getEnrollmentId()==null||verified.stream().noneMatch(s->s.id().equals(candidate.getStudentId())&&candidate.getEnrollmentId().equals(s.enrollmentId())))throw ApiException.conflict("Exam roster contains unresolved enrollment; verify its source first");}
  var p=new GradePublication();p.setExam(e);p.setClassId(e.getClassId());p.setBranchId(e.getBranchId());p.setRevision(revision+1);p.setRequestId(c.requestId().toString());p.setFingerprint(intent);p.setDraftFingerprint(c.draftFingerprint());p.setAction(action);p.setReason(c.reason().trim());p.setPublishedBy(Users.current().accountId());p.setPublishedAt(LocalDateTime.now(BusinessTime.ZONE));p.setExamName(e.getName());p.setExamType(e.getExamType());p.setExamDate(e.getExamDate());p.setMaxScore(e.getMaxScore());p.setWeight(e.getWeight());publications.saveAndFlush(p);
  if(!retract){var all=grades.findByExamId(id);for(var candidate:cs){var g=all.stream().filter(v->v.getStudentId().equals(candidate.getStudentId())).findFirst().orElse(null);var item=new PublishedGrade();item.setPublication(p);item.setStudentId(candidate.getStudentId());item.setEnrollmentId(candidate.getEnrollmentId());item.setScore(g==null?null:g.getScore());item.setStudentComment(g==null?null:g.getStudentComment());em.persist(item);}}
  for(var candidate:cs)if(candidate.getEnrollmentId()!=null)com.example.teaching.notification.StudentEvents.add(em,"teaching:publication:"+p.getId()+":"+candidate.getStudentId(),"GRADES",retract?"GRADES_RETRACTED":"GRADES_PUBLISHED",retract?"Cập nhật công bố điểm":"Điểm học tập đã công bố","Mở hồ sơ học tập để xem thông tin công bố hiện hành.",candidate.getStudentId(),candidate.getEnrollmentId(),e.getClassId(),null,e.getBranchId(),"GRADES",candidate.getEnrollmentId().toString(),null,null);
  em.flush();return state(e);
 }
}
