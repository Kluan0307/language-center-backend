package com.example.teaching.publication;
import com.example.teaching.common.*;
import com.example.teaching.evaluation.EvaluationState;
import com.example.teaching.results.*;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true,isolation=Isolation.READ_COMMITTED)
public class StudentProgressService {
 private final RemoteApi remote;
 private final EntityManager em;
 private final FinalResultRepository results;
 @Value("${services.academic.url}") private String academicUrl;
 public record Source(Long classId,String token,long sourceRevision) {}
 public record GradeView(Long examId,String examName,String examType,LocalDate examDate,BigDecimal maxScore,BigDecimal weight,long revision,LocalDateTime publishedAt,BigDecimal score,String status,String studentComment) {}
 public record Outcome(String state,String outcome,Long revision,LocalDateTime confirmedAt) {}
 private Source owner(Long id){if(Users.current().role()!=Role.STUDENT)throw ApiException.forbidden();return remote.get(academicUrl+"/api/v1/academic/student/me/enrollments/"+id+"/result-source",Source.class);}
 public PageResponse<GradeView> grades(Long enrollment,int page,int size){if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw ApiException.bad("Invalid page");var source=owner(enrollment);String where="g.studentId=:student and g.enrollmentId=:enrollment and p.classId=:class and p.action='PUBLISH' and not exists(select newer.id from GradePublication newer where newer.exam.id=p.exam.id and newer.revision>p.revision)";var query=em.createQuery("select g from PublishedGrade g join fetch g.publication p where "+where+" order by p.examDate desc,p.id desc",PublishedGrade.class).setParameter("student",Users.current().profileId()).setParameter("enrollment",enrollment).setParameter("class",source.classId());long total=em.createQuery("select count(g) from PublishedGrade g join g.publication p where "+where,Long.class).setParameter("student",Users.current().profileId()).setParameter("enrollment",enrollment).setParameter("class",source.classId()).getSingleResult();var rows=query.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(g->{var p=g.getPublication();return new GradeView(p.getExam().getId(),p.getExamName(),p.getExamType(),p.getExamDate(),p.getMaxScore(),p.getWeight(),p.getRevision(),p.getPublishedAt(),g.getScore(),g.getScore()==null?"NOT_GRADED":"GRADED",g.getStudentComment());}).toList();return new PageResponse<>(rows,page,size,total,(int)Math.ceil((double)total/size));}
 public Outcome outcome(Long enrollment){var source=owner(enrollment);var list=results.findByEnrollmentIdOrderByRevisionDesc(enrollment);if(list.isEmpty())return new Outcome("PENDING",null,null,null);var r=list.getFirst();if(!Objects.equals(r.getStudentId(),Users.current().profileId())||!Objects.equals(r.getClassId(),source.classId()))throw ApiException.missing();var barrier=em.find(ResultBarrier.class,source.classId());var teaching=em.find(EvaluationState.class,source.classId());boolean current=r.getState().equals("CONFIRMED")&&Set.of("PASS","FAIL").contains(r.getOutcome())&&barrier!=null&&Objects.equals(barrier.getToken(),source.token())&&source.sourceRevision()==r.getAcademicSourceRevision()&&barrier.getSourceRevision()==r.getAcademicSourceRevision()&&teaching!=null&&teaching.getSourceRevision()==r.getTeachingSourceRevision();return current?new Outcome("CONFIRMED",r.getOutcome(),r.getRevision(),r.getConfirmedAt()):new Outcome("UPDATING",null,null,null);}
}
