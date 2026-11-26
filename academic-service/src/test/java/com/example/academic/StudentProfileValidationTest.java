package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import com.example.academic.common.*;
import com.example.academic.student.*;
import com.example.academic.security.*;
import com.example.academic.support.SupportRequest;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:student-profile-validation;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class StudentProfileValidationTest {
 @Autowired EntityManager em; @Autowired StudentService students;
 final LocalDate today=LocalDate.now(BusinessTime.ZONE);
 StudentDto.Request request(LocalDate birth,String guardian,String phone){return new StudentDto.Request("Student",birth,null,null,"0900000000",null,null,null,1L,guardian,phone);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void minorsNeedBothGuardianFields(){assertThrows(ApiException.class,()->StudentProfileRules.validate(request(today.minusYears(17),null,null)));assertThrows(ApiException.class,()->StudentProfileRules.validate(request(today.minusYears(17),"Parent",null)));assertDoesNotThrow(()->StudentProfileRules.validate(request(today.minusYears(17),"Parent","0900000000")));}
 @Test void eighteenthBirthdayIsAdultAndUnknownBirthdayIsAllowed(){assertDoesNotThrow(()->StudentProfileRules.validate(request(today.minusYears(18),null,null)));assertDoesNotThrow(()->StudentProfileRules.validate(request(null,null,null)));}
 @Test void futureBirthdayAndBadPhoneAreRejected(){assertThrows(ApiException.class,()->StudentProfileRules.validate(request(today.plusDays(1),null,null)));assertThrows(ApiException.class,()->StudentProfileRules.phone("abc123",true));assertThrows(ApiException.class,()->StudentProfileRules.phone("090+---",true));assertThrows(ApiException.class,()->StudentProfileRules.validate(request(today.minusYears(20),null,"0900000000")));}
 @Test void teachingDatesMustFitEmployment(){var t=new TeacherDirectory.Teacher(1L,1L,"Teacher",1L,List.of(1L),today.minusDays(5),today.plusDays(5));assertDoesNotThrow(()->t.requireDuring(today,today.plusDays(5)));assertThrows(ApiException.class,()->t.requireDuring(today.minusDays(6),today));assertThrows(ApiException.class,()->t.requireDuring(today,today.plusDays(6)));}
 void actor(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(1L,1L,1L,Role.ADMIN,"admin"),null,List.of()));}
 @Test void supportHistoryPreventsPhysicalDeletion(){actor();var s=students.save(null,request(today.minusYears(20),null,null));var r=new SupportRequest();r.setStudentId(s.id());r.setBranchId(1L);r.setCategory(SupportRequest.Category.PROFILE);r.setSubject("Contact update");r.setStatus(SupportRequest.Status.OPEN);r.setCreatedAt(LocalDateTime.now());r.setUpdatedAt(LocalDateTime.now());em.persist(r);em.flush();assertThrows(ApiException.class,()->students.delete(s.id()));assertNotNull(em.find(Student.class,s.id()));}
 @Test void sharedFamilyPhoneDoesNotMergeStudents(){actor();var a=students.save(null,request(today.minusYears(20),null,null));var b=students.save(null,request(today.minusYears(21),null,null));assertNotEquals(a.id(),b.id());assertNotEquals(a.studentCode(),b.studentCode());}
}
