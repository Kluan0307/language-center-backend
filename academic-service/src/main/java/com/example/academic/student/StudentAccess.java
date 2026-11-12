package com.example.academic.student;
import com.example.academic.common.*;
import com.example.academic.enrollment.*;
import com.example.academic.security.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class StudentAccess {
 private final StudentRepository students; private final EntityManager em;
 public Student owner(){var u=Users.current();if(u.role()!=Role.STUDENT)throw ApiException.forbidden();var s=students.findByAccountId(u.accountId()).orElseThrow(ApiException::forbidden);if(!Objects.equals(s.getId(),u.profileId()))throw ApiException.forbidden();return s;}
 public Enrollment enrollment(Long id){var s=owner();return em.createQuery("select e from Enrollment e join fetch e.schoolClass c join fetch c.course where e.id=:id and e.student.id=:student",Enrollment.class).setParameter("id",id).setParameter("student",s.getId()).getResultStream().findFirst().orElseThrow(ApiException::missing);}
 public record DocumentScope(Long enrollmentId,Long classId,Long courseId,Long branchId) {}
 public List<DocumentScope> documents(Long enrollmentId){var s=owner();if(enrollmentId!=null)enrollment(enrollmentId);var rows=em.createQuery("select e from Enrollment e join fetch e.schoolClass c join fetch c.course where e.student.id=:student"+(enrollmentId==null?"":" and e.id=:id"),Enrollment.class).setParameter("student",s.getId());if(enrollmentId!=null)rows.setParameter("id",enrollmentId);var all=rows.getResultList();var periods=em.createQuery("select p from EnrollmentPeriod p join fetch p.enrollment e where e.student.id=:student",EnrollmentPeriod.class).setParameter("student",s.getId()).getResultList();var today=LocalDate.now(BusinessTime.ZONE);return all.stream().filter(e->{if(e.getEnrolledAt().toLocalDate().isAfter(today))return false;if(e.getStudyStatus()!=Enrollment.StudyStatus.DROPPED)return true;return periods.stream().anyMatch(p->{var start=p.getActiveFrom().isAfter(e.getSchoolClass().getStartDate())?p.getActiveFrom():e.getSchoolClass().getStartDate();return p.getEnrollment().getId().equals(e.getId())&&!start.isAfter(today)&&(p.getActiveUntil()==null||p.getActiveUntil().isAfter(start));});}).map(e->new DocumentScope(e.getId(),e.getSchoolClass().getId(),e.getSchoolClass().getCourse().getId(),e.getSchoolClass().getBranchId())).toList();}
}
