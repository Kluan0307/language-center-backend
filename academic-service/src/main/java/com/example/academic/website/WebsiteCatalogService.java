package com.example.academic.website;

import com.example.academic.admission.Program;
import com.example.academic.branch.Branch;
import com.example.academic.course.Course;
import com.example.academic.schoolclass.SchoolClass;
import com.example.academic.common.*;
import com.example.academic.security.Users;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.academic.website.WebsitePublication.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class WebsiteCatalogService {
  private final EntityManager em;
  private final WebsitePublicationRepository repository;

  // Eligibility is evaluated against live source records before paging, including ancestors.
  private String visible(Kind kind,String p) {
    String base=p+".state = 'PUBLISHED' and ";
    String rule=base+switch(kind) {
      case PROGRAM -> "exists (select x.id from Program x where x.id="+p+".sourceId and x.active=true)";
      case BRANCH -> "exists (select x.id from Branch x where x.id="+p+".sourceId and x.status='ACTIVE')";
      case COURSE -> "exists (select x.id from Course x where x.id="+p+".sourceId and x.status='OPEN' and (x.branchId is null or exists (select bp.id from WebsitePublication bp, Branch b where bp.kind='BRANCH' and bp.state='PUBLISHED' and bp.sourceId=b.id and b.id=x.branchId and b.status='ACTIVE'))) and exists (select pp.id from WebsitePublication pp, Program pr where pp.kind='PROGRAM' and pp.state='PUBLISHED' and pp.sourceId=pr.id and pr.active=true and pr.id="+p+".programId)";
      case CLASS -> "exists (select x.id from SchoolClass x, WebsitePublication cp, WebsitePublication bp, Branch b where x.id="+p+".sourceId and x.status='UPCOMING' and x.startDate>:today and x.currentSize<x.maxSize and cp.kind='COURSE' and cp.sourceId=x.course.id and "+visible(Kind.COURSE,"cp")+" and bp.kind='BRANCH' and bp.sourceId=x.branchId and b.id=x.branchId and b.status='ACTIVE' and bp.state='PUBLISHED')";
    };
    // A changed admission policy must not leave the course advertised under an old program.
    if(kind==Kind.COURSE)rule+=" and not exists (select ap.id from CourseAdmissionPolicy ap where ap.courseId="+p+".sourceId and ap.programId<>"+p+".programId and ap.revision=(select max(ap2.revision) from CourseAdmissionPolicy ap2 where ap2.courseId="+p+".sourceId))";
    if(kind==Kind.COURSE)rule+=" and exists (select cc.id from Course cc where cc.id="+p+".sourceId and (cc.programId is null or cc.programId="+p+".programId))";
    return rule;
  }
  private List<WebsitePublication> query(Kind kind,String slug,int offset,int limit,String program,String course,String branch) {
    return query(kind,slug,offset,limit,program,course,branch,null,null);
  }
  private List<WebsitePublication> query(Kind kind,String slug,int offset,int limit,String program,String course,String branch,String keyword,java.math.BigDecimal maxTuition) {
    return query(kind,slug,offset,limit,program,course,branch,keyword,maxTuition,null);
  }
  private List<WebsitePublication> query(Kind kind,String slug,int offset,int limit,String program,String course,String branch,String keyword,java.math.BigDecimal maxTuition,String audience) {
    if(program!=null&&kind!=Kind.COURSE||course!=null&&kind!=Kind.CLASS||branch!=null&&kind!=Kind.COURSE&&kind!=Kind.CLASS)throw ApiException.bad("Bộ lọc không phù hợp loại nội dung");
    String filters="";
    if(audience!=null){if(kind!=Kind.COURSE||!java.util.Arrays.stream(com.example.academic.course.AudienceGroup.Code.values()).anyMatch(a->a.name().equals(audience)))throw ApiException.bad("Đối tượng không hợp lệ");filters+=" and exists (select ac.id from Course ac join ac.audienceGroups ag where ac.id=p.sourceId and ag.code=:audience and ag.active=true)";}
    if(program!=null)filters+=" and exists (select fp.id from WebsitePublication fp where fp.kind='PROGRAM' and fp.slug=:program and fp.sourceId=p.programId)";
    if(course!=null)filters+=" and exists (select fc.id from WebsitePublication fc, SchoolClass sc where fc.kind='COURSE' and fc.slug=:course and sc.id=p.sourceId and sc.course.id=fc.sourceId)";
    if(branch!=null)filters+=" and exists (select fb.id from WebsitePublication fb, Branch bv, "+(kind==Kind.COURSE?"Course":"SchoolClass")+" bs where fb.kind='BRANCH' and fb.state='PUBLISHED' and fb.slug=:branch and bv.id=fb.sourceId and bv.status='ACTIVE' and bs.id=p.sourceId and "+(kind==Kind.COURSE?"(bs.branchId is null or bs.branchId=fb.sourceId)":"bs.branchId=fb.sourceId")+")";
    if(keyword!=null&&!keyword.isBlank())filters+=" and exists (select ss.id from "+switch(kind){case PROGRAM->"Program";case COURSE->"Course";case BRANCH->"Branch";case CLASS->"SchoolClass";}+" ss where ss.id=p.sourceId and (locate(:keyword,lower(ss.name))>0 or locate(:keyword,lower(ss.code))>0))";
    if(maxTuition!=null){if(kind!=Kind.COURSE||maxTuition.signum()<0||maxTuition.compareTo(new java.math.BigDecimal("1000000000000"))>0)throw ApiException.bad("Bộ lọc học phí không hợp lệ");filters+=" and p.showTuition=true and exists (select fc2.id from Course fc2 where fc2.id=p.sourceId and fc2.tuitionFee<=:maxTuition)";}
    var q=em.createQuery("select p from WebsitePublication p where p.kind=:kind and "+visible(kind,"p")+(slug==null?"":" and p.slug=:slug")+filters+" order by p.sourceId",WebsitePublication.class).setParameter("kind",kind).setFirstResult(offset).setMaxResults(limit);
    if(kind==Kind.CLASS)q.setParameter("today",LocalDate.now(BusinessTime.ZONE));
    if(slug!=null)q.setParameter("slug",slug);
    if(program!=null)q.setParameter("program",program);
    if(course!=null)q.setParameter("course",course);
    if(branch!=null)q.setParameter("branch",branch);
    if(keyword!=null&&!keyword.isBlank())q.setParameter("keyword",keyword.trim().toLowerCase(Locale.ROOT));
    if(maxTuition!=null)q.setParameter("maxTuition",maxTuition);
    if(audience!=null)q.setParameter("audience",audience);
    return q.getResultList();
  }
  public WebsiteDto.Page list(Kind kind,int page,int size) {
    return list(kind,page,size,null,null,null);
  }
  public WebsiteDto.Page list(Kind kind,int page,int size,String program,String course,String branch) {
    return list(kind,page,size,program,course,branch,null,null);
  }
  public WebsiteDto.Page list(Kind kind,int page,int size,String program,String course,String branch,String keyword,java.math.BigDecimal maxTuition) {
    return list(kind,page,size,program,course,branch,keyword,maxTuition,null);
  }
  public WebsiteDto.Page list(Kind kind,int page,int size,String program,String course,String branch,String keyword,java.math.BigDecimal maxTuition,String audience) {
    if(page<0||page>10000||size<1||size>100)throw ApiException.bad("page 0..10000; size 1..100");
    if(keyword!=null&&keyword.length()>100)throw ApiException.bad("Từ khóa tối đa 100 ký tự");
    var rows=query(kind,null,page*size,size+1,program,course,branch,keyword,maxTuition,audience);
    return new WebsiteDto.Page(rows.stream().limit(size).map(this::item).toList(),page,size,rows.size()>size);
  }
  public WebsiteDto.Item detail(Kind kind,String slug) {
    return query(kind,slug,0,1,null,null,null).stream().findFirst().map(this::item).orElseThrow(ApiException::missing);
  }
  private String slug(Kind kind,Long id) {
    return id==null?null:repository.findByKindAndSourceId(kind,id).map(WebsitePublication::getSlug).orElse(null);
  }
  private Object source(Kind kind,Long id) {
    Class<?> type=switch(kind){case PROGRAM->Program.class;case COURSE->Course.class;case BRANCH->Branch.class;case CLASS->SchoolClass.class;};
    Object x=em.find(type,id);
    if(x==null)throw ApiException.missing();return x;
  }
  private WebsiteDto.Item item(WebsitePublication p) {
    Object x=source(p.getKind(),p.getSourceId());
    String code=null,name=null,address=null,phone=null,entry=null,course=null,branch=null,branchName=null;
    List<WebsiteDto.Meeting> meetings=List.of();
    Integer minutes=null;java.math.BigDecimal fee=null;LocalDate start=null,end=null;
    if(x instanceof Program a){code=a.getCode();name=a.getName();}
    if(x instanceof Branch a){code=a.getCode();name=a.getName();address=a.getAddress();phone=a.getPhone();}
    if(x instanceof Course a){code=a.getCode();name=a.getName();entry=a.getEntryRequirement();minutes=a.getExpectedMinutes();fee=p.isShowTuition()?a.getTuitionFee():null;branch=slug(Kind.BRANCH,a.getBranchId());if(a.getBranchId()!=null)branchName=em.find(Branch.class,a.getBranchId()).getName();}
    if(x instanceof SchoolClass a){code=a.getCode();name=a.getName();start=a.getStartDate();end=a.effectiveEndDate();course=slug(Kind.COURSE,a.getCourse().getId());branch=slug(Kind.BRANCH,a.getBranchId());branchName=em.find(Branch.class,a.getBranchId()).getName();meetings=a.getSchedules().stream().sorted(Comparator.comparing(com.example.academic.schedule.Schedule::getDayOfWeek).thenComparing(com.example.academic.schedule.Schedule::getStartTime)).map(s->new WebsiteDto.Meeting(s.getDayOfWeek().name(),s.getStartTime(),s.getEndTime())).toList();}
    Course classified=x instanceof Course c?c:x instanceof SchoolClass sc?sc.getCourse():null;
    var classification=x instanceof SchoolClass sc?com.example.academic.course.CourseClassificationView.of(sc,em):classified==null?null:com.example.academic.course.CourseClassificationView.of(classified,em);
    return new WebsiteDto.Item(p.getSlug(),code,name,p.getSummary(),p.getAudience(),p.getOutcomes(),p.getTopics()==null?List.of():p.getTopics().lines().filter(s->!s.isBlank()).toList(),p.getImageUrl(),address,phone,p.getOpeningHours(),p.getMapUrl(),slug(Kind.PROGRAM,p.getProgramId()),course,branch,entry,minutes,fee,start,end,p.getUpdatedAt(),branchName,meetings,classification);
  }
  public List<WebsitePublication> edits(Kind kind,int page) {
    if(page<0||page>10000)throw ApiException.bad("Invalid page");
    return em.createQuery("select p from WebsitePublication p where p.kind=:kind order by p.id",WebsitePublication.class).setParameter("kind",kind).getResultStream().filter(p->allowed(kind,p.getSourceId())).skip((long)page*50).limit(50).toList();
  }
  private boolean allowed(Kind kind,Long id){
    var u=Users.current();if(u.role()==com.example.academic.security.Role.SUPER_ADMIN)return true;
    if(u.role()!=com.example.academic.security.Role.ADMIN||kind==Kind.PROGRAM)return false;
    Object x=source(kind,id);Long branch=x instanceof Branch b?b.getId():x instanceof Course c?c.getBranchId():((SchoolClass)x).getBranchId();
    return branch!=null&&u.branchIds().contains(branch);
  }
  public record SourceOption(Long id,String name,Long programId,com.example.academic.course.CourseClassificationView classification,Long branchId) {
    public SourceOption(Long id,String name){this(id,name,null,null,null);}
    public SourceOption(Long id,String name,Long programId,com.example.academic.course.CourseClassificationView classification){this(id,name,programId,classification,null);}
  }
  public java.util.List<com.example.academic.course.CourseClassificationView.Audience> audiences(){return em.createQuery("select a from AudienceGroup a where a.active=true order by a.sortOrder",com.example.academic.course.AudienceGroup.class).getResultStream().filter(a->java.util.Arrays.stream(com.example.academic.course.AudienceGroup.Code.values()).anyMatch(code->code.name().equals(a.getCode()))).map(a->new com.example.academic.course.CourseClassificationView.Audience(a.getCode(),a.getName())).toList();}
  public List<SourceOption> sources(Kind kind,int page){
    if(page<0||page>10000)throw ApiException.bad("Invalid page");
    String type=switch(kind){case PROGRAM->"Program";case BRANCH->"Branch";case COURSE->"Course";case CLASS->"SchoolClass";};
    return em.createQuery("select s.id from "+type+" s order by s.id",Long.class).getResultStream().filter(id->allowed(kind,id)).skip((long)page*100).limit(100).map(id->{Object x=source(kind,id);String name=x instanceof Program a?a.getName():x instanceof Branch a?a.getName():x instanceof Course a?a.getName():((SchoolClass)x).getName();Course metadataCourse=x instanceof Course cc?cc:x instanceof SchoolClass sc?sc.getCourse():null;return new SourceOption(id,name,metadataCourse==null?null:metadataCourse.getProgramId(),metadataCourse==null?null:com.example.academic.course.CourseClassificationView.of(metadataCourse,em),x instanceof Branch b?b.getId():x instanceof Course cc?cc.getBranchId():x instanceof SchoolClass sc?sc.getBranchId():null);}).toList();
  }
  public List<WebsiteEditEvent> history(Kind kind,Long sourceId){if(!allowed(kind,sourceId))throw ApiException.forbidden();return em.createQuery("select e from WebsiteEditEvent e where e.kind=:kind and e.sourceId=:id order by e.id desc",WebsiteEditEvent.class).setParameter("kind",kind.name()).setParameter("id",sourceId).setMaxResults(100).getResultList();}
  @Transactional
  public WebsitePublication edit(Kind kind,WebsiteDto.Edit r) {
    Object original=source(kind,r.sourceId());
    var p=repository.findByKindAndSourceId(kind,r.sourceId()).orElseGet(WebsitePublication::new);
    if(!allowed(kind,r.sourceId()))throw ApiException.forbidden();
    if(Users.current().role()!=com.example.academic.security.Role.SUPER_ADMIN&&(r.state()==State.PUBLISHED||p.getState()==State.PUBLISHED))throw ApiException.forbidden();
    if(p.getVersion()!=r.version())throw ApiException.conflict("Nội dung đã thay đổi. Hãy tải lại.");
    if(repository.existsByKindAndSlugAndSourceIdNot(kind,r.slug(),r.sourceId()))throw ApiException.conflict("Slug đã tồn tại");
    // Preserve published URLs; rename requires a redirect workflow, not an accidental broken link.
    if(p.getId()!=null&&p.getState()==State.PUBLISHED&&!p.getSlug().equals(r.slug()))throw ApiException.conflict("Ẩn nội dung trước khi đổi slug");
    safeUrl(r.imageUrl());safeUrl(r.mapUrl());
    if(kind==Kind.COURSE){
      if(original instanceof Course c&&c.getProgramId()!=null&&!c.getProgramId().equals(r.programId()))throw ApiException.bad("Chương trình website phải khớp chương trình của khóa");
      if(r.programId()==null)throw ApiException.bad("Khóa học cần programId");
      source(Kind.PROGRAM,r.programId());
      var policies=em.createQuery("select a.programId from CourseAdmissionPolicy a where a.courseId=:id order by a.revision desc",Long.class).setParameter("id",r.sourceId()).setMaxResults(1).getResultList();
      if(!policies.isEmpty()&&!policies.getFirst().equals(r.programId()))throw ApiException.bad("Chương trình phải khớp chính sách đầu vào hiện hành");
    }else if(r.programId()!=null||r.showTuition())throw ApiException.bad("programId/showTuition chỉ dành cho khóa học");
    if(r.state()==State.PUBLISHED){
      if(r.summary()==null||r.summary().isBlank())throw ApiException.bad("Cần mô tả trước khi xuất bản");
      if(original instanceof Program a&&!a.isActive()||original instanceof Course c&&c.getStatus()!=Course.Status.OPEN||original instanceof Branch b&&(b.getStatus()!=Branch.Status.ACTIVE||b.getAddress()==null||b.getAddress().isBlank()))throw ApiException.bad("Nguồn chưa đủ điều kiện công khai");
      if(original instanceof SchoolClass a&&(a.getStatus()!=SchoolClass.Status.UPCOMING||!a.getStartDate().isAfter(LocalDate.now(BusinessTime.ZONE))||a.getCurrentSize()>=a.getMaxSize()))throw ApiException.bad("Lớp không còn tuyển sinh");
      if(original instanceof Course a){requirePublished(Kind.PROGRAM,r.programId());if(a.getBranchId()!=null)requirePublished(Kind.BRANCH,a.getBranchId());}
      if(original instanceof SchoolClass a){requirePublished(Kind.COURSE,a.getCourse().getId());requirePublished(Kind.BRANCH,a.getBranchId());if(a.getCourse().getBranchId()!=null&&!a.getCourse().getBranchId().equals(a.getBranchId()))throw ApiException.bad("Chi nhánh lớp phải khớp khóa học");}
    }
    p.setKind(kind);p.setSourceId(r.sourceId());p.setSlug(r.slug());p.setState(r.state());p.setSummary(r.summary());p.setAudience(r.audience());p.setOutcomes(r.outcomes());p.setTopics(r.topics());p.setImageUrl(r.imageUrl());p.setOpeningHours(r.openingHours());p.setMapUrl(r.mapUrl());p.setProgramId(r.programId());p.setShowTuition(r.showTuition());p.setUpdatedAt(LocalDateTime.now(BusinessTime.ZONE));p.setUpdatedBy(Users.current().accountId());
    repository.saveAndFlush(p);
    var event=new WebsiteEditEvent();event.setKind(kind.name());event.setSourceId(r.sourceId());event.setActorId(Users.current().accountId());event.setActorName(Users.current().username());event.setOccurredAt(p.getUpdatedAt());event.setState(p.getState().name());event.setSnapshot(r.toString());em.persist(event);
    return p;
  }
  private void requirePublished(Kind kind,Long id){
    var parent=repository.findByKindAndSourceId(kind,id).orElseThrow(()->ApiException.bad("Cần xuất bản "+kind+" trước"));
    if(query(kind,parent.getSlug(),0,1,null,null,null).isEmpty())throw ApiException.bad("Nội dung cha chưa được công khai: "+kind);
  }
  private void safeUrl(String value){
    if(value==null||value.isBlank())return;
    try{var u=URI.create(value);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException();}
    catch(IllegalArgumentException e){throw ApiException.bad("URL phải là địa chỉ HTTPS hợp lệ");}
  }
}
