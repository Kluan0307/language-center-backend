package com.example.academic.website;

import com.example.academic.branch.Branch;
import com.example.academic.course.Course;
import com.example.academic.lead.*;
import com.example.academic.schoolclass.SchoolClass;
import com.example.academic.common.*;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import static com.example.academic.website.WebsitePublication.Kind.*;

@Service @RequiredArgsConstructor
public class WebsiteIntakeService {
  public static final String CONSENT_VERSION="website-consultation-v1";
  private final WebsiteCatalogService catalog;
  private final WebsitePublicationRepository publications;
  private final WebsiteInquiryRepository inquiries;
  private final LeadRepository leads;
  private final LeadActivityRepository activities;
  private final WebsiteLeadRouting routing;
  private final WebsiteIntakeLimiter limiter;
  private final JdbcTemplate jdbc;
  private final EntityManager em;

  public static String normalizePhone(String input){
    String phone=input==null?"":input.replaceAll("[\\s.()-]","");
    if(phone.startsWith("+84"))phone="0"+phone.substring(3);
    if(!phone.matches("0[35789][0-9]{8}"))throw ApiException.bad("Nhập số điện thoại di động Việt Nam hợp lệ.");
    return phone;
  }
  public static String hash(String input){
    try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}
    catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  }
  private String text(String value){return value==null?"":value.trim().replaceAll("\\s+"," ");}
  private WebsitePublication selected(WebsitePublication.Kind kind,String slug){
    try{catalog.detail(kind,slug);}catch(ApiException e){throw ApiException.bad("Lựa chọn đã thay đổi. Vui lòng chọn lại chương trình, khóa học hoặc chi nhánh.");}
    return publications.findByKindAndSlug(kind,slug).orElseThrow(ApiException::missing);
  }
  @Transactional(isolation=Isolation.READ_COMMITTED)
  public WebsiteIntakeDto.Accepted submit(WebsiteIntakeDto.Request r){
    if(!Boolean.TRUE.equals(r.consent())||r.website()!=null&&!r.website().isEmpty())throw ApiException.bad("Vui lòng kiểm tra thông tin đăng ký.");
    String phone=normalizePhone(r.phone()),name=text(r.fullName());
    if(name.length()<2)throw ApiException.bad("Vui lòng nhập họ tên.");
    String fingerprint=hash(String.join("\n",name,phone,text(r.email()).toLowerCase(Locale.ROOT),r.branchSlug(),text(r.programSlug()),text(r.courseSlug()),text(r.classSlug()),text(r.note()),CONSENT_VERSION));
    String key=r.requestId().toLowerCase(Locale.ROOT);
    var prior=inquiries.findById(key);
    if(prior.isPresent())return replay(prior.get(),fingerprint);
    var branchPublication=selected(BRANCH,r.branchSlug());
    // All submissions within a branch serialize deduplication and load-based routing.
    var branch=em.find(Branch.class,branchPublication.getSourceId(),LockModeType.PESSIMISTIC_WRITE);
    em.refresh(branch,LockModeType.PESSIMISTIC_WRITE);
    if(branch.getStatus()!=Branch.Status.ACTIVE)throw ApiException.bad("Chi nhánh tạm ngừng tiếp nhận.");
    prior=inquiries.findById(key);if(prior.isPresent())return replay(prior.get(),fingerprint);
    WebsitePublication program=r.programSlug()==null?null:selected(PROGRAM,r.programSlug());
    WebsitePublication coursePublication=r.courseSlug()==null?null:selected(COURSE,r.courseSlug());
    Course course=coursePublication==null?null:em.find(Course.class,coursePublication.getSourceId());
    if(course!=null){
      if(course.getBranchId()!=null&&!course.getBranchId().equals(branch.getId()))throw ApiException.bad("Khóa học không thuộc chi nhánh đã chọn.");
      if(program!=null&&!program.getSourceId().equals(coursePublication.getProgramId()))throw ApiException.bad("Khóa học không thuộc chương trình đã chọn.");
      if(program==null)program=publications.findByKindAndSourceId(PROGRAM,coursePublication.getProgramId()).orElseThrow(ApiException::missing);
    }
    SchoolClass preferredClass=null;
    if(r.classSlug()!=null){
      var cp=selected(CLASS,r.classSlug());var sc=em.find(SchoolClass.class,cp.getSourceId());preferredClass=sc;
      if(course==null||!sc.getCourse().getId().equals(course.getId())||!sc.getBranchId().equals(branch.getId()))throw ApiException.bad("Lớp học không khớp khóa học và chi nhánh.");
    }
    String phoneHash=hash(phone);var now=LocalDateTime.now(BusinessTime.ZONE);
    // Durable count survives process restarts; in-memory bucket also bounds concurrent branches.
    if(inquiries.countByPhoneHashAndCreatedAtAfter(phoneHash,now.minusHours(1))>=3)throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Bạn đã gửi nhiều yêu cầu. Vui lòng thử lại sau.");
    limiter.take("phone:"+phoneHash,3,Duration.ofHours(1));
    var ids=jdbc.queryForList("""
      select id from leads where branch_id=? and status in ('NEW','CONTACTED','CONSULTING')
      and replace(replace(replace(replace(replace(replace(phone,' ',''),'-',''),'.',''),'(',''),')',''),'+84','0')=?
      order by id desc limit 1
      """,Long.class,branch.getId(),phone);
    Lead lead=ids.isEmpty()?new Lead():em.find(Lead.class,ids.getFirst(),LockModeType.PESSIMISTIC_WRITE);
    boolean fresh=lead.getId()==null;
    var recipient=routing.choose(branch.getId(),fresh?null:lead.getAssignedTo());
    Long oldOwner=lead.getAssignedTo();String oldOwnerName=lead.getAssignedToName();
    lead.setAssignedTo(recipient.id());lead.setAssignedToName(recipient.name());
    String description="Đăng ký tư vấn từ website. Họ tên gửi: "+name
      +(program==null?"":"; Chương trình: "+catalog.detail(PROGRAM,program.getSlug()).name())
      +(course==null?"":"; Khóa học: "+course.getName())
      +(r.classSlug()==null?"":"; Lớp: "+r.classSlug())
      +(text(r.note()).isEmpty()?"":"; Nhu cầu: "+text(r.note()));
    if(fresh){lead.setFullName(name);lead.setPhone(phone);lead.setEmail(text(r.email()).isEmpty()?null:text(r.email()));lead.setBranchId(branch.getId());lead.setSource("WEBSITE");lead.setInterestedCourse(course);lead.setPreferredClass(preferredClass);lead.setNote(description);}
    // A new inquiry never overwrites an existing prospect's profile, ownership scope or status.
    lead.setUpdatedAt(now);leads.saveAndFlush(lead);
    var event=new LeadActivity();event.setLeadId(lead.getId());event.setAction(fresh?"CREATED":"WEBSITE_INQUIRY");event.setNote(description);event.setChannel("WEBSITE");event.setNewStatus(lead.getStatus().name());event.setAssigneeId(lead.getAssignedTo());event.setAssigneeName(lead.getAssignedToName());event.setOldAssigneeId(oldOwner);event.setOldAssigneeName(oldOwnerName);event.setActorId(0L);event.setActorName("Website");event.setOccurredAt(now);activities.save(event);
    var receipt=new WebsiteInquiry();receipt.setRequestId(key);receipt.setFingerprint(fingerprint);receipt.setPhoneHash(phoneHash);receipt.setLeadId(lead.getId());receipt.setBranchId(branch.getId());receipt.setProgramSlug(program==null?null:program.getSlug());receipt.setCourseSlug(r.courseSlug());receipt.setClassSlug(r.classSlug());receipt.setConsentVersion(CONSENT_VERSION);receipt.setCreatedAt(now);inquiries.saveAndFlush(receipt);
    return WebsiteIntakeDto.Accepted.received();
  }
  private WebsiteIntakeDto.Accepted replay(WebsiteInquiry previous,String fingerprint){
    if(!previous.getFingerprint().equals(fingerprint))throw ApiException.conflict("Nội dung yêu cầu đã thay đổi. Vui lòng tạo lượt gửi mới.");
    return WebsiteIntakeDto.Accepted.received();
  }
}
