package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import com.example.academic.website.*;
import com.example.academic.admission.*;
import com.example.academic.branch.Branch;
import com.example.academic.course.Course;
import com.example.academic.room.Room;
import com.example.academic.schedule.Schedule;
import com.example.academic.schoolclass.SchoolClass;
import com.example.academic.common.*;
import com.example.academic.security.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static com.example.academic.website.WebsitePublication.*;

class WebsiteCatalogTest {
  static SessionFactory factory;
  EntityManager em;WebsiteCatalogService service;WebsitePublicationRepository repo;
  Program program;Branch branch;Course course;SchoolClass schoolClass;
  @BeforeAll static void boot(){
    var c=new Configuration();
    for(var type:List.of(com.example.academic.course.AudienceGroup.class,WebsiteEditEvent.class,WebsitePublication.class,Program.class,Branch.class,Course.class,Room.class,SchoolClass.class,Schedule.class,CourseAdmissionPolicy.class))c.addAnnotatedClass(type);
    c.setProperty("hibernate.connection.driver_class","org.h2.Driver");
    c.setProperty("hibernate.connection.url","jdbc:h2:mem:website;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE");
    c.setProperty("hibernate.hbm2ddl.auto","create-drop");
    c.setPhysicalNamingStrategy(new org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl());
    c.setProperty("hibernate.hbm2ddl.halt_on_error","true");
    c.setProperty("hibernate.show_sql","false");factory=c.buildSessionFactory();
  }
  @AfterAll static void close(){if(factory!=null)factory.close();}
  @BeforeEach void setup(){
    em=factory.createEntityManager();em.getTransaction().begin();
    repo=new JpaRepositoryFactory(em).getRepository(WebsitePublicationRepository.class);
    service=new WebsiteCatalogService(em,repo);
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(1L,1L,null,Role.SUPER_ADMIN,"test"),null,List.of()));
    program=new Program();program.setCode("GE");program.setName("Tiếng Anh");em.persist(program);
    branch=new Branch();branch.setCode("CN1");branch.setName("Chi nhánh 1");branch.setPhone("0901234567");branch.setAddress("Địa chỉ đã xác nhận");em.persist(branch);
    course=new Course();course.setCode("KH1");course.setName("Giao tiếp");course.setBranchId(branch.getId());course.setTuitionFee(new BigDecimal("3000000"));course.setMaxStudents(20);course.setExpectedMinutes(1200);em.persist(course);
    Room room=new Room();room.setCode("P1");room.setName("Phòng 1");room.setCapacity(20);room.setBranchId(branch.getId());em.persist(room);
    schoolClass=new SchoolClass();schoolClass.setCode("L1");schoolClass.setName("Lớp giao tiếp");schoolClass.setCourse(course);schoolClass.setBranchId(branch.getId());schoolClass.setRoom(room);schoolClass.setTeacherId(99L);schoolClass.setTeacherName("PRIVATE TEACHER");schoolClass.setMaxSize(20);schoolClass.setStartDate(LocalDate.now(BusinessTime.ZONE).plusDays(7));schoolClass.setEndDate(schoolClass.getStartDate().plusMonths(2));em.persist(schoolClass);em.flush();
  }
  @AfterEach void cleanup(){if(em.getTransaction().isActive())em.getTransaction().rollback();em.close();SecurityContextHolder.clearContext();}
  WebsitePublication publish(Kind kind,Long id,String slug,Long parent,boolean fee){return service.edit(kind,new WebsiteDto.Edit(id,0L,slug,State.PUBLISHED,"Mô tả chính thức","Người học","Mục tiêu","Nghe\nNói",null,null,null,parent,fee));}
  void all(){publish(Kind.PROGRAM,program.getId(),"tieng-anh",null,false);publish(Kind.BRANCH,branch.getId(),"chi-nhanh-1",null,false);publish(Kind.COURSE,course.getId(),"giao-tiep",program.getId(),false);publish(Kind.CLASS,schoolClass.getId(),"lop-1",null,false);}
  @Test void existingOperationalDataIsPrivateByDefault(){for(var k:Kind.values())assertTrue(service.list(k,0,10).items().isEmpty());}
  @Test void publishedDataUsesLiveSourceAndOmitsPrivateFields(){all();var item=service.detail(Kind.COURSE,"giao-tiep");assertNull(item.tuitionFee());assertEquals(1200,item.expectedMinutes());course.setName("Tên đã cập nhật");assertEquals("Tên đã cập nhật",service.detail(Kind.COURSE,"giao-tiep").name());assertEquals("Chi nhánh 1",service.detail(Kind.CLASS,"lop-1").branchName());assertFalse(Arrays.stream(WebsiteDto.Item.class.getRecordComponents()).anyMatch(r->r.getName().toLowerCase().matches(".*(teacher|student|account|updatedby|room).*")));}
  @Test void disablingAncestorImmediatelyHidesCoursesAndClasses(){all();branch.setStatus(Branch.Status.INACTIVE);em.flush();assertTrue(service.list(Kind.COURSE,0,10).items().isEmpty());assertTrue(service.list(Kind.CLASS,0,10).items().isEmpty());assertThrows(ApiException.class,()->service.detail(Kind.CLASS,"lop-1"));branch.setStatus(Branch.Status.ACTIVE);program.setActive(false);em.flush();assertTrue(service.list(Kind.COURSE,0,10).items().isEmpty());assertTrue(service.list(Kind.CLASS,0,10).items().isEmpty());}
  @Test void fullStartedAndFinishedClassesAreNotRecruiting(){all();schoolClass.setCurrentSize(20);em.flush();assertTrue(service.list(Kind.CLASS,0,10).items().isEmpty());schoolClass.setCurrentSize(0);schoolClass.setStartDate(LocalDate.now(BusinessTime.ZONE));em.flush();assertTrue(service.list(Kind.CLASS,0,10).items().isEmpty());schoolClass.setStartDate(LocalDate.now(BusinessTime.ZONE).plusDays(2));schoolClass.setStatus(SchoolClass.Status.FINISHED);em.flush();assertTrue(service.list(Kind.CLASS,0,10).items().isEmpty());}
  @Test void filtersAndPagingApplyBeforePublicProjection(){all();assertEquals(1,service.list(Kind.COURSE,0,1,"tieng-anh",null,"chi-nhanh-1").items().size());assertTrue(service.list(Kind.COURSE,0,1,"unknown",null,null).items().isEmpty());assertEquals(1,service.list(Kind.CLASS,0,1,null,"giao-tiep",null).items().size());assertTrue(service.list(Kind.CLASS,1,1).items().isEmpty());assertThrows(ApiException.class,()->service.list(Kind.PROGRAM,0,101));}
  @Test void explicitlyPublishedFeesStaySynchronized(){publish(Kind.PROGRAM,program.getId(),"tieng-anh",null,false);publish(Kind.BRANCH,branch.getId(),"chi-nhanh-1",null,false);publish(Kind.COURSE,course.getId(),"giao-tiep",program.getId(),true);course.setTuitionFee(new BigDecimal("4000000"));assertEquals(0,new BigDecimal("4000000").compareTo(service.detail(Kind.COURSE,"giao-tiep").tuitionFee()));}
  @Test void staleEditsUnsafeLinksAndDraftsAreHandled(){var p=publish(Kind.PROGRAM,program.getId(),"tieng-anh",null,false);assertThrows(ApiException.class,()->service.edit(Kind.PROGRAM,new WebsiteDto.Edit(program.getId(),-1L,"tieng-anh",State.DRAFT,null,null,null,null,null,null,null,null,false)));assertThrows(ApiException.class,()->service.edit(Kind.BRANCH,new WebsiteDto.Edit(branch.getId(),0L,"cn1",State.PUBLISHED,"Mô tả",null,null,null,null,null,"javascript:alert(1)",null,false)));service.edit(Kind.PROGRAM,new WebsiteDto.Edit(program.getId(),p.getVersion(),"tieng-anh",State.DRAFT,null,null,null,null,null,null,null,null,false));assertThrows(ApiException.class,()->service.detail(Kind.PROGRAM,"tieng-anh"));}
  @Test void policyChangeHidesCourseUntilItsProgramMappingIsReviewed(){
    all();Program second=new Program();second.setCode("IELTS");second.setName("IELTS");em.persist(second);
    var a=new CourseAdmissionPolicy();a.setCourseId(course.getId());a.setRevision(1);a.setMode(CourseAdmissionPolicy.Mode.OPEN_ENTRY);a.setProgramId(second.getId());a.setProgramCode("IELTS");a.setProgramName("IELTS");a.setOutputLevelId(1L);a.setOutputLevelCode("A1");a.setOutputLevelName("A1");a.setConfirmedBy(1L);a.setConfirmedByName("Admin");a.setConfirmedAt(LocalDateTime.now(BusinessTime.ZONE));a.setReason("Changed program");em.persist(a);em.flush();
    assertTrue(service.list(Kind.COURSE,0,10).items().isEmpty());assertTrue(service.list(Kind.CLASS,0,10).items().isEmpty());
    assertThrows(ApiException.class,()->service.edit(Kind.COURSE,new WebsiteDto.Edit(course.getId(),repo.findByKindAndSourceId(Kind.COURSE,course.getId()).orElseThrow().getVersion(),"giao-tiep",State.PUBLISHED,"Mô tả",null,null,null,null,null,null,program.getId(),false)));
  }
  @Test void paginationHasNextAndDraftRowsDoNotConsumePublicSlots(){
    publish(Kind.PROGRAM,program.getId(),"tieng-anh",null,false);
    var draft=new Program();draft.setCode("DRAFT");draft.setName("Draft");em.persist(draft);
    service.edit(Kind.PROGRAM,new WebsiteDto.Edit(draft.getId(),0L,"draft",State.DRAFT,null,null,null,null,null,null,null,null,false));
    var second=new Program();second.setCode("IELTS");second.setName("IELTS");em.persist(second);publish(Kind.PROGRAM,second.getId(),"ielts",null,false);
    var first=service.list(Kind.PROGRAM,0,1);assertTrue(first.hasNext());assertEquals("tieng-anh",first.items().getFirst().slug());
    var next=service.list(Kind.PROGRAM,1,1);assertFalse(next.hasNext());assertEquals("ielts",next.items().getFirst().slug());
  }
  @Test void keywordAndPriceFiltersNeverInferHiddenTuition(){
    all();assertEquals(1,service.list(Kind.COURSE,0,10,null,null,null,"giao",null).items().size());
    assertTrue(service.list(Kind.COURSE,0,10,null,null,null,null,new BigDecimal("5000000")).items().isEmpty());
    var p=repo.findByKindAndSourceId(Kind.COURSE,course.getId()).orElseThrow();p.setShowTuition(true);em.flush();
    assertEquals(1,service.list(Kind.COURSE,0,10,null,null,null,null,new BigDecimal("5000000")).items().size());
    assertTrue(service.list(Kind.COURSE,0,10,null,null,null,"absent",null).items().isEmpty());
  }
  @Test void sharedCoursesAppearAtPublishedBranchFilters(){all();course.setBranchId(null);em.flush();assertEquals(1,service.list(Kind.COURSE,0,10,"tieng-anh",null,"chi-nhanh-1").items().size());assertTrue(service.list(Kind.COURSE,0,10,null,null,"unknown").items().isEmpty());}
  @Test void publicAudienceFilterUsesCanonicalGroupsAndDoesNotDuplicate(){
    all();var a=new com.example.academic.course.AudienceGroup();a.setCode("SECONDARY");a.setName("THCS");em.persist(a);var b=new com.example.academic.course.AudienceGroup();b.setCode("HIGH_SCHOOL");b.setName("THPT");em.persist(b);course.getAudienceGroups().add(a);course.getAudienceGroups().add(b);course.setProgramId(program.getId());em.flush();
    var page=service.list(Kind.COURSE,0,1,"tieng-anh",null,null,null,null,"SECONDARY");assertEquals(1,page.items().size());assertFalse(page.hasNext());assertEquals(2,page.items().getFirst().classification().audiences().size());assertTrue(service.list(Kind.COURSE,0,10,null,null,null,null,null,"PRIMARY").items().isEmpty());
    a.setActive(false);em.flush();assertTrue(service.list(Kind.COURSE,0,10,null,null,null,null,null,"SECONDARY").items().isEmpty());assertThrows(ApiException.class,()->service.list(Kind.COURSE,0,10,null,null,null,null,null,"UNKNOWN"));
  }
  @Test void editorialHistoryAndBranchDraftPermissions(){
    var p=publish(Kind.BRANCH,branch.getId(),"cn-1",null,false);assertEquals(1,service.history(Kind.BRANCH,branch.getId()).size());
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(2L,2L,branch.getId(),Role.ADMIN,"admin",List.of(branch.getId())),null,List.of()));
    assertThrows(ApiException.class,()->service.edit(Kind.BRANCH,new WebsiteDto.Edit(branch.getId(),p.getVersion(),"cn-1",State.DRAFT,"Mô tả",null,null,null,null,null,null,null,false)));
    assertTrue(service.sources(Kind.PROGRAM,0).isEmpty());
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(3L,3L,999L,Role.ADMIN,"other",List.of(999L)),null,List.of()));
    assertTrue(service.edits(Kind.BRANCH,0).isEmpty());assertThrows(ApiException.class,()->service.history(Kind.BRANCH,branch.getId()));
  }
}
