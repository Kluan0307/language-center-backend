package com.example.teaching.grading;

import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.results.ResultBarrierService;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static com.example.teaching.grading.GradingModels.*;

@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class GradingLegacyService {
 private final EntityManager em;private final JsonMapper json;private final AcademicDirectory academic;
 private final BranchScope scope;private final ResultBarrierService barriers;private final GradingService grading;
 private final ExamRepository exams;private final GradeRepository grades;
 public record Mapping(@NotNull @Positive Long examId,@NotBlank String component) {}
 public record Plan(@NotBlank String templateId,@NotEmpty @Size(max=200) List<@NotNull @Valid Mapping> mappings) {}
 public record Apply(@NotNull UUID requestId,@NotNull @Valid Plan plan,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String fingerprint,@NotBlank @Size(min=3,max=500) String reason) {}
 public record LegacyExam(Long id,String name,String examType,LocalDate date,java.math.BigDecimal maxScore,long grades,long publishedVersions) {}
 public record Source(Long classId,List<LegacyExam> exams,boolean alreadyMigrated,List<String> notices) {}
 public record Preview(Long classId,String templateId,String fingerprint,List<String> blockers,int exams,int grades,GradingCalculation.Report proposed) {}
 public record Imported(Long id,Long classId,String templateId,int exams,int grades,LocalDateTime createdAt) {}
 private AcademicDirectory.SchoolClass school(Long id){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN).contains(Users.current().role()))throw ApiException.forbidden();var c=academic.schoolClass(id);scope.check(c.branchId());return c;}
 private String hash(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional(readOnly=true) public Source source(Long id){school(id);var all=exams.findByClassId(id).stream().sorted(Comparator.comparing(Exam::getId)).toList();return new Source(id,all.stream().map(e->new LegacyExam(e.getId(),e.getName(),e.getExamType(),e.getExamDate(),e.getMaxScore(),grades.findByExamId(e.getId()).size(),em.createQuery("select count(p) from GradePublication p where p.exam.id=:id",Long.class).setParameter("id",e.getId()).getSingleResult())).toList(),em.find(GradingBinding.class,id)!=null,List.of("Chỉ đối chiếu vào quy chế điểm nội bộ, không tự tách điểm tổng IELTS/TOEIC thành kỹ năng.","Sao chép điểm cùng thang, chỉ mang theo nhận xét dành cho học viên; điểm và công bố cũ được giữ nguyên.","Kết quả mới là bản nháp, phải kiểm tra, gửi duyệt và công bố lại."));}
 public Preview preview(Long id,Plan plan){var c=school(id);var barrier=barriers.lock(id,c.branchId());return calculate(id,plan,c,barrier.getConfirmedCount());}
 private Preview calculate(Long id,Plan plan,AcademicDirectory.SchoolClass school,long confirmed){
  var blockers=new ArrayList<String>();var template=em.find(GradingTemplate.class,plan.templateId());if(template==null)throw ApiException.missing();var spec=json.readValue(template.getSpecification(),Spec.class);
  if(spec.kind()!=Kind.INTERNAL)blockers.add("Chỉ chuyển vào quy chế điểm nội bộ; không suy ra kỹ năng hoặc nhận xét từ điểm tổng cũ");
  if(Boolean.TRUE.equals(school.closed()))blockers.add("Lớp đã đóng; cần mở lại có lý do trước khi chuyển");
  if(em.find(GradingBinding.class,id)!=null)blockers.add("Lớp đã có quy chế mới, không được nhập lại");if(confirmed>0)blockers.add("Đang có kết quả chính thức; mở lại và đối chiếu trước khi chuyển");
  var all=exams.findByClassId(id).stream().sorted(Comparator.comparing(Exam::getId)).toList();var old=grades.findByExamClassId(id).stream().sorted(Comparator.comparing(Grade::getId)).toList();
  var mapping=new HashMap<Long,String>();for(var m:plan.mappings())if(mapping.put(m.examId(),m.component())!=null)blockers.add("Một bài cũ chỉ được ánh xạ một lần");
  if(all.isEmpty())blockers.add("Lớp không có bài cũ để chuyển");if(!mapping.keySet().equals(new HashSet<>(all.stream().map(Exam::getId).toList())))blockers.add("Phải ánh xạ đầy đủ, không bỏ hoặc thêm bài không thuộc lớp");
  var activities=new ArrayList<GradingActivity>();var marks=new ArrayList<GradingMark>();var sourceRows=new ArrayList<Object>();
  for(var e:all){var component=spec.components().stream().filter(p->p.code().equals(mapping.get(e.getId()))).findFirst().orElse(null);var roster=academic.studentsAt(id,e.getExamDate());
   if(component==null){blockers.add("Chưa chọn thành phần hợp lệ: "+e.getName());continue;}
   if(spec.kind()!=Kind.INTERNAL||e.getMaxScore().compareTo(component.maxScore())!=0)blockers.add("Khác thang điểm, không tự quy đổi: "+e.getName());
   var a=new GradingActivity();a.setId(e.getId());a.setClassId(id);a.setName(e.getName());a.setAssessmentDate(e.getExamDate());a.setComponents(component.code());a.setCreatedBy(Users.current().accountId());
   a.setRosterFrozenAt(LocalDateTime.of(2000,1,1,0,0));for(var student:roster)if(student.enrollmentId()!=null)a.getCandidates().add(new GradingActivity.Candidate(student.id(),student.fullName(),student.enrollmentId()));
   if(roster.stream().anyMatch(s->s.enrollmentId()==null)||roster.stream().map(AcademicDirectory.Student::id).distinct().count()!=roster.size())blockers.add("Danh sách ngày thi chưa xác minh được đăng ký: "+e.getName());
   for(var g:old.stream().filter(g->g.getExam().getId().equals(e.getId())).toList()){
    if(roster.stream().noneMatch(s->s.id().equals(g.getStudentId())))blockers.add("Có điểm cũ ngoài danh sách xác minh tại ngày thi: "+e.getName());
    var m=new GradingMark();m.setId(g.getId());m.setActivityId(e.getId());m.setStudentId(g.getStudentId());m.setComponent(component.code());m.setScore(g.getScore());m.setStatus(Status.GRADED);m.setComment(g.getStudentComment());m.setVersion(g.getVersion()==null?0:g.getVersion());
    try{GradingRules.mark(spec.kind(),component,new Mark(g.getStudentId(),component.code(),null,Status.GRADED,g.getScore(),null,g.getStudentComment()));}catch(ApiException ex){blockers.add("Điểm cũ không phù hợp quy chế: "+e.getName());}marks.add(m);
   }
   activities.add(a);sourceRows.add(Arrays.asList(e.getId(),e.getVersion(),e.getName(),e.getExamType(),e.getExamDate(),e.getMaxScore(),e.getWeight(),roster));
  }
  var context=academic.evaluationContext(id,spec.minAttendancePercent()!=null);if(context==null||context.enrollments()==null||context.sessions()==null||context.attendance()==null)throw ApiException.bad("Nguồn học vụ chưa đủ để xem trước");
  var b=new GradingBinding();b.setClassId(id);b.setTemplateId(template.getId());b.setName(template.getName());
  var proposed=blockers.isEmpty()?GradingCalculation.calculate(b,spec,context,activities,marks,LocalDate.now(BusinessTime.ZONE)):null;
  var sourceGrades=old.stream().map(g->Arrays.asList(g.getId(),g.getExam().getId(),g.getStudentId(),g.getScore(),g.getStudentComment(),g.getVersion())).toList();
  String fp=hash(Arrays.asList(id,school,plan,template.getSpecification(),sourceRows,sourceGrades,context.enrollments(),context.sessions(),context.attendance(),context.duration(),confirmed));
  return new Preview(id,plan.templateId(),fp,blockers.stream().distinct().toList(),all.size(),old.size(),proposed);
 }
 public Imported apply(Long id,Apply command){
  var c=school(id);var barrier=barriers.lock(id,c.branchId());String intent=hash(Arrays.asList(id,command.plan(),command.fingerprint(),command.reason().trim()));
  var replay=em.createQuery("select i from GradingLegacyImport i where i.requestId=:r",GradingLegacyImport.class).setParameter("r",command.requestId().toString()).getResultStream().findFirst().orElse(null);
  if(replay!=null){if(!replay.getIntent().equals(intent))throw ApiException.conflict("Mã yêu cầu đã dùng cho lần nhập khác");return imported(replay);}
  var preview=calculate(id,command.plan(),c,barrier.getConfirmedCount());if(!preview.blockers().isEmpty())throw ApiException.conflict(String.join("; ",preview.blockers()));if(!preview.fingerprint().equals(command.fingerprint()))throw ApiException.conflict("Nguồn hoặc ánh xạ đã đổi; xem trước lại");
  var t=em.find(GradingTemplate.class,command.plan().templateId());var b=new GradingBinding();b.setClassId(id);b.setBranchId(c.branchId());b.setCourseId(c.courseId());b.setTemplateId(t.getId());b.setName(t.getName());b.setSpecification(t.getSpecification());b.setRevision(1);b.setAssignedBy(Users.current().accountId());b.setAssignedAt(LocalDateTime.now(BusinessTime.ZONE));em.persist(b);em.flush();
  var copied=new ArrayList<Object>();for(var mapping:command.plan().mappings()){
   var e=exams.findById(mapping.examId()).orElseThrow();var a=grading.create(id,new ActivityCommand(e.getName(),e.getExamDate(),List.of(mapping.component())));
   var input=grades.findByExamId(e.getId()).stream().map(g->new Mark(g.getStudentId(),mapping.component(),null,Status.GRADED,g.getScore(),null,g.getStudentComment())).toList();
   if(!input.isEmpty())grading.save(id,a.id(),new SaveMarks(input,command.reason()));copied.add(Arrays.asList(e.getId(),a.id(),mapping.component(),input.size()));
  }
  var record=new GradingLegacyImport();record.setClassId(id);record.setRequestId(command.requestId().toString());record.setFingerprint(preview.fingerprint());record.setIntent(intent);record.setTemplateId(t.getId());record.setReason(command.reason().trim());record.setActorId(Users.current().accountId());record.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));record.setSnapshot(json.writeValueAsString(Map.of("preview",preview,"copies",copied)));em.persist(record);em.flush();return imported(record);
 }
 private Imported imported(GradingLegacyImport i){var node=json.readTree(i.getSnapshot()).get("preview");return new Imported(i.getId(),i.getClassId(),i.getTemplateId(),node.get("exams").asInt(),node.get("grades").asInt(),i.getCreatedAt());}
}
