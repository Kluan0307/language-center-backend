package com.example.teaching.publication;

import com.example.teaching.common.*;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ManagementProgressService {
  private final RemoteApi remote;
  private final EntityManager em;
  private final BranchScope scope;
  @Value("${services.academic.url}") private String academic;

  public record Identity(Long id) {}
  public record Enrollment(Long id,Long studentId,Long classId,Long branchId) {}
  public record Study(Enrollment enrollment) {}
  public record Profile(Identity student,List<Study> studies) {}

  public PageResponse<StudentProgressService.GradeView> grades(Long studentId,Long enrollmentId,int page,int size){
    return grades(studentId,enrollmentId,page,size,null);
  }
  public PageResponse<StudentProgressService.GradeView> grades(Long studentId,Long enrollmentId,int page,int size,java.util.Set<Long> selectedBranches){
    if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.STAFF).contains(Users.current().role()))throw ApiException.forbidden();
    if(studentId==null||studentId<=0||enrollmentId==null||enrollmentId<=0||page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw ApiException.bad("Invalid progress query");
    var proof=remote.get(academic+"/api/v1/academic/students/"+studentId+"/learning-profile",Profile.class);
    if(proof.student()==null||!Objects.equals(proof.student().id(),studentId)||proof.studies()==null)throw ApiException.missing();
    var enrollment=proof.studies().stream().map(Study::enrollment).filter(Objects::nonNull).filter(e->Objects.equals(e.id(),enrollmentId)&&Objects.equals(e.studentId(),studentId)).findFirst().orElseThrow(ApiException::missing);
    if(enrollment.branchId()==null||enrollment.classId()==null)throw ApiException.missing();
    scope.check(enrollment.branchId());
    if(selectedBranches!=null&&!selectedBranches.contains(enrollment.branchId()))throw ApiException.missing();
    String where="g.studentId=:student and g.enrollmentId=:enrollment and p.classId=:class and p.branchId=:branch and p.action='PUBLISH' and not exists(select newer.id from GradePublication newer where newer.exam.id=p.exam.id and newer.revision>p.revision)";
    var q=em.createQuery("select g from PublishedGrade g join fetch g.publication p where "+where+" order by p.examDate desc,p.id desc",PublishedGrade.class);
    var count=em.createQuery("select count(g) from PublishedGrade g join g.publication p where "+where,Long.class);
    for(var query:List.of(q,count)){query.setParameter("student",studentId);query.setParameter("enrollment",enrollmentId);query.setParameter("class",enrollment.classId());query.setParameter("branch",enrollment.branchId());}
    long total=count.getSingleResult();
    var rows=q.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(g->{var p=g.getPublication();return new StudentProgressService.GradeView(p.getExam().getId(),p.getExamName(),p.getExamType(),p.getExamDate(),p.getMaxScore(),p.getWeight(),p.getRevision(),p.getPublishedAt(),g.getScore(),g.getScore()==null?"NOT_GRADED":"GRADED",g.getStudentComment());}).toList();
    return new PageResponse<>(rows,page,size,total,(int)Math.ceil((double)total/size));
  }
}
