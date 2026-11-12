package com.example.academic.student;

import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.enrollment.*;
import com.example.academic.lifecycle.*;
import com.example.academic.security.*;
import com.example.academic.session.ClassSession;
import jakarta.persistence.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class StudentLearningService {
 private final StudentRepository students;
 private final BranchRepository branches;
 private final EnrollmentPeriodRepository periods;
 private final LearningEventRepository events;
 private final EntityManager em;
 public enum Group { ALL, CURRENT, HISTORY }
 public record BranchView(Long id,String name,String phone,String address) {}
 public record EnrollmentView(Long id,Long classId,String classCode,String className,String courseName,String teacherName,String roomName,BranchView branch,Enrollment.StudyStatus status,String classStatus,LocalDate startDate,LocalDate endDate,LocalDateTime enrolledAt) {}
 public record PeriodView(LocalDate activeFrom,LocalDate activeUntil) {}
 public record PublicEvent(String action,String oldStatus,String newStatus,LocalDate effectiveDate,LocalDate expectedReturnDate) {}
 public record Detail(EnrollmentView enrollment,List<PeriodView> periods,String coverage,List<PublicEvent> publicEvents) {}
 public record SessionView(Long sessionId,Long enrollmentId,List<Long> enrollmentIds,Long classId,String classCode,String className,String courseName,BranchView branch,LocalDate date,LocalTime startTime,LocalTime endTime,ClassSession.Status status,ClassSession.Kind kind,boolean isMakeup,boolean teacherChanged,String teacherName,String roomName,boolean legacyEstimated,long version,java.time.OffsetDateTime updatedAt) {}
 public record Next(SessionView session,OffsetDateTime asOf,String timezone) {}
 private static final String FETCH_ENROLLMENT="select e from Enrollment e join fetch e.schoolClass c join fetch c.course join fetch c.room where ";
 // Exactly the same ownership/eligibility predicate is used for count, page, detail and next session.
 private static final String ELIGIBLE="e.student.id=:student and e.schoolClass.id=s.schoolClass.id and "
  +"(exists(select p.id from EnrollmentPeriod p where p.enrollment.id=e.id and p.activeFrom<=s.sessionDate and (p.activeUntil is null or s.sessionDate<p.activeUntil)) "
  +"or (e.studyStatus='STUDYING' and not exists(select p.id from EnrollmentPeriod p where p.enrollment.id=e.id) and s.sessionDate>=cast(e.enrolledAt as LocalDate) and s.sessionDate>=s.schoolClass.startDate) "
  +"or (s.sessionDate<=:today and exists(select a.id from Attendance a where a.enrollment.id=e.id and a.classSession.id=s.id)))";
 private Student owner(){var u=Users.current();if(u.role()!=Role.STUDENT)throw ApiException.forbidden();var s=students.findByAccountId(u.accountId()).orElseThrow(ApiException::forbidden);if(!s.getId().equals(u.profileId()))throw ApiException.forbidden();return s;}
 private static void page(int page,int size){if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw ApiException.bad("page >=0, size 1..100");}
 private Map<Long,BranchView> branchMap(Collection<Long> ids){return branches.findAllById(new HashSet<>(ids)).stream().collect(Collectors.toMap(Branch::getId,b->new BranchView(b.getId(),b.getName(),b.getPhone(),b.getAddress())));}
 private EnrollmentView enrollment(Enrollment e,Map<Long,BranchView> bs){var c=e.getSchoolClass();var b=bs.get(c.getBranchId());if(b==null)throw ApiException.missing();return new EnrollmentView(e.getId(),c.getId(),c.getCode(),c.getName(),c.getCourse().getName(),c.getTeacherName(),c.getRoom().getName(),b,e.getStudyStatus(),c.effectiveStatus().name(),c.getStartDate(),c.effectiveEndDate(),e.getEnrolledAt());}
 private Enrollment require(Long id,Long student){return em.createQuery(FETCH_ENROLLMENT+"e.id=:id and e.student.id=:student",Enrollment.class).setParameter("id",id).setParameter("student",student).getResultStream().findFirst().orElseThrow(ApiException::missing);}
 public PageResponse<EnrollmentView> enrollments(int page,int size,Group group,Enrollment.StudyStatus status){page(page,size);var s=owner();String where="e.student.id=:student";if(group==Group.CURRENT)where+=" and e.studyStatus in ('STUDYING','RESERVED')";if(group==Group.HISTORY)where+=" and e.studyStatus not in ('STUDYING','RESERVED')";if(status!=null)where+=" and e.studyStatus=:status";
  var query=em.createQuery(FETCH_ENROLLMENT+where+" order by e.enrolledAt desc,e.id desc",Enrollment.class).setParameter("student",s.getId());var count=em.createQuery("select count(e) from Enrollment e where "+where,Long.class).setParameter("student",s.getId());if(status!=null){query.setParameter("status",status);count.setParameter("status",status);}var rows=query.setFirstResult(page*size).setMaxResults(size).getResultList();var bs=branchMap(rows.stream().map(e->e.getSchoolClass().getBranchId()).toList());long total=count.getSingleResult();return new PageResponse<>(rows.stream().map(e->enrollment(e,bs)).toList(),page,size,total,(int)Math.ceil((double)total/size));
 }
 public Detail detail(Long id){var s=owner();var e=require(id,s.getId());var ps=periods.findByEnrollmentId(id).stream().sorted(Comparator.comparing(EnrollmentPeriod::getActiveFrom).thenComparing(EnrollmentPeriod::getId)).map(p->new PeriodView(p.getActiveFrom(),p.getActiveUntil())).toList();String coverage=!ps.isEmpty()?"VERIFIED_PERIODS":e.getStudyStatus()==Enrollment.StudyStatus.STUDYING?"LEGACY_ESTIMATED":"LEGACY_UNVERIFIED";
  var allowed=Set.of("ENROLLED","TRANSFER_IN","TRANSFER_OUT","RESERVED","RESUMED","RESERVATION_UPDATED","DROPPED","COMPLETED");var history=events.findByEnrollmentIdOrderByIdDesc(id).stream().filter(v->v.getStudentId().equals(s.getId())&&allowed.contains(v.getAction())).map(v->new PublicEvent(v.getAction(),v.getOldStatus(),v.getNewStatus(),v.getEffectiveDate(),v.getExpectedReturnDate())).toList();return new Detail(enrollment(e,branchMap(List.of(e.getSchoolClass().getBranchId()))),ps,coverage,history);
 }
 private String predicate(Long enrollment){return "exists(select e.id from Enrollment e where "+ELIGIBLE+(enrollment==null?"":" and e.id=:enrollment")+")";}
 private <T> TypedQuery<T> bind(TypedQuery<T> q,Long student,Long enrollment){q.setParameter("student",student);q.setParameter("today",LocalDate.now(BusinessTime.ZONE));if(enrollment!=null)q.setParameter("enrollment",enrollment);return q;}
 private List<SessionView> views(List<ClassSession> sessions,Long student,Long selected){if(sessions.isEmpty())return List.of();var sessionIds=sessions.stream().map(ClassSession::getId).toList();var pairs=bind(em.createQuery("select s.id,e from ClassSession s,Enrollment e where s.id in :ids and "+ELIGIBLE+(selected==null?"":" and e.id=:enrollment"),Object[].class).setParameter("ids",sessionIds),student,selected).getResultList();Map<Long,List<Enrollment>> bySession=new HashMap<>();for(var pair:pairs)bySession.computeIfAbsent((Long)pair[0],key->new ArrayList<>()).add((Enrollment)pair[1]);var enrollmentIds=pairs.stream().map(r->((Enrollment)r[1]).getId()).distinct().toList();Set<Long> tracked=enrollmentIds.isEmpty()?Set.of():new HashSet<>(em.createQuery("select distinct p.enrollment.id from EnrollmentPeriod p where p.enrollment.id in :ids",Long.class).setParameter("ids",enrollmentIds).getResultList());var bs=branchMap(sessions.stream().map(v->v.getSchoolClass().getBranchId()).toList());
  return sessions.stream().map(v->{var c=v.getSchoolClass();var eligible=bySession.getOrDefault(v.getId(),List.of());var ids=eligible.stream().map(Enrollment::getId).distinct().sorted().toList();if(ids.isEmpty())throw ApiException.conflict("Schedule source changed; reload");var b=bs.get(c.getBranchId());if(b==null)throw ApiException.missing();return new SessionView(v.getId(),ids.getFirst(),ids,c.getId(),c.getCode(),c.getName(),c.getCourse().getName(),b,v.getSessionDate(),v.getStartTime(),v.getEndTime(),v.getStatus(),v.getKind(),v.getKind()==ClassSession.Kind.MAKEUP,v.getTeacherId()!=null&&v.getPlannedTeacherId()!=null&&!v.getTeacherId().equals(v.getPlannedTeacherId()),v.getTeacherName()!=null?v.getTeacherName():v.getPlannedTeacherName(),v.getRoomName(),eligible.stream().anyMatch(e->!tracked.contains(e.getId())&&e.getStudyStatus()==Enrollment.StudyStatus.STUDYING),v.getVersion(),v.getUpdatedAt()==null?null:v.getUpdatedAt().atZone(BusinessTime.ZONE).toOffsetDateTime());}).toList();
 }
 public PageResponse<SessionView> schedule(LocalDate from,LocalDate to,Long enrollment,int page,int size){page(page,size);if(from==null||to==null||to.isBefore(from)||ChronoUnit.DAYS.between(from,to)>91)throw ApiException.bad("Use an inclusive date window of at most92days");var s=owner();if(enrollment!=null)require(enrollment,s.getId());String where="s.sessionDate between :from and :to and "+predicate(enrollment);var query=bind(em.createQuery("select s from ClassSession s join fetch s.schoolClass c join fetch c.course where "+where+" order by s.sessionDate,s.startTime,s.id",ClassSession.class),s.getId(),enrollment).setParameter("from",from).setParameter("to",to);long count=bind(em.createQuery("select count(s) from ClassSession s where "+where,Long.class),s.getId(),enrollment).setParameter("from",from).setParameter("to",to).getSingleResult();var rows=query.setFirstResult(page*size).setMaxResults(size).getResultList();return new PageResponse<>(views(rows,s.getId(),enrollment),page,size,count,(int)Math.ceil((double)count/size));}
 public SessionView session(Long id){var s=owner();var rows=bind(em.createQuery("select s from ClassSession s join fetch s.schoolClass c join fetch c.course where s.id=:id and "+predicate(null),ClassSession.class),s.getId(),null).setParameter("id",id).getResultList();if(rows.isEmpty())throw ApiException.missing();return views(rows,s.getId(),null).getFirst();}
 public Next next(){var student=owner();var now=OffsetDateTime.now(BusinessTime.ZONE);var rows=bind(em.createQuery("select s from ClassSession s join fetch s.schoolClass c join fetch c.course where s.status='SCHEDULED' and s.startTime is not null and s.endTime>s.startTime and (s.sessionDate>:today or (s.sessionDate=:today and s.endTime>:time)) and "+predicate(null)+" order by s.sessionDate,s.startTime,s.id",ClassSession.class),student.getId(),null).setParameter("time",now.toLocalTime()).setParameter("today",now.toLocalDate()).setMaxResults(1).getResultList();var result=views(rows,student.getId(),null);return new Next(result.isEmpty()?null:result.getFirst(),now,BusinessTime.ZONE.getId());}
}
