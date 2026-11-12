package com.example.academic.student;
import com.example.academic.common.ApiException;
import com.example.academic.enrollment.Enrollment;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/academic/student/me") @RequiredArgsConstructor @PreAuthorize("hasRole('STUDENT')")
public class StudentBillingController {
 private final StudentAccess access; private final EntityManager em;
 public record Registration(Long enrollmentId,Long billingRootId,Long classId,String className,Long courseId,String courseName,Long branchId) {}
 public record Scopes(Long studentId,List<Registration> registrations) {}
 @GetMapping("/billing-scopes") @Transactional(readOnly=true)
 public Scopes scopes(){var student=access.owner();var rows=em.createQuery("select e from Enrollment e join fetch e.schoolClass c join fetch c.course where e.student.id=:student order by e.id",Enrollment.class).setParameter("student",student.getId()).getResultList();var map=new HashMap<Long,Enrollment>();rows.forEach(e->map.put(e.getId(),e));return new Scopes(student.getId(),rows.stream().map(e->{Long root=root(e,map);var c=e.getSchoolClass();return new Registration(e.getId(),root,c.getId(),c.getName(),c.getCourse().getId(),c.getCourse().getName(),c.getBranchId());}).toList());}
 public static Long root(Enrollment e,Map<Long,Enrollment> owned){var seen=new HashSet<Long>();while(true){if(!seen.add(e.getId()))throw ApiException.conflict("Invalid billing chain; contact the center");Long next=e.getBillingEnrollmentId();if(next==null||next.equals(e.getId()))return e.getId();e=owned.get(next);if(e==null)throw ApiException.conflict("Unverified billing owner; contact the center");}}
}
