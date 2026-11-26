package com.example.academic.course;
import com.example.academic.admission.Program;
import jakarta.persistence.EntityManager;
import java.util.*;
/** Informational metadata, not the student's assessed level or an admission decision. */
public record CourseClassificationView(String programCode,String programName,List<Audience> audiences,boolean complete) {
 public record Audience(String code,String name) {}
 public static CourseClassificationView of(com.example.academic.schoolclass.SchoolClass schoolClass,EntityManager em){
  var current=of(schoolClass.getCourse(),em);
  if(schoolClass.getAdmissionPolicyId()!=null){var policy=em.find(com.example.academic.admission.CourseAdmissionPolicy.class,schoolClass.getAdmissionPolicyId());if(policy!=null)return new CourseClassificationView(policy.getProgramCode(),policy.getProgramName(),current.audiences(),!current.audiences().isEmpty());}
  return current;
 }
 public static CourseClassificationView of(Course course,EntityManager em){
  Program program=course.getProgramId()==null?null:em.find(Program.class,course.getProgramId());
  var groups=course.getAudienceGroups().stream().sorted(Comparator.comparing(AudienceGroup::getSortOrder)).map(a->new Audience(a.getCode(),a.getName())).toList();
  return new CourseClassificationView(program==null?null:program.getCode(),program==null?null:program.getName(),groups,program!=null&&!groups.isEmpty());
 }
}
