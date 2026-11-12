package com.example.academic.notification;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
/** Shared-schema durable event contract. Persisted in the caller's business transaction. */
public final class StudentEvents {
 private StudentEvents() {}
 public static void add(EntityManager em,String key,String category,String type,String title,String body,Long student,Long enrollment,Long school,Long course,Long branch,String resource,String resourceId,LocalDate date,LocalDate previous){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();if(em.find(StudentEvent.class,id)!=null)return;
  var e=new StudentEvent();e.setId(id);e.setSourceKey(key);e.setCategory(category);e.setType(type);e.setTitle(title);e.setBody(body);e.setStudentId(student);e.setEnrollmentId(enrollment);e.setClassId(school);e.setCourseId(course);e.setBranchId(branch);e.setResourceType(resource);e.setResourceId(resourceId);e.setSessionDate(date);e.setPreviousDate(previous);e.setOccurredAt(LocalDateTime.now(com.example.academic.common.BusinessTime.ZONE));em.persist(e);
 }
}
