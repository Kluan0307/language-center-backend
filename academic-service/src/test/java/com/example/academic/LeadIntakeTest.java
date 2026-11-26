package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.academic.lead.*;
import com.example.academic.student.*;
import com.example.academic.branch.*;
import com.example.academic.course.*;
import com.example.academic.room.*;
import com.example.academic.schoolclass.*;
import com.example.academic.enrollment.*;
import com.example.academic.security.*;
import com.example.academic.common.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:lead-intake;MODE=MySQL;DB_CLOSE_DELAY=-1")
class LeadIntakeTest {
 @Autowired LeadIntakeService intake;@Autowired LeadService leads;@Autowired StudentService students;@Autowired StudentRepository studentRows;@Autowired EnrollmentRepository enrollments;
 @Autowired EntityManager em;@Autowired PlatformTransactionManager transactions;@Autowired PlacementTestFixtures fixture;@MockitoBean RemoteApi remote;
 @Autowired tools.jackson.databind.json.JsonMapper json;
 Long branch,classId;LeadDto lead;
 void actor(Role role,long account){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(account,1L,branch,role,"actor"),null,List.of()));}
 @BeforeEach void setup(){
  new TransactionTemplate(transactions).executeWithoutResult(t->{var b=new Branch();b.setCode("B"+UUID.randomUUID());b.setName("Test branch");b.setPhone("0901234567");em.persist(b);em.flush();branch=b.getId();var course=new Course();course.setCode("K"+UUID.randomUUID());course.setName("Test course");course.setBranchId(branch);course.setDuration(10);course.setMaxStudents(1);course.setTuitionFee(new BigDecimal("100"));em.persist(course);var room=new Room();room.setBranchId(branch);room.setCode("R"+UUID.randomUUID());room.setName("Room");room.setCapacity(10);em.persist(room);var c=new SchoolClass();c.setBranchId(branch);c.setCode("L"+UUID.randomUUID());c.setName("Class");c.setCourse(course);c.setRoom(room);c.setTeacherId(1L);c.setTeacherName("Teacher");c.setStartDate(LocalDate.now().plusDays(7));c.setEndDate(LocalDate.now().plusMonths(1));c.setMaxSize(1);em.persist(c);em.flush();classId=c.getId();});
  actor(Role.SUPER_ADMIN,1);fixture.open(classId);when(remote.token()).thenReturn("Bearer synthetic");
  lead=leads.save(null,new LeadDto.Request("Learner","0901234567",null,"TEST",null,"B2",null,branch));lead=leads.status(lead.id(),new LeadDto.StatusRequest(Lead.Status.CONTACTED,"Called customer",lead.version()));
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 LeadIntakeService.Selection selection(LeadIntakeService.Mode mode){return new LeadIntakeService.Selection(lead.version(),mode,null,null,null,mode==LeadIntakeService.Mode.ENROLL?classId:null,null,false);}
 LeadIntakeService.Confirm command(LeadIntakeService.Selection r){var p=intake.preview(lead.id(),r);assertTrue(p.blockers().isEmpty(),p.blockers().toString());return new LeadIntakeService.Confirm(UUID.randomUUID(),r,p.fingerprint(),"Reviewed intake");}
 @Test void previewDoesNotCreateStudentOrEnrollment(){long s=studentRows.count(),e=enrollments.count();var p=intake.preview(lead.id(),selection(LeadIntakeService.Mode.ENROLL));assertTrue(p.blockers().isEmpty(),p.blockers().toString());assertEquals(s,studentRows.count());assertEquals(e,enrollments.count());assertNull(p.profile().currentLevel());assertEquals(1,p.target().maxSize());}
 @Test void oneConfirmCreatesStudentAndEnrollmentAndReplayIsStable(){var cmd=command(selection(LeadIntakeService.Mode.ENROLL));var result=intake.confirm(lead.id(),cmd);assertNotNull(result.enrollmentId());assertTrue(result.createdStudent());assertEquals(result,intake.confirm(lead.id(),cmd));assertEquals(Lead.Status.CONVERTED,leads.get(lead.id()).status());assertNull(students.get(result.studentId()).student().currentLevel());}
 @Test void profileOnlyDoesNotCreateEnrollment(){long before=enrollments.count();var result=intake.confirm(lead.id(),command(selection(LeadIntakeService.Mode.PROFILE_ONLY)));assertNull(result.enrollmentId());assertEquals(before,enrollments.count());}
 @Test void fullClassAfterPreviewDoesNotLeaveNewProfile(){var cmd=command(selection(LeadIntakeService.Mode.ENROLL));new TransactionTemplate(transactions).executeWithoutResult(t->em.find(SchoolClass.class,classId).setMaxSize(0));long before=studentRows.count();assertThrows(ApiException.class,()->intake.confirm(lead.id(),cmd));assertEquals(before,studentRows.count());assertEquals(Lead.Status.CONTACTED,leads.get(lead.id()).status());}
 @Test void staleLeadCannotConfirm(){var cmd=command(selection(LeadIntakeService.Mode.PROFILE_ONLY));leads.status(lead.id(),new LeadDto.StatusRequest(Lead.Status.CONSULTING,"More consultation",lead.version()));assertThrows(ApiException.class,()->intake.confirm(lead.id(),cmd));}
 @Test void sharedPhoneNeedsExplicitReview(){students.convert("Sibling","0901234567",null,null,branch);var p=intake.preview(lead.id(),selection(LeadIntakeService.Mode.PROFILE_ONLY));assertFalse(p.similarProfiles().isEmpty());assertFalse(p.blockers().isEmpty());var r=new LeadIntakeService.Selection(lead.version(),LeadIntakeService.Mode.PROFILE_ONLY,null,null,null,null,null,true);assertTrue(intake.preview(lead.id(),r).blockers().isEmpty());}
 @Test void existingStudentIsReusedWithoutOverwritingProfile(){var student=students.convert("Existing learner","0901234567",null,"Legacy",branch);var r=new LeadIntakeService.Selection(lead.version(),LeadIntakeService.Mode.ENROLL,student.getId(),student.getVersion(),null,classId,null,false);long count=studentRows.count();var result=intake.confirm(lead.id(),command(r));assertFalse(result.createdStudent());assertEquals(student.getId(),result.studentId());assertEquals(count,studentRows.count());assertEquals("Existing learner",students.get(student.getId()).student().fullName());}
 @Test void unassignedStaffAndTeacherCannotInspect(){actor(Role.STAFF,2);assertThrows(ApiException.class,()->intake.preview(lead.id(),selection(LeadIntakeService.Mode.PROFILE_ONLY)));actor(Role.TEACHER,1);assertThrows(ApiException.class,()->intake.preview(lead.id(),selection(LeadIntakeService.Mode.PROFILE_ONLY)));}
 @Test void preferenceIsLinkedAndCanBeCleared(){var p=intake.preference(lead.id(),new LeadIntakeService.Preference(lead.version(),null,classId,"Evening","Customer selected class"));assertEquals(classId,p.preferredClassId());assertNotNull(p.interestedCourseId());var cleared=intake.preference(lead.id(),new LeadIntakeService.Preference(p.version(),null,null,null,"Customer changed preference"));assertNull(cleared.preferredClassId());}
 @Test void differentIntentCannotReuseReceipt(){var cmd=command(selection(LeadIntakeService.Mode.PROFILE_ONLY));intake.confirm(lead.id(),cmd);assertThrows(ApiException.class,()->intake.confirm(lead.id(),new LeadIntakeService.Confirm(cmd.requestId(),cmd.selection(),cmd.fingerprint(),"Different reason")));}
 @Test void failureAfterCreatingProfileRollsBackWholeIntake(){
  var cmd=command(selection(LeadIntakeService.Mode.ENROLL));
  new TransactionTemplate(transactions).executeWithoutResult(t->{var b=new com.example.academic.results.ResultBarrier();b.setClassId(classId);b.setBranchId(branch);b.setToken(UUID.randomUUID().toString());b.setConfirmedCount(1);em.persist(b);});
  long before=studentRows.count(),registered=enrollments.count();assertThrows(ApiException.class,()->intake.confirm(lead.id(),cmd));assertEquals(before,studentRows.count());assertEquals(registered,enrollments.count());assertEquals(Lead.Status.CONTACTED,leads.get(lead.id()).status());
 }
 @Test void twoLeadsCanReferToSameExistingLearner(){
  var student=students.convert("Returning learner","0901234567",null,null,branch);
  var r=new LeadIntakeService.Selection(lead.version(),LeadIntakeService.Mode.PROFILE_ONLY,student.getId(),student.getVersion(),null,null,null,false);intake.confirm(lead.id(),command(r));
  lead=leads.save(null,new LeadDto.Request("Other consultation","0901234567",null,"TEST",null,null,null,branch));lead=leads.status(lead.id(),new LeadDto.StatusRequest(Lead.Status.CONTACTED,"New consultation",lead.version()));
  var second=new LeadIntakeService.Selection(lead.version(),LeadIntakeService.Mode.PROFILE_ONLY,student.getId(),student.getVersion(),null,null,null,false);assertEquals(student.getId(),intake.confirm(lead.id(),command(second)).studentId());
 }
 @Test void optionalAcknowledgmentCanBeOmittedInJson(){var request=json.readValue("{\"leadVersion\":"+lead.version()+",\"mode\":\"PROFILE_ONLY\"}",LeadIntakeService.Selection.class);assertNull(request.acknowledgeSimilarProfiles());assertTrue(intake.preview(lead.id(),request).blockers().isEmpty());}
 @Test void recoveryReturnsOnlyMatchingLeadReceipt(){var cmd=command(selection(LeadIntakeService.Mode.PROFILE_ONLY));assertThrows(ApiException.class,()->intake.recover(lead.id(),cmd.requestId()));var result=intake.confirm(lead.id(),cmd);assertEquals(result,intake.recover(lead.id(),cmd.requestId()));assertThrows(ApiException.class,()->intake.recover(lead.id(),UUID.randomUUID()));actor(Role.STAFF,2);assertThrows(ApiException.class,()->intake.recover(lead.id(),cmd.requestId()));}
 @Test void twoConcurrentRequestsForOneLeadReturnOneResult() throws Exception {
  var cmd=command(selection(LeadIntakeService.Mode.ENROLL));Long id=lead.id();long before=studentRows.count();var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var start=new java.util.concurrent.CyclicBarrier(2);
  try{java.util.concurrent.Callable<LeadIntakeService.Result> job=()->{actor(Role.SUPER_ADMIN,1);try{start.await(10,java.util.concurrent.TimeUnit.SECONDS);return intake.confirm(id,cmd);}finally{clear();}};var a=pool.submit(job);var b=pool.submit(job);assertEquals(a.get(30,java.util.concurrent.TimeUnit.SECONDS),b.get(30,java.util.concurrent.TimeUnit.SECONDS));assertEquals(before+1,studentRows.count());assertEquals(1,enrollments.countBySchoolClassIdAndStudyStatus(classId,Enrollment.StudyStatus.STUDYING));}finally{pool.shutdownNow();}
 }
 @Test void concurrentLeadsCannotTakeTheSameLastSeat() throws Exception {
  Long first=lead.id();var a=command(selection(LeadIntakeService.Mode.ENROLL));
  var other=leads.save(null,new LeadDto.Request("Different learner","0901234568",null,"TEST",null,null,null,branch));other=leads.status(other.id(),new LeadDto.StatusRequest(Lead.Status.CONTACTED,"Called customer",other.version()));Long second=other.id();var selection=new LeadIntakeService.Selection(other.version(),LeadIntakeService.Mode.ENROLL,null,null,null,classId,null,false);var p=intake.preview(second,selection);assertTrue(p.blockers().isEmpty());var b=new LeadIntakeService.Confirm(UUID.randomUUID(),selection,p.fingerprint(),"Reviewed other learner");long before=studentRows.count();var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var start=new java.util.concurrent.CyclicBarrier(2);
  try{java.util.function.BiFunction<Long,LeadIntakeService.Confirm,java.util.concurrent.Callable<Boolean>> job=(id,cmd)->()->{actor(Role.SUPER_ADMIN,1);try{start.await(10,java.util.concurrent.TimeUnit.SECONDS);try{intake.confirm(id,cmd);return true;}catch(ApiException e){assertEquals(409,e.getStatus().value());return false;}}finally{clear();}};var f=pool.submit(job.apply(first,a));var g=pool.submit(job.apply(second,b));assertNotEquals(f.get(30,java.util.concurrent.TimeUnit.SECONDS),g.get(30,java.util.concurrent.TimeUnit.SECONDS));assertEquals(before+1,studentRows.count());assertEquals(1,enrollments.countBySchoolClassIdAndStudyStatus(classId,Enrollment.StudyStatus.STUDYING));}finally{pool.shutdownNow();}
 }
}
