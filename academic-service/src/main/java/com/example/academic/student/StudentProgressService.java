package com.example.academic.student;
import com.example.academic.attendance.Attendance;
import com.example.academic.common.*;
import com.example.academic.enrollment.*;
import com.example.academic.results.ResultBarrier;
import com.example.academic.security.*;
import com.example.academic.session.ClassSession;
import jakarta.persistence.EntityManager;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class StudentProgressService {
 private final StudentRepository students;
 private final EntityManager em;
 public record Source(Long classId,String token,long sourceRevision) {}
 public record Entry(Long sessionId,Long attendanceId,LocalDate date,LocalTime startTime,LocalTime endTime,String status,boolean hasPermission,String source) {}
 public record Summary(int eligibleCompleted,int present,int late,int absent,int notRecorded,int legacyUnverified,boolean coverageComplete,BigDecimal attendancePercent) {}
 public record AttendancePage(List<Entry> content,int page,int size,long totalElements,int totalPages,Summary summary) {}
 private Enrollment owned(Long id){var u=Users.current();if(u.role()!=Role.STUDENT)throw ApiException.forbidden();var s=students.findByAccountId(u.accountId()).orElseThrow(ApiException::forbidden);if(!s.getId().equals(u.profileId()))throw ApiException.forbidden();return em.createQuery("select e from Enrollment e join fetch e.schoolClass where e.id=:id and e.student.id=:student",Enrollment.class).setParameter("id",id).setParameter("student",s.getId()).getResultStream().findFirst().orElseThrow(ApiException::missing);}
 public Source source(Long id){var e=owned(id);var b=em.find(ResultBarrier.class,e.getSchoolClass().getId());return new Source(e.getSchoolClass().getId(),b==null?null:b.getToken(),b==null?0:b.getSourceRevision());}
 public AttendancePage attendance(Long id,int page,int size,LocalDate from,LocalDate to){if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE||from!=null&&to!=null&&to.isBefore(from))throw ApiException.bad("Invalid page or date range");var e=owned(id);var ps=em.createQuery("select p from EnrollmentPeriod p where p.enrollment.id=:id",EnrollmentPeriod.class).setParameter("id",id).getResultList();var sessions=em.createQuery("select s from ClassSession s where s.schoolClass.id=:class and s.status='COMPLETED' and s.sessionDate<=:today order by s.sessionDate,s.id",ClassSession.class).setParameter("class",e.getSchoolClass().getId()).setParameter("today",LocalDate.now(BusinessTime.ZONE)).getResultList();var records=em.createQuery("select a from Attendance a left join fetch a.classSession where a.enrollment.id=:id",Attendance.class).setParameter("id",id).getResultList();return assemble(e,ps,sessions,records,page,size,from,to);}
 public static AttendancePage assemble(Enrollment e,List<EnrollmentPeriod> periods,List<ClassSession> sessions,List<Attendance> records,int page,int size,LocalDate from,LocalDate to){var rows=new ArrayList<Entry>();Set<Long> consumed=new HashSet<>();int eligible=0,present=0,late=0,absent=0,missing=0,legacy=0;boolean tracked=!periods.isEmpty();for(var s:sessions){boolean verified=periods.stream().anyMatch(p->p.includes(s.getSessionDate()));boolean estimated=!tracked&&e.getStudyStatus()==Enrollment.StudyStatus.STUDYING&&!s.getSessionDate().isBefore(e.getEnrolledAt().toLocalDate())&&!s.getSessionDate().isBefore(e.getSchoolClass().getStartDate());var matching=records.stream().filter(a->a.getClassSession()!=null&&a.getClassSession().getId().equals(s.getId())&&a.getSessionDate().equals(s.getSessionDate())).toList();if(!verified&&!estimated&&matching.isEmpty())continue;var a=matching.size()==1?matching.getFirst():null;boolean official=verified&&a!=null;String status=a==null?"NOT_RECORDED":a.getStatus().name();if(a!=null)consumed.add(a.getId());if(verified){eligible++;if(a==null)missing++;else switch(a.getStatus()){case PRESENT->present++;case LATE->late++;case ABSENT->absent++;}}if(!verified){legacy++;}rows.add(new Entry(s.getId(),a==null?null:a.getId(),s.getSessionDate(),s.getStartTime(),s.getEndTime(),status,a!=null&&a.isHasPermission(),verified?"VERIFIED":"LEGACY_UNVERIFIED"));}
  for(var a:records)if(!consumed.contains(a.getId())){legacy++;rows.add(new Entry(null,a.getId(),a.getSessionDate(),null,null,a.getStatus().name(),a.isHasPermission(),"LEGACY_UNVERIFIED"));}
  boolean complete=tracked&&missing==0&&legacy==0;BigDecimal percent=complete&&eligible>0?BigDecimal.valueOf((present+late)*100L).divide(BigDecimal.valueOf(eligible),2,RoundingMode.HALF_UP):null;var summary=new Summary(eligible,present,late,absent,missing,legacy,complete,percent);var filtered=rows.stream().filter(r->(from==null||!r.date().isBefore(from))&&(to==null||!r.date().isAfter(to))).sorted(Comparator.comparing(Entry::date).reversed().thenComparing(r->r.sessionId()==null?Long.MAX_VALUE:r.sessionId())).toList();int start=Math.min(filtered.size(),page*size),end=Math.min(filtered.size(),start+size);return new AttendancePage(filtered.subList(start,end),page,size,filtered.size(),(int)Math.ceil((double)filtered.size()/size),summary);
 }
}
