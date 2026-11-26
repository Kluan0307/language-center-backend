package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import com.example.academic.course.*;
import com.example.academic.admission.Program;
import com.example.academic.common.ApiException;
import com.example.academic.security.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:course-classification-tests;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class CourseClassificationWorkflowTest {
 @Autowired CourseClassificationService service;@Autowired CourseService courses;@Autowired EntityManager em;
 Program p;AudienceGroup a,b;
 @BeforeEach void setup(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(1L,1L,null,Role.SUPER_ADMIN,"admin"),null,List.of()));p=new Program();p.setCode("GE");p.setName("Tổng quát");em.persist(p);a=audience("PRIMARY");b=audience("SECONDARY");}
 AudienceGroup audience(String code){var g=new AudienceGroup();g.setCode(code);g.setName(code);em.persist(g);return g;}
 CourseDto.Request request(Long program,List<Long> groups,long revision){return new CourseDto.Request("KH1","Khóa 1",null,null,null,new BigDecimal("1000000"),null,null,20,null,null,program,groups,revision);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void requiredAndDuplicateClassificationIsRejected(){assertThrows(ApiException.class,()->service.save(null,request(null,List.of(a.getId()),0)));assertThrows(ApiException.class,()->service.save(null,request(p.getId(),List.of(),0)));assertThrows(ApiException.class,()->service.save(null,request(p.getId(),List.of(a.getId(),a.getId()),0)));}
 @Test void multipleAudiencesAndFilteringDoNotDuplicateCourses(){var c=service.save(null,request(p.getId(),List.of(a.getId(),b.getId()),0));assertTrue(c.classificationComplete());assertEquals(2,c.audiences().size());assertEquals(1,c.classificationRevision());var result=courses.list(0,20,null,null,null,null,null,null,null,true,false,p.getId(),a.getId());assertEquals(1,result.content().size());}
 @Test void staleRevisionIsRejected(){var c=service.save(null,request(p.getId(),List.of(a.getId()),0));assertEquals(409,assertThrows(ApiException.class,()->service.save(c.id(),request(p.getId(),List.of(b.getId()),0))).getStatus().value());}
 @Test void unknownAndInactiveAudiencesAreRejected(){a.setActive(false);em.flush();assertThrows(ApiException.class,()->service.save(null,request(p.getId(),List.of(a.getId()),0)));}
 @Test void inactiveExistingSelectionMayBeRetained(){var c=service.save(null,request(p.getId(),List.of(a.getId()),0));a.setActive(false);em.flush();assertDoesNotThrow(()->service.save(c.id(),request(p.getId(),List.of(a.getId()),c.classificationRevision())));}
 @Test void noncanonicalProgramIsNotSelectable(){p.setCode("CUSTOM");em.flush();assertThrows(ApiException.class,()->service.save(null,request(p.getId(),List.of(a.getId()),0)));}
 @Test void courseWithClassCannotChangeProgram(){
  var c=service.save(null,request(p.getId(),List.of(a.getId()),0));
  var branch=new com.example.academic.branch.Branch();branch.setCode("CN1");branch.setName("CN1");branch.setPhone("0901234567");em.persist(branch);
  var room=new com.example.academic.room.Room();room.setCode("P1");room.setName("Phòng 1");room.setCapacity(20);room.setBranchId(branch.getId());em.persist(room);
  var sc=new com.example.academic.schoolclass.SchoolClass();sc.setCode("L1");sc.setName("Lớp 1");sc.setCourse(em.find(Course.class,c.id()));sc.setRoom(room);sc.setBranchId(branch.getId());sc.setTeacherId(1L);sc.setTeacherName("Giáo viên");sc.setMaxSize(20);sc.setStartDate(java.time.LocalDate.now().plusDays(1));sc.setEndDate(java.time.LocalDate.now().plusDays(30));em.persist(sc);
  var other=new Program();other.setCode("IELTS");other.setName("IELTS");em.persist(other);em.flush();
  assertThrows(ApiException.class,()->service.save(c.id(),request(other.getId(),List.of(a.getId()),c.classificationRevision())));
 }
 @Test void freeTextTypeIsRejectedAndStaffCannotReclassify(){
  var c=service.save(null,request(p.getId(),List.of(a.getId()),0));
  var r=new CourseDto.Request("KH1","Khóa 1","Tùy ý",null,null,new BigDecimal("1000000"),null,null,20,null,null,p.getId(),List.of(a.getId()),c.classificationRevision());
  assertThrows(ApiException.class,()->service.save(c.id(),r));
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(2L,2L,1L,Role.STAFF,"staff"),null,List.of()));
  assertEquals(403,assertThrows(ApiException.class,()->service.save(c.id(),request(p.getId(),List.of(b.getId()),c.classificationRevision()))).getStatus().value());
 }
}
