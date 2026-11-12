package com.example.teaching;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.example.teaching.common.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.publication.*;
import com.example.teaching.results.*;
import com.example.teaching.evaluation.*;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:student-publication;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class StudentPublicationTest {
 @Autowired GradePublicationService publication;
 @Autowired StudentProgressService student;
 @Autowired EntityManager em;
 @MockitoBean AcademicDirectory academic;
 @MockitoBean ExamService exams;
 @MockitoBean RemoteApi remote;
 Exam exam;Grade grade;
 void actor(Role role,Long id){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(100L,id,3L,role,"test"),null,List.of()));}
 @BeforeEach void setup(){actor(Role.TEACHER,2L);exam=new Exam();exam.setClassId(9L);exam.setBranchId(3L);exam.setName("Exam");exam.setExamType("FINAL");exam.setExamDate(LocalDate.now(BusinessTime.ZONE).minusDays(1));exam.setMaxScore(BigDecimal.TEN);exam.setWeight(BigDecimal.ONE);exam.setRosterFrozenAt(LocalDateTime.now());em.persist(exam);when(exams.require(exam.getId())).thenReturn(exam);for(long i=11;i<=12;i++){var c=new ExamCandidate();c.setExamId(exam.getId());c.setStudentId(i);c.setEnrollmentId(i+100);c.setStudentName("Student"+i);c.setSource("VERIFIED");em.persist(c);}grade=new Grade();grade.setExam(exam);grade.setBranchId(3L);grade.setStudentId(11L);grade.setStudentName("Student11");grade.setEnteredBy(100L);grade.setEnteredByName("Teacher");grade.setScore(new BigDecimal("8.00"));grade.setComment("PRIVATE_DO_NOT_EXPOSE");em.persist(grade);em.flush();when(academic.studentsAt(eq(9L),any())).thenReturn(List.of(new AcademicDirectory.Student(11L,"One",111L,"VERIFIED"),new AcademicDirectory.Student(12L,"Two",112L,"VERIFIED")));when(remote.get(contains("/111/result-source"),eq(StudentProgressService.Source.class))).thenReturn(new StudentProgressService.Source(9L,"token",0));when(remote.get(contains("/112/result-source"),eq(StudentProgressService.Source.class))).thenReturn(new StudentProgressService.Source(9L,"token",0));when(remote.get(contains("/999/result-source"),eq(StudentProgressService.Source.class))).thenThrow(ApiException.missing());}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 GradePublicationService.Command command(){var state=publication.state(exam.getId());return new GradePublicationService.Command(UUID.randomUUID(),state.revision(),state.draftFingerprint(),"Reviewed");}
 void publish(){publication.apply(exam.getId(),command(),false);}
 @Test void draftNeverVisibleAndInternalCommentNeverCopied(){actor(Role.STUDENT,11L);assertEquals(0,student.grades(111L,0,20).totalElements());actor(Role.TEACHER,2L);publish();actor(Role.STUDENT,11L);var g=student.grades(111L,0,20).content().getFirst();assertEquals(0,new BigDecimal("8").compareTo(g.score()));assertNull(g.studentComment());}
 @Test void missingIsNotGradedAndZeroIsReal(){grade.setScore(BigDecimal.ZERO);grade.setStudentComment("Public feedback");em.flush();publish();actor(Role.STUDENT,11L);var g=student.grades(111L,0,20).content().getFirst();assertEquals("GRADED",g.status());assertEquals(0,g.score().signum());assertEquals("Public feedback",g.studentComment());actor(Role.STUDENT,12L);g=student.grades(112L,0,20).content().getFirst();assertEquals("NOT_GRADED",g.status());assertNull(g.score());}
 @Test void editingDraftDoesNotChangePublishedSnapshot(){publish();grade.setScore(new BigDecimal("9"));em.flush();assertTrue(publication.state(exam.getId()).draftChanged());actor(Role.STUDENT,11L);assertEquals(0,new BigDecimal("8").compareTo(student.grades(111L,0,20).content().getFirst().score()));actor(Role.TEACHER,2L);publish();actor(Role.STUDENT,11L);assertEquals(0,new BigDecimal("9").compareTo(student.grades(111L,0,20).content().getFirst().score()));}
 @Test void retractAndHistoricalRetryDoNotRepublish(){var original=command();publication.apply(exam.getId(),original,false);publication.apply(exam.getId(),command(),true);assertFalse(publication.apply(exam.getId(),original,false).published());actor(Role.STUDENT,11L);assertEquals(0,student.grades(111L,0,20).totalElements());}
 @Test void staleDraftAndStaleRevisionConflict(){var stale=command();grade.setStudentComment("Changed");em.flush();assertThrows(ApiException.class,()->publication.apply(exam.getId(),stale,false));var now=command();publication.apply(exam.getId(),now,false);assertThrows(ApiException.class,()->publication.apply(exam.getId(),new GradePublicationService.Command(UUID.randomUUID(),0,now.draftFingerprint(),"old"),false));}
 @Test void unresolvedAndUnfrozenRosterBlocked(){when(academic.studentsAt(eq(9L),any())).thenReturn(List.of());assertThrows(ApiException.class,()->publish());exam.setRosterFrozenAt(null);em.flush();assertThrows(ApiException.class,()->publish());}
 @Test void ownershipAndRolesCannotBeSpoofed(){publish();actor(Role.STUDENT,11L);assertEquals(0,student.grades(112L,0,20).totalElements());assertThrows(ApiException.class,()->student.grades(999L,0,20));assertThrows(ApiException.class,()->publication.state(exam.getId()));actor(Role.STAFF,2L);assertThrows(ApiException.class,()->publication.state(exam.getId()));actor(Role.ADMIN,2L);assertThrows(ApiException.class,()->student.grades(111L,0,20));}
 @Test void requestUuidCannotChangeIntent(){var c=command();publication.apply(exam.getId(),c,false);assertThrows(ApiException.class,()->publication.apply(exam.getId(),new GradePublicationService.Command(c.requestId(),c.expectedRevision(),c.draftFingerprint(),"Different"),false));}
 @Test void confirmedFinalMustMatchCurrentSourcesAndReopenHidesOutcome(){var b=new ResultBarrier();b.setClassId(9L);b.setBranchId(3L);b.setToken("token");em.persist(b);var t=new EvaluationState();t.setClassId(9L);t.setBranchId(3L);em.persist(t);var r=new FinalResult();r.setClassId(9L);r.setBranchId(3L);r.setEnrollmentId(111L);r.setStudentId(11L);r.setRevision(1);r.setRequestId(UUID.randomUUID().toString());r.setFingerprint("a".repeat(64));r.setOutcome("PASS");r.setState("CONFIRMED");r.setReason("INTERNAL");r.setSnapshot("PRIVATE");em.persist(r);em.flush();actor(Role.STUDENT,11L);assertEquals("PASS",student.outcome(111L).outcome());r.setState("REOPENED");assertEquals("UPDATING",student.outcome(111L).state());assertNull(student.outcome(111L).outcome());r.setState("CONFIRMED");t.setSourceRevision(1);assertEquals("UPDATING",student.outcome(111L).state());}
 @Test void dependencyFailureIsNotPending(){actor(Role.STUDENT,11L);when(remote.get(contains("/111/result-source"),eq(StudentProgressService.Source.class))).thenThrow(new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Unavailable"));assertThrows(ApiException.class,()->student.outcome(111L));}
}
