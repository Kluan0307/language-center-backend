package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import com.example.academic.common.*;
import com.example.academic.security.*;
import com.example.academic.lead.*;
import com.example.academic.student.Student;
import com.example.academic.support.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:academic-work-queue;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class WorkQueueTest {
 @Autowired EntityManager em; @Autowired com.example.academic.stats.WorkQueueController queue; @Autowired SupportService support; @Autowired LeadService leads;
 void actor(Role role,List<Long> ids){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(100L,11L,ids.isEmpty()?null:ids.getFirst(),role,"operator",ids),null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 void seedBranch(long branch){var student=new Student();student.setBranchId(branch);student.setFullName("Student");student.setPhone("0900000000");em.persist(student);var ticket=new SupportRequest();ticket.setStudentId(student.getId());ticket.setBranchId(branch);ticket.setCategory(SupportRequest.Category.OTHER);ticket.setSubject("Support question");ticket.setCreatedAt(LocalDateTime.now());ticket.setUpdatedAt(LocalDateTime.now());em.persist(ticket);var lead=new Lead();lead.setBranchId(branch);lead.setFullName("Lead");lead.setPhone("0900000001");lead.setAssignedTo(100L);lead.setAssignedToName("Operator");em.persist(lead);}
 @BeforeEach void seed(){seedBranch(3);seedBranch(4);seedBranch(5);em.flush();actor(Role.ADMIN,List.of(3L,4L));}
 @Test void aggregateQueueAndDestinationListsHaveIdenticalScopeAndTotals(){var q=queue.get(null,true);assertEquals(2,q.supportActive());assertEquals(2,q.leadsNew());assertEquals(q.supportActive(),support.search(false,0,1,null,null,null,null,null,"ACTIVE",true).totalElements());assertEquals(q.leadsNew(),leads.list(0,1,null,Lead.Status.NEW,null,null,null,null,true).totalElements());assertEquals(1,support.search(false,1,1,null,null,null,null,null,"ACTIVE",true).content().size());assertEquals(1,queue.get(null,false).supportActive());}
 @Test void foreignScopeAndStaffAggregateFlagAreRejected(){assertThrows(ApiException.class,()->queue.get(5L,false));assertThrows(ApiException.class,()->queue.get(3L,true));actor(Role.STAFF,List.of(3L));assertThrows(ApiException.class,()->queue.get(null,true));assertThrows(ApiException.class,()->support.search(false,0,1,null,null,null,null,null,null,true));assertThrows(ApiException.class,()->leads.list(0,1,null,null,null,null,null,null,true));}
 @Test void superAdminSeesWholeSystemAndTeachingRolesCannotQueryOperations(){actor(Role.SUPER_ADMIN,List.of());assertEquals(3,queue.get(null,false).supportActive());assertEquals(0,queue.get(99L,false).leadsNew());for(var role:List.of(Role.TEACHER,Role.STUDENT)){actor(role,List.of(3L));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->queue.get(null,false));}}
}
