package com.example.teaching;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.example.teaching.grading.*;
import static com.example.teaching.grading.GradingModels.*;
import com.example.teaching.common.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.security.*;
import com.example.teaching.exam.*;
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

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:grading-workflow;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class GradingWorkflowTest {
 @Autowired GradingService service;
 @Autowired EntityManager em;
 @Autowired ExamService legacyExams;
 @MockitoBean AcademicDirectory academic;
 private final LocalDate today=LocalDate.now(BusinessTime.ZONE);
 private void actor(Role role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(1L,2L,3L,role,"operator"),null,List.of()));}
 @BeforeEach void setup(){actor(Role.SUPER_ADMIN);when(academic.schoolClass(9L)).thenReturn(new AcademicDirectory.SchoolClass(9L,3L,2L,7L,"Class"));when(academic.course(7L,3L)).thenReturn(new AcademicDirectory.Course(7L,null));when(academic.studentsAt(eq(9L),any())).thenReturn(List.of(new AcademicDirectory.Student(11L,"Learner",101L,"ENROLLMENT"),new AcademicDirectory.Student(12L,"Other",102L,"ENROLLMENT")));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 private BindingView bind(Kind k){return service.assign(9L,new Assignment(k+"-v1",0L,"Apply class scheme"));}
 private ActivityView activity(Kind k){bind(k);return service.create(9L,new ActivityCommand("Assessment",today,List.of(k==Kind.INTERNAL?"FINAL":"LISTENING")));}
 private Mark mark(Long student,String component,Long version,Status status,String score,Level level,String comment){return new Mark(student,component,version,status,score==null?null:new BigDecimal(score),level,comment);}
 private Sheet save(ActivityView a,Mark... marks){return service.save(9L,a.id(),new SaveMarks(List.of(marks),"Save draft assessment results"));}
 @Test void initializesFourTemplatesWithoutAssigningAnyClass(){assertEquals(4,service.templates().size());assertNull(service.binding(9L));}
 @Test void revisingTemplateDoesNotChangeClassSnapshot(){var b=bind(Kind.INTERNAL);var v=service.revise(new TemplateCommand(Kind.INTERNAL,1L,"New internal",GradingRules.standard(Kind.INTERNAL),"Update template version"));assertEquals(2,v.revision());assertEquals("INTERNAL-v1",service.binding(9L).templateId());assertEquals(b.spec(),service.binding(9L).spec());}
 @Test void rejectsStaleTemplateRevision(){service.revise(new TemplateCommand(Kind.INTERNAL,1L,"Next",GradingRules.standard(Kind.INTERNAL),"Update template"));assertThrows(ApiException.class,()->service.revise(new TemplateCommand(Kind.INTERNAL,1L,"Stale",GradingRules.standard(Kind.INTERNAL),"Update template")));}
 @Test void validatesWeightTotal(){var s=GradingRules.standard(Kind.INTERNAL);var components=new ArrayList<>(s.components());var first=components.getFirst();components.set(0,new Component(first.code(),first.name(),true,first.minScore(),first.maxScore(),first.step(),BigDecimal.ONE));assertThrows(ApiException.class,()->GradingRules.validate(new Spec(Kind.INTERNAL,components,s.passPercent(),null)));}
 @Test void activityFreezesClassScheme(){activity(Kind.INTERNAL);assertTrue(service.binding(9L).locked());assertThrows(ApiException.class,()->service.assign(9L,new Assignment("IELTS-v1",1L,"Replace scheme")));}
 @Test void courseDefaultIsScopedAndDoesNotAssignExistingClass(){actor(Role.ADMIN);var d=service.courseDefault(new CourseAssignment(7L,3L,"INTERNAL-v1",0L,"Set course default"));assertEquals(1,d.getRevision());assertNull(service.binding(9L));assertThrows(ApiException.class,()->service.courseDefault(new CourseAssignment(7L,4L,"INTERNAL-v1",0L,"Foreign branch")));}
 @Test void wrongRoleCannotManageTemplatesOrAssignments(){actor(Role.TEACHER);assertThrows(ApiException.class,()->bind(Kind.INTERNAL));assertThrows(ApiException.class,()->service.revise(new TemplateCommand(Kind.INTERNAL,1L,"Next",GradingRules.standard(Kind.INTERNAL),"Update template")));actor(Role.STAFF);assertThrows(ApiException.class,()->service.templates());}
 @Test void branchMismatchCannotAccessClass(){actor(Role.ADMIN);when(academic.schoolClass(9L)).thenReturn(new AcademicDirectory.SchoolClass(9L,4L,2L,7L,"Foreign"));assertThrows(ApiException.class,()->service.binding(9L));}
 @Test void unassignedTeacherCannotReadOrWrite(){actor(Role.TEACHER);when(academic.schoolClass(9L)).thenReturn(new AcademicDirectory.SchoolClass(9L,3L,99L,7L,"Foreign teacher"));assertThrows(ApiException.class,()->service.activities(9L));}
 @Test void numericZeroIsRecordedAndMissingLearnerRemainsUnrecorded(){var a=activity(Kind.INTERNAL);actor(Role.TEACHER);var sheet=save(a,mark(11L,"FINAL",null,Status.GRADED,"0",null,null));assertEquals(1,sheet.records().size());assertEquals(0,sheet.records().getFirst().score().signum());assertEquals(2,sheet.students().size());assertTrue(sheet.activity().rosterFrozen());}
 @Test void absenceIsNotZeroAndRequiresExplanation(){var a=activity(Kind.INTERNAL);assertThrows(ApiException.class,()->save(a,mark(11L,"FINAL",null,Status.ABSENT,"0",null,"Absent")));var sheet=save(a,mark(11L,"FINAL",null,Status.ABSENT,null,null,"Illness"));assertNull(sheet.records().getFirst().score());assertEquals(Status.ABSENT,sheet.records().getFirst().status());}
 @Test void observationUsesLevelAndCommentInsteadOfScore(){var a=activity(Kind.OBSERVATION);var s=save(a,mark(11L,"LISTENING",null,Status.GRADED,null,Level.DEVELOPING,"Recognizes familiar words"));assertEquals(Level.DEVELOPING,s.records().getFirst().level());assertNull(s.records().getFirst().score());}
 @Test void invalidNumericRangeIsRejected(){var a=activity(Kind.INTERNAL);assertThrows(ApiException.class,()->save(a,mark(11L,"FINAL",null,Status.GRADED,"11",null,null)));}
 @Test void ieltsRejectsInvalidBandStep(){var a=activity(Kind.IELTS);assertThrows(ApiException.class,()->save(a,mark(11L,"LISTENING",null,Status.GRADED,"6.25",null,null)));}
 @Test void toeicRejectsRawCountAsScaledScore(){var a=activity(Kind.TOEIC_LR);assertThrows(ApiException.class,()->save(a,mark(11L,"LISTENING",null,Status.GRADED,"63",null,null)));}
 @Test void optimisticVersionPreventsLostUpdate(){var a=activity(Kind.INTERNAL);var first=save(a,mark(11L,"FINAL",null,Status.GRADED,"7",null,null));long v=first.records().getFirst().version();save(a,mark(11L,"FINAL",v,Status.GRADED,"8",null,"Correction"));assertThrows(ApiException.class,()->save(a,mark(11L,"FINAL",v,Status.GRADED,"9",null,"Stale")));}
 @Test void firstSaveFreezesExamDayRoster(){var a=activity(Kind.INTERNAL);save(a,mark(11L,"FINAL",null,Status.GRADED,"7",null,null));when(academic.studentsAt(eq(9L),any())).thenReturn(List.of());assertEquals(2,save(a,mark(12L,"FINAL",null,Status.GRADED,"8",null,null)).students().size());}
 @Test void futureAssessmentIsReadOnly(){bind(Kind.INTERNAL);var a=service.create(9L,new ActivityCommand("Future",today.plusDays(1),List.of("FINAL")));assertThrows(ApiException.class,()->save(a,mark(11L,"FINAL",null,Status.GRADED,"7",null,null)));}
 @Test void wrongLearnerAndWrongComponentAreRejected(){var a=activity(Kind.INTERNAL);assertThrows(ApiException.class,()->save(a,mark(99L,"FINAL",null,Status.GRADED,"7",null,null)));assertThrows(ApiException.class,()->save(a,mark(11L,"QUIZ",null,Status.GRADED,"7",null,null)));}
 @Test void noLegacyExamCanBeCreatedForConfiguredClass(){bind(Kind.INTERNAL);assertThrows(ApiException.class,()->legacyExams.save(null,new ExamDto.Request(9L,"Legacy","FINAL",today,BigDecimal.ONE,BigDecimal.TEN)));}
 @Test void legacyExamPreventsSilentConversion(){legacyExams.save(null,new ExamDto.Request(9L,"Legacy","FINAL",today,BigDecimal.ONE,BigDecimal.TEN));assertThrows(ApiException.class,()->bind(Kind.INTERNAL));}
 @Test void closedClassCannotChange(){when(academic.schoolClass(9L)).thenReturn(new AcademicDirectory.SchoolClass(9L,3L,2L,7L,"Closed",true));assertThrows(ApiException.class,()->bind(Kind.INTERNAL));}
 @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 void invalidBatchRollsBackEveryMarkAndRosterFreeze(){
  when(academic.schoolClass(99L)).thenReturn(new AcademicDirectory.SchoolClass(99L,3L,2L,7L,"Atomic class"));
  when(academic.studentsAt(eq(99L),any())).thenReturn(List.of(new AcademicDirectory.Student(11L,"Learner",101L,"ENROLLMENT"),new AcademicDirectory.Student(12L,"Other",102L,"ENROLLMENT")));
  service.assign(99L,new Assignment("INTERNAL-v1",0L,"Apply atomic scheme"));var a=service.create(99L,new ActivityCommand("Batch",today,List.of("FINAL")));
  assertThrows(ApiException.class,()->service.save(99L,a.id(),new SaveMarks(List.of(mark(11L,"FINAL",null,Status.GRADED,"7",null,null),mark(12L,"FINAL",null,Status.GRADED,"11",null,null)),"Atomic batch check")));
  var sheet=service.sheet(99L,a.id());assertTrue(sheet.records().isEmpty());assertFalse(sheet.activity().rosterFrozen());
 }
}
