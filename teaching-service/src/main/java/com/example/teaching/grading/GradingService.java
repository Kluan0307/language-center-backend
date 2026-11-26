package com.example.teaching.grading;

import com.example.teaching.common.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.results.ResultBarrierService;
import com.example.teaching.security.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static com.example.teaching.grading.GradingModels.*;

@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class GradingService {
 private final EntityManager em;
 private final JsonMapper json;
 private final AcademicDirectory academic;
 private final BranchScope branches;
 private final ResultBarrierService barriers;
 private final GradingBindingRepository bindings;
 private final GradingConversionService conversions;
 private final GradingWriteGuard writeGuard;
 private LocalDateTime now(){return LocalDateTime.now(BusinessTime.ZONE);}
 private void reader(){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.TEACHER).contains(Users.current().role()))throw ApiException.forbidden();}
 private void manager(){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN).contains(Users.current().role()))throw ApiException.forbidden();}
 private void reason(String value){if(value==null||value.trim().length()<3||value.trim().length()>500)throw ApiException.bad("Lý do phải từ 3 đến 500 ký tự");}
 private String encode(Object o){return json.writeValueAsString(o);}
 private Spec spec(String value){return json.readValue(value,Spec.class);}
 private void event(Long classId,String kind,String why,Object before,Object after){var e=new GradingEvent();e.setClassId(classId);e.setKind(kind);e.setActorId(Users.current().accountId());e.setOccurredAt(now());e.setReason(why.trim());e.setBeforeValue(before==null?null:encode(before));e.setAfterValue(encode(after));em.persist(e);}
 public void initialize(){for(var kind:Kind.values()){String id=kind.name()+"-v1";if(em.find(GradingTemplate.class,id)!=null)continue;var t=new GradingTemplate();t.setId(id);t.setCode(kind);t.setRevision(1);t.setName(switch(kind){case OBSERVATION->"Nhận xét trẻ nhỏ";case INTERNAL->"Điểm nội bộ";case IELTS->"IELTS thi thử";case TOEIC_LR->"TOEIC Listening & Reading thi thử";});t.setSpecification(encode(GradingRules.standard(kind)));t.setCreatedBy(0L);t.setCreatedAt(now());t.setReason("Khởi tạo mẫu tham khảo; chỉ có hiệu lực khi quản lý chủ động áp dụng");em.persist(t);}}
 private GradingTemplate template(String id){var t=em.find(GradingTemplate.class,id);if(t==null)throw ApiException.missing();return t;}
 private TemplateView view(GradingTemplate t){return new TemplateView(t.getId(),t.getCode(),t.getRevision(),t.getName(),spec(t.getSpecification()));}
 @Transactional(readOnly=true) public List<TemplateView> templates(){reader();return em.createQuery("select t from GradingTemplate t order by t.code,t.revision desc",GradingTemplate.class).setMaxResults(200).getResultList().stream().map(this::view).toList();}
 public TemplateView revise(TemplateCommand r){
  if(Users.current().role()!=Role.SUPER_ADMIN)throw ApiException.forbidden();reason(r.reason());GradingRules.validate(r.spec());if(r.code()!=r.spec().kind())throw ApiException.bad("Loại quy chế không khớp");
  em.find(GradingTemplate.class,r.code().name()+"-v1",LockModeType.PESSIMISTIC_WRITE);
  long latest=em.createQuery("select max(t.revision) from GradingTemplate t where t.code=:c",Long.class).setParameter("c",r.code()).getSingleResult();if(latest!=r.expectedRevision())throw ApiException.conflict("Quy chế đã có phiên bản mới; tải lại");
  var t=new GradingTemplate();t.setCode(r.code());t.setRevision(latest+1);t.setId(r.code().name()+"-v"+t.getRevision());t.setName(r.name().trim());t.setSpecification(encode(r.spec()));t.setCreatedBy(Users.current().accountId());t.setCreatedAt(now());t.setReason(r.reason().trim());em.persist(t);event(null,"TEMPLATE_REVISION",r.reason(),null,view(t));return view(t);
 }
 private AcademicDirectory.SchoolClass school(Long classId,boolean write){reader();var c=academic.schoolClass(classId);branches.check(c.branchId());branches.teacher(c.teacherId(),write);if(write&&Boolean.TRUE.equals(c.closed()))throw ApiException.conflict("Lớp đã đóng, không được sửa đánh giá");return c;}
 private BindingView view(GradingBinding b){return b==null?null:new BindingView(b.getClassId(),b.getBranchId(),b.getCourseId(),b.getRevision(),b.getTemplateId(),b.getName(),spec(b.getSpecification()),b.isLocked(),b.getAssignedAt());}
 private GradingBinding binding(AcademicDirectory.SchoolClass c){var b=bindings.findById(c.id()).orElseThrow(()->ApiException.conflict("Quản lý chưa áp dụng quy chế đánh giá cho lớp"));if(!b.getBranchId().equals(c.branchId())||!b.getCourseId().equals(c.courseId()))throw ApiException.conflict("Thông tin khóa học/chi nhánh đã thay đổi; cần rà soát quy chế lớp");return b;}
 @Transactional(readOnly=true) public BindingView binding(Long id){var c=school(id,false);var b=bindings.findById(id).orElse(null);if(b!=null)binding(c);return view(b);}
 @Transactional(readOnly=true) public GradingCourseDefault courseDefault(Long courseId,Long branchId){manager();branches.check(branchId);academic.course(courseId,branchId);return em.find(GradingCourseDefault.class,courseId+"-"+branchId);}
 public GradingCourseDefault courseDefault(CourseAssignment r){manager();reason(r.reason());branches.check(r.branchId());academic.course(r.courseId(),r.branchId());template(r.templateId());String key=r.courseId()+"-"+r.branchId();var d=em.find(GradingCourseDefault.class,key,LockModeType.PESSIMISTIC_WRITE);if((d==null?0:d.getRevision())!=r.expectedRevision())throw ApiException.conflict("Mặc định khóa học đã thay đổi; tải lại");Object before=d==null?null:List.of(d.getTemplateId(),d.getRevision());if(d==null){d=new GradingCourseDefault();d.setId(key);d.setCourseId(r.courseId());d.setBranchId(r.branchId());}d.setTemplateId(r.templateId());d.setRevision(d.getRevision()+1);em.persist(d);event(null,"COURSE_DEFAULT",r.reason(),before,List.of(d.getCourseId(),d.getBranchId(),d.getTemplateId(),d.getRevision()));return d;}
 public BindingView assign(Long classId,Assignment r){
  manager();reason(r.reason());var c=school(classId,true);barriers.mutable(c.id(),c.branchId());writeGuard.check(classId);var b=bindings.findById(classId).orElse(null);
  if((b==null?0:b.getRevision())!=r.expectedRevision())throw ApiException.conflict("Quy chế lớp đã thay đổi; tải lại");
  if(b!=null&&b.isLocked())throw ApiException.conflict("Lớp đã tạo bài đánh giá; phiên bản quy chế đã được chốt");
  long legacy=em.createQuery("select count(e) from Exam e where e.classId=:id",Long.class).setParameter("id",classId).getSingleResult();
  if(legacy>0)throw ApiException.conflict("Lớp có bài kiểm tra theo quy chế cũ; cần chuyển đổi có đối chiếu, không tự áp dụng quy chế mới");
  var t=template(r.templateId());Object before=view(b);if(b==null){b=new GradingBinding();b.setClassId(c.id());b.setBranchId(c.branchId());b.setCourseId(c.courseId());}else binding(c);
  b.setRevision(b.getRevision()+1);b.setTemplateId(t.getId());b.setName(t.getName());b.setSpecification(t.getSpecification());b.setAssignedBy(Users.current().accountId());b.setAssignedAt(now());bindings.saveAndFlush(b);event(classId,"CLASS_ASSIGNMENT",r.reason(),before,view(b));return view(b);
 }
 private List<String> components(GradingActivity a){return Arrays.asList(a.getComponents().split(","));}
 private ActivityView view(GradingActivity a){return new ActivityView(a.getId(),a.getClassId(),a.getName(),a.getAssessmentDate(),components(a),a.getRosterFrozenAt()!=null,a.getCandidates().size(),a.getRetakeOf(),a.getConversionId(),a.getVersion());}
 public ActivityView edit(Long classId,Long id,ActivityEdit r){var c=school(classId,true);barriers.mutable(classId,c.branchId());writeGuard.check(classId);var a=activity(classId,id);em.refresh(a,LockModeType.PESSIMISTIC_WRITE);if(a.getVersion()!=r.expectedVersion())throw ApiException.conflict("Bài đánh giá đã đổi; tải lại");if(a.getRosterFrozenAt()!=null||!marks(id).isEmpty())throw ApiException.conflict("Đã bắt đầu nhập kết quả; không được đổi ngày hoặc thông tin bài");
  for(var record:em.createQuery("select i from GradingLegacyImport i where i.classId=:id",GradingLegacyImport.class).setParameter("id",classId).getResultList())for(var copy:json.readTree(record.getSnapshot()).get("copies"))if(copy.get(1).asLong()==id)throw ApiException.conflict("Bài được sao chép đối chiếu từ dữ liệu cũ; giữ nguyên dấu vết nguồn");
  reason(r.reason());var before=view(a);a.setName(r.name().trim());a.setAssessmentDate(r.date());em.flush();event(classId,"ACTIVITY_EDITED",r.reason(),before,view(a));return view(a);
 }
 @Transactional(readOnly=true) public List<ActivityView> activities(Long id){school(id,false);return em.createQuery("select a from GradingActivity a where a.classId=:id order by a.assessmentDate desc,a.id desc",GradingActivity.class).setParameter("id",id).setMaxResults(200).getResultList().stream().map(this::view).toList();}
 public ActivityView create(Long id,ActivityCommand r){
  var c=school(id,true);barriers.mutable(id,c.branchId());writeGuard.check(id);var b=binding(c);var s=spec(b.getSpecification());Set<String> codes=new HashSet<>(s.components().stream().map(Component::code).toList());
  if(r.components()==null||r.components().isEmpty()||r.components().size()!=new HashSet<>(r.components()).size()||!codes.containsAll(r.components()))throw ApiException.bad("Thành phần bài đánh giá không thuộc quy chế");
  if(s.kind()==Kind.INTERNAL&&r.components().size()!=1)throw ApiException.bad("Mỗi bài điểm nội bộ thuộc một thành phần quy chế");
  long count=em.createQuery("select count(a) from GradingActivity a where a.classId=:id",Long.class).setParameter("id",id).getSingleResult();if(count>=200)throw ApiException.bad("Lớp có tối đa 200 bài đánh giá");
  if(r.conversionId()!=null){if(s.kind()!=Kind.TOEIC_LR)throw ApiException.bad("Bảng quy đổi này chỉ dành cho TOEIC L&R");conversions.require(r.conversionId());}
  var a=new GradingActivity();a.setClassId(id);a.setName(r.name().trim());a.setAssessmentDate(r.date());a.setComponents(String.join(",",r.components()));a.setCreatedBy(Users.current().accountId());a.setConversionId(r.conversionId());
  if(r.retakeOf()!=null){var original=activity(id,r.retakeOf());if(original.getRetakeOf()!=null||original.getRosterFrozenAt()==null||r.date().isBefore(original.getAssessmentDate())||!new HashSet<>(components(original)).equals(new HashSet<>(r.components())))throw ApiException.bad("Thi lại phải tham chiếu bài gốc đã chốt danh sách, đúng thành phần và không trước ngày bài gốc");
   var selected=r.studentIds()==null?original.getCandidates().stream().map(GradingActivity.Candidate::getStudentId).toList():r.studentIds();if(selected.isEmpty()||new HashSet<>(selected).size()!=selected.size()||selected.stream().anyMatch(student->original.getCandidates().stream().noneMatch(x->x.getStudentId().equals(student))))throw ApiException.bad("Danh sách thi lại không thuộc bài gốc");
   for(var candidate:original.getCandidates())if(selected.contains(candidate.getStudentId()))a.getCandidates().add(new GradingActivity.Candidate(candidate.getStudentId(),candidate.getStudentName(),candidate.getEnrollmentId()));a.setRetakeOf(original.getId());a.setRosterFrozenAt(now());
  }else if(r.studentIds()!=null)throw ApiException.bad("Chỉ chọn riêng học viên cho bài thi lại");
  em.persist(a);b.setLocked(true);em.flush();event(id,"ACTIVITY_CREATED","Tạo bài đánh giá theo quy chế lớp",null,view(a));return view(a);
 }
 private GradingActivity activity(Long classId,Long id){var a=em.find(GradingActivity.class,id);if(a==null||!a.getClassId().equals(classId))throw ApiException.missing();return a;}
 private List<Candidate> roster(GradingActivity a){if(a.getRosterFrozenAt()!=null)return a.getCandidates().stream().map(x->new Candidate(x.getStudentId(),x.getStudentName(),x.getEnrollmentId())).toList();return academic.studentsAt(a.getClassId(),a.getAssessmentDate()).stream().map(x->new Candidate(x.id(),x.fullName(),x.enrollmentId())).toList();}
 private List<GradingMark> marks(Long activityId){return em.createQuery("select m from GradingMark m where m.activityId=:id order by m.studentId,m.component",GradingMark.class).setParameter("id",activityId).getResultList();}
 private MarkView view(GradingMark m){return new MarkView(m.getStudentId(),m.getComponent(),m.getVersion(),m.getStatus(),m.getScore(),m.getLevel(),m.getComment(),m.getEnteredBy(),m.getUpdatedAt(),m.getRawScore(),m.getConversionId());}
 @Transactional(readOnly=true) public Sheet sheet(Long classId,Long id){var c=school(classId,false);var a=activity(classId,id);return new Sheet(view(binding(c)),view(a),roster(a),marks(id).stream().map(this::view).toList());}
 public Sheet save(Long classId,Long id,SaveMarks r){
  reason(r.reason());var c=school(classId,true);barriers.mutable(classId,c.branchId());writeGuard.check(classId);var b=binding(c);var a=activity(classId,id);em.refresh(a,LockModeType.PESSIMISTIC_WRITE);
  if(a.getAssessmentDate().isAfter(LocalDate.now(BusinessTime.ZONE)))throw ApiException.conflict("Chưa đến ngày đánh giá, chưa được nhập kết quả");
  var candidates=roster(a);if(candidates.isEmpty()||candidates.stream().anyMatch(x->x.enrollmentId()==null)||candidates.stream().map(Candidate::studentId).distinct().count()!=candidates.size())throw ApiException.conflict("Danh sách dự đánh giá chưa xác định được đăng ký học hợp lệ");
  var rules=spec(b.getSpecification());var old=marks(id);var before=old.stream().map(this::view).toList();var seen=new HashSet<String>();
  for(var input:r.records()){
   var mark=converted(input,a,rules.kind());
   String key=mark.studentId()+":"+mark.component();if(!seen.add(key))throw ApiException.bad("Trùng kết quả học viên/thành phần trong yêu cầu");
   if(candidates.stream().noneMatch(x->x.studentId().equals(mark.studentId())))throw ApiException.bad("Học viên không thuộc danh sách tại ngày đánh giá");
   if(!components(a).contains(mark.component()))throw ApiException.bad("Thành phần không thuộc bài đánh giá");var component=rules.components().stream().filter(x->x.code().equals(mark.component())).findFirst().orElseThrow();GradingRules.mark(rules.kind(),component,mark);
   var m=old.stream().filter(x->x.getStudentId().equals(mark.studentId())&&x.getComponent().equals(mark.component())).findFirst().orElse(null);
   if(m==null&&mark.expectedVersion()!=null||m!=null&&(mark.expectedVersion()==null||mark.expectedVersion()!=m.getVersion()))throw ApiException.conflict("Kết quả đã thay đổi; tải lại trước khi lưu");
   boolean fresh=m==null;if(fresh){m=new GradingMark();m.setActivityId(id);m.setStudentId(mark.studentId());m.setComponent(mark.component());m.setEnteredBy(Users.current().accountId());}
   m.setStatus(mark.status());m.setScore(mark.score());m.setLevel(mark.level());m.setComment(mark.comment()==null?null:mark.comment().trim());m.setUpdatedAt(now());
   m.setRawScore(mark.rawScore());m.setConversionId(mark.rawScore()==null?null:a.getConversionId());
   if(fresh)em.persist(m);
  }
  if(a.getRosterFrozenAt()==null){a.setRosterFrozenAt(now());for(var candidate:candidates)a.getCandidates().add(new GradingActivity.Candidate(candidate.studentId(),candidate.studentName(),candidate.enrollmentId()));}
  em.flush();event(classId,"DRAFT_RESULTS_SAVED",r.reason(),before,marks(id).stream().map(this::view).toList());return sheet(classId,id);
 }
 private Mark converted(Mark m,GradingActivity a,Kind kind){if(m.rawScore()==null)return m;if(kind!=Kind.TOEIC_LR||a.getConversionId()==null||m.status()!=Status.GRADED)throw ApiException.bad("Nhập số câu đúng cần bài TOEIC có bảng quy đổi được xác định");var score=conversions.convert(a.getConversionId(),m.component(),m.rawScore());if(m.score()!=null&&m.score().compareTo(score)!=0)throw ApiException.bad("Điểm gửi lên không khớp bảng quy đổi của bài");return new Mark(m.studentId(),m.component(),m.expectedVersion(),m.status(),score,m.level(),m.comment(),m.rawScore());}
 @Transactional(readOnly=true) public List<GradingEvent> history(Long classId){school(classId,false);return em.createQuery("select e from GradingEvent e where e.classId=:id order by e.id desc",GradingEvent.class).setParameter("id",classId).setMaxResults(100).getResultList();}
}
