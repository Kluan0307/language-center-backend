package com.example.academic.notification;

import com.example.academic.common.BusinessTime;
import com.example.academic.enrollment.*;
import com.example.academic.student.Student;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor
public class NotificationDelivery {
 private final EntityManager em;
 @org.springframework.beans.factory.annotation.Value("${notifications.push.enabled:false}") private boolean pushEnabled;
 private boolean on(Enrollment e,LocalDate date){if(date==null||e.getEnrolledAt().toLocalDate().isAfter(date)||e.getSchoolClass().getStartDate().isAfter(date))return false;var periods=em.createQuery("select p from EnrollmentPeriod p where p.enrollment.id=:id",EnrollmentPeriod.class).setParameter("id",e.getId()).getResultList();if(periods.isEmpty())return e.getStudyStatus()==Enrollment.StudyStatus.STUDYING;return periods.stream().anyMatch(p->!p.getActiveFrom().isAfter(date)&&(p.getActiveUntil()==null||date.isBefore(p.getActiveUntil())));}
 private Set<Student> recipients(StudentEvent event){var result=new LinkedHashMap<Long,Student>();if(event.getStudentId()!=null){var s=em.find(Student.class,event.getStudentId());if(s!=null)result.put(s.getId(),s);return new HashSet<>(result.values());}
  String where=event.getClassId()!=null?"e.schoolClass.id=:scope":"e.schoolClass.course.id=:scope and e.schoolClass.branchId=:branch";var q=em.createQuery("select e from Enrollment e join fetch e.student join fetch e.schoolClass where "+where+" and e.enrolledAt<=:at",Enrollment.class).setParameter("scope",event.getClassId()!=null?event.getClassId():event.getCourseId()).setParameter("at",event.getOccurredAt());if(event.getClassId()==null)q.setParameter("branch",event.getBranchId());
  for(var e:q.getResultList()){boolean allowed;if(event.getCategory().equals("SCHEDULE")){allowed=on(e,event.getSessionDate())||on(e,event.getPreviousDate());if(!allowed&&event.getSessionDate()!=null&&!event.getSessionDate().isAfter(LocalDate.now(BusinessTime.ZONE)))allowed=em.createQuery("select count(a) from Attendance a where a.enrollment.id=:enrollment and a.classSession.id=:session",Long.class).setParameter("enrollment",e.getId()).setParameter("session",Long.valueOf(event.getResourceId())).getSingleResult()>0;}else{allowed=e.getStudyStatus()!=Enrollment.StudyStatus.DROPPED||em.createQuery("select p from EnrollmentPeriod p where p.enrollment.id=:id",EnrollmentPeriod.class).setParameter("id",e.getId()).getResultList().stream().anyMatch(p->{var start=p.getActiveFrom().isAfter(e.getSchoolClass().getStartDate())?p.getActiveFrom():e.getSchoolClass().getStartDate();return !start.isAfter(event.getOccurredAt().toLocalDate())&&(p.getActiveUntil()==null||p.getActiveUntil().isAfter(start));});}if(allowed)result.put(e.getStudent().getId(),e.getStudent());}return new HashSet<>(result.values());
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED) public void deliver(String id){var e=em.find(StudentEvent.class,id,LockModeType.PESSIMISTIC_WRITE);if(e==null||!e.getStatus().equals("PENDING"))return;int count=0;for(var s:recipients(e)){if(s.getAccountId()==null)continue;var n=new StudentNotification();n.setEventId(e.getId());n.setAccountId(s.getAccountId());n.setStudentId(s.getId());n.setCategory(e.getCategory());n.setType(e.getType());n.setTitle(e.getTitle());n.setBody(e.getBody());n.setResourceType(e.getResourceType());n.setResourceId(e.getResourceId());n.setCreatedAt(e.getOccurredAt());em.persist(n);if(pushEnabled){var prefs=em.find(NotificationPreference.class,n.getAccountId());if(prefs!=null&&prefs.allows(n.getCategory()))for(var d:em.createQuery("select d from PushDevice d where d.accountId=:account and d.enabled=true",PushDevice.class).setParameter("account",n.getAccountId()).getResultList()){var push=new PushDelivery();push.setNotificationId(n.getId());push.setInstallationId(d.getInstallationId());push.setBindingVersion(d.getBindingVersion());em.persist(push);}}count++;}e.setRecipientCount(count);e.setDeliveredAt(LocalDateTime.now(BusinessTime.ZONE));e.setStatus(count==0?"SKIPPED":"DONE");}
}
