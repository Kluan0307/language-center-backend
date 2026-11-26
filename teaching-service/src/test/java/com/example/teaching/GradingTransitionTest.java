package com.example.teaching;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.example.teaching.grading.*;
import static com.example.teaching.grading.GradingModels.*;
import com.example.teaching.common.*;
import com.example.teaching.evaluation.*;
import com.example.teaching.exam.*;
import com.example.teaching.grade.*;
import com.example.teaching.integration.AcademicDirectory;
import com.example.teaching.results.*;
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
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:grading-transition;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class GradingTransitionTest {
 @Autowired GradingService grading;@Autowired GradingLegacyService legacy;@Autowired GradingOutcomeService outcomes;@Autowired GradingCompletionService completion;@Autowired GradingReadService read;
 @Autowired ExamService exams;@Autowired GradeService grades;@Autowired GradeRepository gradeRows;@Autowired EntityManager em;@Autowired ResultBarrierService barriers;@Autowired FinalResultRepository finals;
 @MockitoBean AcademicDirectory academic;@MockitoBean RemoteApi remote;
 LocalDate today=LocalDate.now(BusinessTime.ZONE);AcademicEvaluationContext context;
 void actor(Role r,long account,long profile){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(account,profile,3L,r,"actor"),null,List.of()));}
 @BeforeEach void setup(){actor(Role.ADMIN,20,2);when(academic.schoolClass(9L)).thenReturn(new AcademicDirectory.SchoolClass(9L,3L,2L,7L,"Class"));when(academic.studentsAt(eq(9L),any())).thenReturn(List.of(new AcademicDirectory.Student(11L,"Learner A",101L,"ENROLLMENT"),new AcademicDirectory.Student(12L,"Learner B",102L,"ENROLLMENT")));
  context=new AcademicEvaluationContext(9L,3L,LocalDateTime.now(),List.of(new AcademicEvaluationContext.Learner(101L,11L,"Learner A","STUDYING",List.of(new AcademicEvaluationContext.Period(today.minusMonths(1),null))),new AcademicEvaluationContext.Learner(102L,12L,"Learner B","STUDYING",List.of(new AcademicEvaluationContext.Period(today.minusMonths(1),null)))),List.of(new AcademicEvaluationContext.Session(1L,today,LocalTime.of(8,0),LocalTime.of(9,0),"COMPLETED")),List.of(new AcademicEvaluationContext.Mark(101L,1L,today,"PRESENT",false),new AcademicEvaluationContext.Mark(102L,1L,today,"PRESENT",false)),new AcademicEvaluationContext.Duration(60,1,60L,60L,0,0,true));
  when(academic.evaluationContext(eq(9L),anyBoolean())).thenAnswer(i->context);
  when(remote.get(contains("/classes/9/result-source"),eq(GradingOutcomeService.Source.class))).thenAnswer(i->{var b=barriers.read(9L);return new GradingOutcomeService.Source(null,b.getToken(),b.getSourceRevision());});
  when(remote.get(contains("/enrollments/101/result-source"),eq(GradingOutcomeService.Source.class))).thenAnswer(i->{var b=barriers.read(9L);return new GradingOutcomeService.Source(9L,b.getToken(),b.getSourceRevision());});
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 ExamDto legacyExam(){var e=exams.save(null,new ExamDto.Request(9L,"Legacy final","FINAL",today,BigDecimal.valueOf(40),BigDecimal.TEN));actor(Role.TEACHER,10,2);grades.save(new GradeDto.Request(e.id(),11L,BigDecimal.valueOf(7),"INTERNAL_NEVER_COPY",null,"Public feedback"));actor(Role.ADMIN,20,2);return e;}
 GradingLegacyService.Plan plan(ExamDto e){return new GradingLegacyService.Plan("INTERNAL-v1",List.of(new GradingLegacyService.Mapping(e.id(),"FINAL")));}
 void newInternal(){actor(Role.ADMIN,20,2);grading.assign(9L,new Assignment("INTERNAL-v1",0L,"Apply internal scheme"));actor(Role.TEACHER,10,2);for(var code:List.of("HOMEWORK","QUIZ","MIDTERM","FINAL")){var a=grading.create(9L,new ActivityCommand(code,today,List.of(code)));grading.save(9L,a.id(),new SaveMarks(List.of(new Mark(11L,code,null,Status.GRADED,BigDecimal.valueOf(7),null,"Public A"),new Mark(12L,code,null,Status.GRADED,BigDecimal.valueOf(8),null,"Public B")),"Record results"));}}
 void action(GradingOutcomeService.Action action){var p=outcomes.preview(9L);outcomes.action(9L,action,new GradingOutcomeService.Command(UUID.randomUUID(),p.workflow().revision(),p.fingerprint(),"Reviewed source and results"));}
 void publish(){action(GradingOutcomeService.Action.SUBMIT);actor(Role.ADMIN,20,2);action(GradingOutcomeService.Action.APPROVE);action(GradingOutcomeService.Action.PUBLISH);}
 @Test void legacyPreviewDoesNotWriteOrRescale(){var e=legacyExam();var preview=legacy.preview(9L,plan(e));assertTrue(preview.blockers().isEmpty());assertEquals(1,preview.grades());assertNull(em.find(GradingBinding.class,9L));assertEquals(1,gradeRows.count());assertNull(preview.proposed().students().getFirst().score());}
 @Test void importKeepsOriginalAndOnlyCopiesPublicComments(){var e=legacyExam();var p=legacy.preview(9L,plan(e));var r=legacy.apply(9L,new GradingLegacyService.Apply(UUID.randomUUID(),plan(e),p.fingerprint(),"Reviewed mapping, keep originals"));assertEquals(1,r.grades());assertEquals(1,gradeRows.count());var marks=grading.sheet(9L,grading.activities(9L).getFirst().id()).records();assertEquals("Public feedback",marks.getFirst().comment());assertFalse(marks.toString().contains("INTERNAL_NEVER_COPY"));assertTrue(outcomes.history(9L).isEmpty());}
 @Test void importIsIdempotent(){var e=legacyExam();var p=legacy.preview(9L,plan(e));var cmd=new GradingLegacyService.Apply(UUID.randomUUID(),plan(e),p.fingerprint(),"Reviewed mapping");var a=legacy.apply(9L,cmd);assertEquals(a.id(),legacy.apply(9L,cmd).id());assertEquals(1,grading.activities(9L).size());}
 @Test void staleLegacySourceCannotBeImported(){var e=legacyExam();var p=legacy.preview(9L,plan(e));var g=gradeRows.findByExamId(e.id()).getFirst();g.setScore(BigDecimal.valueOf(8));em.flush();assertThrows(ApiException.class,()->legacy.apply(9L,new GradingLegacyService.Apply(UUID.randomUUID(),plan(e),p.fingerprint(),"Reviewed mapping")));assertNull(em.find(GradingBinding.class,9L));}
 @Test void wholeIeltsScoreCannotBecomeFourSkills(){var e=legacyExam();var p=legacy.preview(9L,new GradingLegacyService.Plan("IELTS-v1",List.of(new GradingLegacyService.Mapping(e.id(),"LISTENING"))));assertFalse(p.blockers().isEmpty());}
 @Test void sourceOutsideDatedRosterBlocksCopy(){var e=legacyExam();when(academic.studentsAt(eq(9L),any())).thenReturn(List.of(new AcademicDirectory.Student(12L,"Other",102L,"ENROLLMENT")));assertFalse(legacy.preview(9L,plan(e)).blockers().isEmpty());}
 @Test void incompleteOrForeignMappingIsRejected(){legacyExam();var p=legacy.preview(9L,new GradingLegacyService.Plan("INTERNAL-v1",List.of(new GradingLegacyService.Mapping(999L,"FINAL"))));assertFalse(p.blockers().isEmpty());}
 @Test void teacherCannotImportLegacy(){var e=legacyExam();actor(Role.TEACHER,10,2);assertThrows(ApiException.class,()->legacy.preview(9L,plan(e)));}
 @Test void originalGradesLockedAfterImport(){var e=legacyExam();var p=legacy.preview(9L,plan(e));legacy.apply(9L,new GradingLegacyService.Apply(UUID.randomUUID(),plan(e),p.fingerprint(),"Reviewed mapping"));actor(Role.TEACHER,10,2);assertThrows(ApiException.class,()->grades.save(new GradeDto.Request(e.id(),11L,BigDecimal.valueOf(9),null,"Correction")));}
 @Test void onlyPublishedCompleteResultsCanBeConfirmed(){newInternal();actor(Role.ADMIN,20,2);assertFalse(completion.preview(9L).blockers().isEmpty());}
 @Test void publishedBatchCreatesProofsForAcademicWithoutChangingEnrollment(){newInternal();publish();var p=completion.preview(9L);assertTrue(p.blockers().isEmpty(),p.blockers().toString());var result=completion.confirm(9L,new GradingCompletionService.Command(UUID.randomUUID(),p.fingerprint(),"Confirm full class assessment"));assertEquals(2,result.results().size());assertTrue(result.results().stream().allMatch(r->r.state().equals("CONFIRMED")&&r.gradingReleaseId()!=null));assertEquals(2,barriers.read(9L).getConfirmedCount());assertEquals("STUDYING",context.enrollments().getFirst().studyStatus());}
 @Test void completionBatchReplayDoesNotDuplicate(){newInternal();publish();var p=completion.preview(9L);var command=new GradingCompletionService.Command(UUID.randomUUID(),p.fingerprint(),"Confirm completed assessment");completion.confirm(9L,command);completion.confirm(9L,command);assertEquals(2,finals.findByClassIdOrderByIdDesc(9L).size());}
 @Test void reopenInvalidatesOfficialProofsAndHidesPublication(){newInternal();publish();var p=completion.preview(9L);completion.confirm(9L,new GradingCompletionService.Command(UUID.randomUUID(),p.fingerprint(),"Confirm completed assessment"));action(GradingOutcomeService.Action.REOPEN);assertEquals(0,barriers.read(9L).getConfirmedCount());assertTrue(finals.findByClassIdOrderByIdDesc(9L).stream().allMatch(r->r.getState().equals("REOPENED")));actor(Role.STUDENT,40,11);assertNull(outcomes.student(101L).result());}
 @Test void completionStatusSynchronizationDoesNotChangeGradingFingerprint(){newInternal();publish();String before=outcomes.preview(9L).fingerprint();var learners=context.enrollments().stream().map(l->new AcademicEvaluationContext.Learner(l.enrollmentId(),l.studentId(),l.studentName(),"COMPLETED",List.of(new AcademicEvaluationContext.Period(today.minusMonths(1),today.plusDays(1))))).toList();context=new AcademicEvaluationContext(9L,3L,LocalDateTime.now(),learners,context.sessions(),context.attendance(),context.duration());assertEquals(before,outcomes.preview(9L).fingerprint());}
 @Test void exportIsBomUtf8AndProtectsFormulaStrings(){assertTrue(GradingExport.cell(" =HYPERLINK(\"bad\")").startsWith("\"'"));assertTrue(GradingExport.cell("\t+CMD").startsWith("\"'"));newInternal();publish();byte[] bytes=read.export(9L,true);assertEquals(0xef,bytes[0]&255);assertEquals(0xbb,bytes[1]&255);assertEquals(0xbf,bytes[2]&255);}
 @Test void personalCsvContainsNoOtherStudent(){newInternal();publish();actor(Role.STUDENT,40,11);String csv=new String(read.studentExport(101L),java.nio.charset.StandardCharsets.UTF_8);assertTrue(csv.contains("Learner A"));assertFalse(csv.contains("Learner B"));}
 @Test void staffReadsOnlyPublishedLearnerInsideVerifiedBranch(){
  newInternal();actor(Role.STAFF,30,3);
  var profile=new com.example.teaching.publication.ManagementProgressService.Profile(new com.example.teaching.publication.ManagementProgressService.Identity(11L),List.of(new com.example.teaching.publication.ManagementProgressService.Study(new com.example.teaching.publication.ManagementProgressService.Enrollment(101L,11L,9L,3L))));
  when(remote.get(contains("/students/11/learning-profile"),eq(com.example.teaching.publication.ManagementProgressService.Profile.class))).thenReturn(profile);
  assertNull(read.management(11L,101L).result());actor(Role.TEACHER,10,2);publish();actor(Role.STAFF,30,3);
  assertEquals(11L,read.management(11L,101L).result().studentId());assertThrows(ApiException.class,()->read.management(11L,102L));
  var foreign=new com.example.teaching.publication.ManagementProgressService.Profile(profile.student(),List.of(new com.example.teaching.publication.ManagementProgressService.Study(new com.example.teaching.publication.ManagementProgressService.Enrollment(101L,11L,9L,4L))));
  when(remote.get(contains("/students/11/learning-profile"),eq(com.example.teaching.publication.ManagementProgressService.Profile.class))).thenReturn(foreign);
  assertThrows(ApiException.class,()->read.management(11L,101L));
 }
 @Test void retainedRetakeHistoryShowsOriginalAndSelectedScores(){
  newInternal();var original=grading.activities(9L).stream().filter(a->a.components().contains("FINAL")).findFirst().orElseThrow();var retake=grading.create(9L,new ActivityCommand("Retake",today,List.of("FINAL"),original.id(),null,List.of(11L)));
  grading.save(9L,retake.id(),new SaveMarks(List.of(new Mark(11L,"FINAL",null,Status.GRADED,BigDecimal.valueOf(9),null,"Improved")),"Retake recorded"));
  var item=outcomes.preview(9L).report().students().getFirst().components().stream().filter(c->c.code().equals("FINAL")).findFirst().orElseThrow().assessments().getFirst();
  assertEquals(2,item.history().size());assertEquals(0,BigDecimal.valueOf(9).compareTo(item.history().stream().filter(GradingCalculation.Attempt::selected).findFirst().orElseThrow().score()));
  assertEquals(0,BigDecimal.valueOf(7).compareTo(item.history().stream().filter(h->!h.selected()).findFirst().orElseThrow().score()));
 }
 @Test void activityCanBeCorrectedOnlyBeforeRosterFreeze(){actor(Role.ADMIN,20,2);grading.assign(9L,new Assignment("INTERNAL-v1",0L,"Apply scheme"));var a=grading.create(9L,new ActivityCommand("Wrong date",today.plusDays(3),List.of("FINAL")));var edit=grading.edit(9L,a.id(),new ActivityEdit(a.version(),"Correct date",today,"Correct schedule"));assertEquals(today,edit.date());grading.save(9L,a.id(),new SaveMarks(List.of(new Mark(11L,"FINAL",null,Status.GRADED,BigDecimal.ONE,null,null)),"Record grade"));assertThrows(ApiException.class,()->grading.edit(9L,a.id(),new ActivityEdit(edit.version(),"Changed",today,"Invalid change")));}
}
