package com.example.academic.lead;

import com.example.academic.common.*;
import com.example.academic.security.*;
import com.example.academic.student.*;
import com.example.academic.enrollment.*;
import com.example.academic.schoolclass.*;
import com.example.academic.placement.*;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class LeadIntakeService {
 private final LeadService leads;private final StudentService students;private final EnrollmentService enrollments;
 private final PlacementService placement;private final SchoolClassService classes;private final EntityManager em;private final JsonMapper json;
 public enum Mode {PROFILE_ONLY,ENROLL}
 public record Selection(@NotNull @PositiveOrZero Long leadVersion,@NotNull Mode mode,@Positive Long existingStudentId,@PositiveOrZero Long studentVersion,@Valid StudentDto.Request profile,@Positive Long classId,@Positive Long approvalId,Boolean acknowledgeSimilarProfiles){}
 public record Confirm(@NotNull UUID requestId,@NotNull @Valid Selection selection,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String fingerprint,@NotBlank @Size(min=3,max=500) String reason){}
 public record Preference(@NotNull @PositiveOrZero Long version,@Positive Long courseId,@Positive Long classId,@Size(max=500) String preferredTime,@NotBlank @Size(min=3,max=500) String reason){}
 public record Candidate(Long id,String studentCode,String fullName,LocalDate dateOfBirth,String phone,long version){}
 public record ClassInfo(Long id,String code,String name,Long branchId,Long courseId,LocalDate startDate,LocalDate endDate,BigDecimal tuitionFee,long currentSize,int maxSize){}
 public record Preview(LeadDto lead,Selection selection,StudentDto.Request profile,List<Candidate> similarProfiles,ClassInfo target,PlacementDto.Preview placement,List<String> blockers,List<String> notices,String fingerprint){}
 public record Result(Long leadId,Long studentId,Long enrollmentId,Mode mode,boolean createdStudent){}
 private void access(Lead l){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.STAFF).contains(Users.current().role()))throw ApiException.forbidden();leads.owner(l);}
 private String hash(Object data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(data).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private String phone(String p){String digits=p==null?"":p.replaceAll("[^0-9]","");return digits.startsWith("84")?"0"+digits.substring(2):digits;}
 private List<Candidate> candidates(Lead l,String contact,String name){
  // Same branch only; phone is a hint, never identity. Families may share a number.
  var rows=em.createQuery("select s from Student s where s.branchId=:branch and (replace(replace(replace(replace(replace(replace(s.phone,' ',''),'-',''),'.',''),'(',''),')',''),'+84','0')=:phone or lower(s.fullName)=:name) order by s.id desc",Student.class).setParameter("branch",l.getBranchId()).setParameter("phone",phone(contact)).setParameter("name",name.trim().toLowerCase(Locale.ROOT)).setMaxResults(21).getResultList();
  return rows.stream().map(s->new Candidate(s.getId(),s.getStudentCode(),s.getFullName(),s.getDateOfBirth(),s.getPhone(),s.getVersion())).toList();
 }
 @Transactional(readOnly=true) public Preview preview(Long id,Selection r){return inspect(leads.require(id),r);}
 private Preview inspect(Lead l,Selection r){
  access(l);var blockers=new ArrayList<String>();var notices=new ArrayList<String>();
  if(l.getVersion()!=r.leadVersion())blockers.add("Lead đã thay đổi; tải lại");
  if(!Set.of(Lead.Status.CONTACTED,Lead.Status.CONSULTING).contains(l.getStatus()))blockers.add("Chỉ tiếp nhận Lead đã liên hệ hoặc đang tư vấn");
  Student existing=null;StudentDto.Request profile=r.profile();
  if(r.existingStudentId()!=null){
   if(profile!=null)throw ApiException.bad("Chọn hồ sơ hiện có hoặc tạo mới, không gửi cả hai");
   existing=students.require(r.existingStudentId());if(!existing.getBranchId().equals(l.getBranchId()))throw ApiException.missing();
   if(r.studentVersion()==null||r.studentVersion()!=existing.getVersion())blockers.add("Hồ sơ học viên đã thay đổi; kiểm tra lại");
  }else{
   if(r.studentVersion()!=null||r.approvalId()!=null)throw ApiException.bad("Phiên bản và ngoại lệ chỉ dùng với học viên hiện có");
   if(profile==null)profile=new StudentDto.Request(l.getFullName(),null,null,l.getEmail(),l.getPhone(),null,null,null,l.getBranchId(),null,null);
   if(profile.branchId()!=null&&!profile.branchId().equals(l.getBranchId()))throw ApiException.bad("Hồ sơ phải cùng chi nhánh Lead");
   StudentProfileRules.validate(profile);
   if(StudentProfileRules.text(profile.currentLevel())!=null)throw ApiException.bad("Trình độ mong muốn không phải kết quả xác minh; bổ sung đánh giá sau khi tạo hồ sơ");
  }
  var similar=candidates(l,profile==null?l.getPhone():profile.phone(),profile==null?l.getFullName():profile.fullName());
  if(existing==null&&!similar.isEmpty()&&!Boolean.TRUE.equals(r.acknowledgeSimilarProfiles()))blockers.add("Có hồ sơ tương tự; chọn hồ sơ hiện có hoặc xác nhận đây là người khác");
  ClassInfo target=null;PlacementDto.Preview evaluation=null;
  if(r.mode()==Mode.PROFILE_ONLY){if(r.classId()!=null||r.approvalId()!=null)throw ApiException.bad("Chỉ tạo hồ sơ không kèm lớp hoặc ngoại lệ");notices.add("Chưa tạo đăng ký lớp hoặc học phí");}
  else if(r.classId()==null)blockers.add("Chọn lớp để đăng ký; lớp mong muốn không tự động trở thành đăng ký");
  else{
   var c=classes.require(r.classId());if(!c.getBranchId().equals(l.getBranchId()))throw ApiException.bad("Lớp phải cùng chi nhánh Lead");
   if(existing==null){var prospect=new Student();prospect.setId(-l.getId());prospect.setBranchId(l.getBranchId());prospect.setFullName(profile.fullName());evaluation=placement.previewProspect(prospect,c.getId());}
   else evaluation=placement.preview(placement.context(existing.getId(),c.getId(),PlacementDto.Intent.ENROLL,null));
   target=new ClassInfo(c.getId(),c.getCode(),c.getName(),c.getBranchId(),c.getCourse().getId(),c.getStartDate(),c.effectiveEndDate(),c.getCourse().getTuitionFee(),evaluation.currentSize(),evaluation.maxSize());
   evaluation.blockers().forEach(b->blockers.add(b.message()));
   if(!evaluation.eligible()){
    if(r.approvalId()==null)blockers.add("Điều kiện đầu vào: "+evaluation.academicStatus()+". Có thể chỉ tạo hồ sơ rồi đánh giá/xin ngoại lệ theo quy trình phân lớp.");
    else notices.add("Ngoại lệ sẽ được kiểm tra lại và tiêu thụ khi xác nhận; không miễn chỗ trống, lịch trùng hoặc quyền chi nhánh");
   }
  }
  notices.add("Không tự gộp theo số điện thoại, không thu tiền hoặc tạo tài khoản đăng nhập trong bước tiếp nhận");
  String fp=hash(Arrays.asList(l.getId(),l.getVersion(),r,profile,similar,target,evaluation==null?null:evaluation.fingerprint(),evaluation==null?null:evaluation.blockers()));
  return new Preview(LeadDto.of(l),r,profile,similar,target,evaluation,List.copyOf(blockers),List.copyOf(notices),fp);
 }
 public Result confirm(Long id,Confirm command){
  var l=leads.lock(id);access(l);String intent=hash(Arrays.asList(id,command.selection(),command.fingerprint(),command.reason().trim()));
  var old=em.find(LeadIntakeReceipt.class,command.requestId().toString());if(old!=null){if(!old.getLeadId().equals(id)||!old.getIntent().equals(intent))throw ApiException.conflict("Mã yêu cầu đã dùng cho nội dung khác");return result(old);}
  if(l.getStatus()==Lead.Status.CONVERTED)throw ApiException.conflict("Lead đã chuyển đổi; dùng chức năng đăng ký lớp cho hồ sơ hiện có");
  var selection=command.selection();if(selection.existingStudentId()!=null)students.lock(selection.existingStudentId());if(selection.classId()!=null)classes.lock(selection.classId());
  var p=inspect(l,selection);if(!p.blockers().isEmpty())throw ApiException.conflict(String.join("; ",p.blockers()));if(!p.fingerprint().equals(command.fingerprint()))throw ApiException.conflict("Thông tin tiếp nhận đã thay đổi; xem trước lại");
  Student s;if(selection.existingStudentId()!=null)s=students.require(selection.existingStudentId());else{var r=p.profile();var saved=students.save(null,new StudentDto.Request(r.fullName(),r.dateOfBirth(),r.gender(),r.email(),r.phone(),r.address(),r.contactPerson(),null,l.getBranchId(),r.guardianName(),r.guardianPhone()));s=students.require(saved.id());}
  EnrollmentDto enrollment=selection.mode()==Mode.ENROLL?enrollments.enroll(new EnrollmentDto.Request(s.getId(),selection.classId(),selection.approvalId())):null;
  var previous=l.getStatus();var next=l.getNextContactAt();l.setConvertedStudent(s);l.setStatus(Lead.Status.CONVERTED);l.setNextContactAt(null);
  leads.record(l,"INTAKE_CONFIRMED",command.reason().trim()+"; học viên "+s.getStudentCode()+(enrollment==null?"; chưa đăng ký lớp":"; đăng ký #"+enrollment.id()),null,previous.name(),null,null,next);
  var receipt=new LeadIntakeReceipt();receipt.setRequestId(command.requestId().toString());receipt.setLeadId(id);receipt.setStudentId(s.getId());receipt.setEnrollmentId(enrollment==null?null:enrollment.id());receipt.setCreatedStudent(selection.existingStudentId()==null);receipt.setIntent(intent);receipt.setActorId(Users.current().accountId());receipt.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));receipt.setReason(command.reason().trim());em.persist(receipt);em.flush();return result(receipt);
 }
 private Result result(LeadIntakeReceipt r){return new Result(r.getLeadId(),r.getStudentId(),r.getEnrollmentId(),r.getEnrollmentId()==null?Mode.PROFILE_ONLY:Mode.ENROLL,r.isCreatedStudent());}
 public Result recover(Long id,UUID requestId){var l=leads.lock(id);access(l);var receipt=em.find(LeadIntakeReceipt.class,requestId.toString());if(receipt==null||!receipt.getLeadId().equals(id))throw ApiException.missing();return result(receipt);}
 public record WebsiteChoice(String programSlug,String courseSlug,String classSlug,LocalDateTime submittedAt){}
 public record Preferences(LeadDto current,List<WebsiteChoice> websiteChoices){}
 @Transactional(readOnly=true) public Preferences preferences(Long id){var l=leads.require(id);access(l);var choices=em.createQuery("select w from WebsiteInquiry w where w.leadId=:id order by w.createdAt desc",com.example.academic.website.WebsiteInquiry.class).setParameter("id",id).setMaxResults(20).getResultList().stream().map(w->new WebsiteChoice(w.getProgramSlug(),w.getCourseSlug(),w.getClassSlug(),w.getCreatedAt())).toList();return new Preferences(LeadDto.of(l),choices);}
 public LeadDto preference(Long id,Preference r){var l=leads.lock(id);access(l);if(l.getStatus()==Lead.Status.CONVERTED||l.getVersion()!=r.version())throw ApiException.conflict("Lead đã chuyển đổi hoặc thay đổi; tải lại");var c=r.classId()==null?null:classes.require(r.classId());if(c!=null&&!c.getBranchId().equals(l.getBranchId()))throw ApiException.bad("Lớp phải cùng chi nhánh");var course=r.courseId()==null?null:em.find(com.example.academic.course.Course.class,r.courseId());if(r.courseId()!=null&&(course==null||course.getBranchId()!=null&&!course.getBranchId().equals(l.getBranchId())))throw ApiException.missing();if(c!=null&&course!=null&&!c.getCourse().getId().equals(course.getId()))throw ApiException.bad("Lớp không khớp khóa học");String before="Khóa #"+(l.getInterestedCourse()==null?null:l.getInterestedCourse().getId())+", lớp #"+(l.getPreferredClass()==null?null:l.getPreferredClass().getId())+", lịch "+l.getPreferredTime();l.setPreferredClass(c);l.setInterestedCourse(c==null?course:c.getCourse());l.setPreferredTime(StudentProfileRules.text(r.preferredTime()));leads.record(l,"PREFERENCE_CHANGED",r.reason()+"; trước: "+before+"; sau: khóa #"+(l.getInterestedCourse()==null?null:l.getInterestedCourse().getId())+", lớp #"+r.classId()+", lịch "+l.getPreferredTime(),null,null,null,null,l.getNextContactAt());em.flush();return LeadDto.of(l);}
}
