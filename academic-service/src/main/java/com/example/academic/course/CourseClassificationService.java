package com.example.academic.course;
import com.example.academic.admission.Program;
import com.example.academic.common.*;
import com.example.academic.security.*;
import jakarta.persistence.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class CourseClassificationService {
 private final EntityManager em;
 private final CourseService courses;
 public record Option(Long id,String code,String name,boolean active) {}
 public record Catalog(List<Option> programs,List<Option> audiences) {}
 @Transactional(readOnly=true)
 public Catalog catalog(){return new Catalog(em.createQuery("select p from Program p order by p.code",Program.class).getResultStream().filter(p->List.of("GE","COMMUNICATION","IELTS","TOEIC").contains(p.getCode())).map(p->new Option(p.getId(),p.getCode(),p.getName(),p.isActive())).toList(),em.createQuery("select a from AudienceGroup a order by a.sortOrder",AudienceGroup.class).getResultStream().filter(a->Arrays.stream(AudienceGroup.Code.values()).anyMatch(code->code.name().equals(a.getCode()))).map(a->new Option(a.getId(),a.getCode(),a.getName(),a.isActive())).toList());}
 @Transactional
 public CourseDto save(Long id,CourseDto.Request r){
  if(r.programId()==null||r.audienceIds()==null||r.audienceIds().isEmpty()||r.audienceIds().size()>5||new HashSet<>(r.audienceIds()).size()!=r.audienceIds().size()||r.audienceIds().stream().anyMatch(x->x==null||x<=0)||r.classificationRevision()==null)throw ApiException.bad("Chọn chương trình, đối tượng hợp lệ và phiên bản phân loại");
  Course before=id==null?null:courses.require(id);
  String legacy=before==null?null:before.getType();
  if(r.type()!=null&&!Objects.equals(r.type(),legacy))throw ApiException.bad("Không nhập loại tự do; chọn chương trình từ danh mục");
  // Core save enforces branch, shared-course and fee permissions; all changes roll back on failure.
  var normalized=new CourseDto.Request(r.code(),r.name(),legacy,r.level(),r.duration(),r.tuitionFee(),r.content(),r.entryRequirement(),r.maxStudents(),r.branchId(),r.expectedMinutes());
  var saved=courses.save(id,normalized);Course c=em.find(Course.class,saved.id(),LockModeType.PESSIMISTIC_WRITE);
  if(c.getClassificationRevision()!=r.classificationRevision())throw ApiException.conflict("Phân loại đã thay đổi; tải lại khóa học");
  var groups=new HashSet<Long>();c.getAudienceGroups().forEach(a->groups.add(a.getId()));
  boolean changed=!Objects.equals(c.getProgramId(),r.programId())||!groups.equals(new HashSet<>(r.audienceIds()));
  if(changed&&Users.current().role()==Role.STAFF)throw ApiException.forbidden();
  Program p=em.find(Program.class,r.programId(),LockModeType.PESSIMISTIC_READ);
  if(p==null||!List.of("GE","COMMUNICATION","IELTS","TOEIC").contains(p.getCode()))throw ApiException.bad("Chương trình không thuộc danh mục chuẩn");
  if(!p.isActive()&&!Objects.equals(c.getProgramId(),p.getId()))throw ApiException.bad("Chương trình đã ngừng áp dụng");
  if(c.getProgramId()!=null&&!c.getProgramId().equals(p.getId())&&em.createQuery("select count(s) from SchoolClass s where s.course.id=:id",Long.class).setParameter("id",c.getId()).getSingleResult()>0)throw ApiException.bad("Khóa đã có lớp; tạo khóa mới thay vì đổi chương trình");
  var policies=em.createQuery("select a.programId from CourseAdmissionPolicy a where a.courseId=:id order by a.revision desc",Long.class).setParameter("id",c.getId()).setMaxResults(1).getResultList();
  if(!policies.isEmpty()&&!policies.getFirst().equals(p.getId()))throw ApiException.bad("Chương trình không khớp chính sách đầu vào; không sửa lịch sử để ép khớp");
  var selected=new HashSet<AudienceGroup>();for(Long aid:new TreeSet<>(r.audienceIds())){var a=em.find(AudienceGroup.class,aid,LockModeType.PESSIMISTIC_READ);if(a==null||!Arrays.stream(AudienceGroup.Code.values()).anyMatch(code->code.name().equals(a.getCode()))||(!a.isActive()&&!groups.contains(aid)))throw ApiException.bad("Đối tượng không còn được phép chọn");selected.add(a);}
  c.setProgramId(p.getId());c.getAudienceGroups().clear();c.getAudienceGroups().addAll(selected);if(changed)c.setClassificationRevision(c.getClassificationRevision()+1);em.flush();return CourseDto.of(c);
 }
}
