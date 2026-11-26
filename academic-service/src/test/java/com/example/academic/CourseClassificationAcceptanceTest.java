package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import com.example.academic.admission.*;
import com.example.academic.assistant.*;
import com.example.academic.branch.Branch;
import com.example.academic.common.*;
import com.example.academic.course.*;
import com.example.academic.enrollment.Enrollment;
import com.example.academic.room.Room;
import com.example.academic.schoolclass.SchoolClass;
import com.example.academic.security.*;
import com.example.academic.student.*;
import com.example.academic.website.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** One isolated data set flows through management, public, student and assistant projections. */
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:classification-acceptance;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class CourseClassificationAcceptanceTest {
  @Autowired EntityManager em;
  @Autowired CourseController management;
  @Autowired CourseClassificationService classification;
  @Autowired WebsiteCatalogService website;
  @Autowired StudentLearningService learning;
  @Autowired AcademicReadService assistant;
  @Autowired CareInsights statistics;
  Branch branch;
  Program program;
  AudienceGroup secondary, adult;

  void actor(Role role, Long account, Long profile) {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        new CurrentUser(account, profile, branch == null ? null : branch.getId(), role, "acceptance",
            branch == null ? List.of() : List.of(branch.getId())), null,
        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }
  @BeforeEach void setup() {
    actor(Role.SUPER_ADMIN, 700L, 700L);
    branch = new Branch(); branch.setCode("G8CN"); branch.setName("Chi nhánh nghiệm thu");
    branch.setPhone("0900000000"); branch.setAddress("Địa chỉ kiểm thử"); em.persist(branch);
    program = new Program(); program.setCode("IELTS"); program.setName("IELTS ban đầu"); em.persist(program);
    secondary = audience("SECONDARY"); adult = audience("ADULT");
  }
  AudienceGroup audience(String code) {
    var a = new AudienceGroup(); a.setCode(code); a.setName(code); em.persist(a); return a;
  }
  @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
  CourseDto.Request request(long revision) {
    return new CourseDto.Request("G8KH", "IELTS Foundation", null, null, null,
        new BigDecimal("4000000"), null, null, 20, branch.getId(), null,
        program.getId(), List.of(secondary.getId(), adult.getId()), revision);
  }
  void publish(WebsitePublication.Kind kind, Long id, String slug, Long parent) {
    website.edit(kind, new WebsiteDto.Edit(id, 0L, slug, WebsitePublication.State.PUBLISHED,
        "Thông tin nghiệm thu", null, null, null, null, null, null, parent, false));
  }

  @Test void canonicalCourseFlowsAcrossAllReadersAndClassSnapshotSurvivesProgramRename() {
    var saved = management.create(request(0));
    var course = em.find(Course.class, saved.id());
    var policy = new CourseAdmissionPolicy();
    policy.setCourseId(course.getId()); policy.setRevision(1); policy.setProgramId(program.getId());
    policy.setProgramCode("IELTS"); policy.setProgramName("IELTS lúc mở lớp");
    policy.setMode(CourseAdmissionPolicy.Mode.OPEN_ENTRY); policy.setOutputLevelId(1L);
    policy.setOutputLevelCode("F"); policy.setOutputLevelName("Foundation");
    policy.setConfirmedBy(700L); policy.setConfirmedByName("Quản lý");
    policy.setConfirmedAt(LocalDateTime.now(BusinessTime.ZONE)); policy.setReason("Fixture được xác nhận"); em.persist(policy);
    var room = new Room(); room.setCode("G8P"); room.setName("Phòng 1"); room.setCapacity(20);
    room.setBranchId(branch.getId()); em.persist(room);
    var school = new SchoolClass(); school.setCode("G8L"); school.setName("Lớp IELTS");
    school.setCourse(course); school.setBranchId(branch.getId()); school.setRoom(room);
    school.setTeacherId(800L); school.setTeacherName("Giáo viên"); school.setMaxSize(20);
    school.setAdmissionPolicyId(policy.getId());
    school.setStartDate(LocalDate.now(BusinessTime.ZONE).plusDays(7));
    school.setEndDate(school.getStartDate().plusMonths(2)); em.persist(school);
    var student = new Student(); student.setFullName("Học viên nghiệm thu"); student.setPhone("0900000001");
    student.setBranchId(branch.getId()); student.setAccountId(900L); em.persist(student);
    var enrollment = new Enrollment(); enrollment.setStudent(student); enrollment.setSchoolClass(school); em.persist(enrollment);
    publish(WebsitePublication.Kind.PROGRAM, program.getId(), "g8-ielts", null);
    publish(WebsitePublication.Kind.BRANCH, branch.getId(), "g8-branch", null);
    publish(WebsitePublication.Kind.COURSE, course.getId(), "g8-course", program.getId());
    publish(WebsitePublication.Kind.CLASS, school.getId(), "g8-class", null);
    program.setName("IELTS tên hiện tại"); em.flush();

    assertEquals(1, website.list(WebsitePublication.Kind.COURSE, 0, 20, "g8-ielts", null, null, null, null, "SECONDARY").items().size());
    assertEquals(1, website.list(WebsitePublication.Kind.COURSE, 0, 20, "g8-ielts", null, null, null, null, "ADULT").items().size());
    assertEquals("IELTS tên hiện tại", website.detail(WebsitePublication.Kind.COURSE, "g8-course").classification().programName());
    assertEquals("IELTS lúc mở lớp", website.detail(WebsitePublication.Kind.CLASS, "g8-class").classification().programName());
    var read = assistant.courses(ReadQuery.Mode.ACTIVE_BRANCH, branch.getId(), null, null, null, null,
        null, new BigDecimal("5000000"), false, 0, 5, "IELTS", "SECONDARY");
    assertEquals(1, ((PageResponse<?>)read.data()).totalElements());
    var stats = new CareInsights.Request(); stats.setBranchId(branch.getId());
    stats.setMetric("COURSES_COUNT"); stats.setProgramCode("IELTS");
    assertEquals(1L, ((Map<?, ?>)statistics.center(stats).data()).get("value"));
    assertEquals(2, management.get(course.getId()).audiences().size());

    actor(Role.STUDENT, 900L, student.getId());
    var detail = learning.detail(enrollment.getId());
    assertEquals("IELTS lúc mở lớp", detail.enrollment().classification().programName());
    assertEquals(2, detail.enrollment().classification().audiences().size());
    actor(Role.STUDENT, 901L, student.getId());
    assertThrows(ApiException.class, () -> learning.detail(enrollment.getId()));
  }

  @Test void legacyTypeIsPreservedButNeverBecomesCanonicalClassification() {
    var legacy = new Course(); legacy.setCode("G8OLD"); legacy.setName("Khóa cũ");
    legacy.setType("IELTS THCS"); legacy.setTuitionFee(new BigDecimal("1000000"));
    legacy.setMaxStudents(20); legacy.setBranchId(branch.getId()); em.persist(legacy); em.flush();
    assertEquals("IELTS THCS", management.get(legacy.getId()).type());
    assertFalse(management.get(legacy.getId()).classificationComplete());
    var read = assistant.courses(ReadQuery.Mode.ACTIVE_BRANCH, branch.getId(), null, null, null,
        null, null, null, true, 0, 5, "IELTS", "SECONDARY");
    assertEquals(0, ((PageResponse<?>)read.data()).totalElements());
  }

  @Test void managementCatalogDoesNotOfferUnsupportedAudienceCodes() {
    audience("CUSTOM"); em.flush();
    assertTrue(classification.catalog().audiences().stream().noneMatch(a -> a.code().equals("CUSTOM")));
  }
}
