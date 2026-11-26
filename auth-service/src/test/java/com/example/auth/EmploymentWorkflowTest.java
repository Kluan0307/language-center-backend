package com.example.auth;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.auth.common.*;
import com.example.auth.entity.*;
import com.example.auth.service.*;
import com.example.auth.integration.BranchDirectory;
import com.example.auth.dto.LoginRequest;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:employment-workflow;MODE=MySQL;DB_CLOSE_DELAY=-1") @Transactional
class EmploymentWorkflowTest {
 @Autowired EntityManager em; @Autowired EmploymentService service; @Autowired AuthService auth;
 @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
 @MockitoBean BranchDirectory branches;
 Account manager,target; Staff profile;
 final LocalDate today=LocalDate.now(BusinessTime.ZONE);
 Account account(String username,Role role,long branch){var a=Account.builder().username(username).password(passwords.encode("workflow-pass")).role(role).status(true).branchId(branch).build();em.persist(a);var b=new AccountBranch();b.setAccount(a);b.setBranchId(branch);b.setStatus(true);b.setAssignedAt(LocalDateTime.now(BusinessTime.ZONE));a.getBranchAssignments().add(b);em.persist(b);return a;}
 @BeforeEach void setup(){manager=account("employment-sa",Role.SUPER_ADMIN,1);em.persist(SuperAdmin.builder().account(manager).fullName("Manager").build());target=account("employment-staff",Role.STAFF,1);profile=Staff.builder().account(target).fullName("Staff").hireDate(today.minusYears(1)).workStatus("ACTIVE").build();em.persist(profile);em.flush();}
 EmploymentService.Update change(long version,String status,LocalDate hire,LocalDate end){return new EmploymentService.Update(version,status,hire,end,"Verified change");}
 @Test void offboardingPreservesAccountAndAssignmentsButRevokesSession(){var before=target.getSessionVersion();var r=service.update(target.getId(),change(profile.getVersion(),"INACTIVE",profile.getHireDate(),today),manager.getUsername());assertEquals("INACTIVE",r.workStatus());assertEquals(today,r.endDate());assertTrue(target.getStatus());assertEquals(List.of(1L),target.activeBranchIds());assertEquals(before+1,target.getSessionVersion());assertEquals(1,service.history(target.getId(),manager.getUsername()).size());var login=new LoginRequest();login.setUsername(target.getUsername());login.setPassword("workflow-pass");assertThrows(ApiException.class,()->auth.login(login));}
 @Test void staleVersionCannotOverwriteEmployment(){assertThrows(ApiException.class,()->service.update(target.getId(),change(profile.getVersion()+1,"INACTIVE",profile.getHireDate(),today),manager.getUsername()));assertEquals("ACTIVE",profile.getWorkStatus());}
 @Test void outstandingLeadPreventsOffboarding(){doThrow(ApiException.conflict("handover required")).when(branches).requireNoActiveLeads(target.getId(),List.of(1L));assertThrows(ApiException.class,()->service.update(target.getId(),change(profile.getVersion(),"INACTIVE",profile.getHireDate(),today),manager.getUsername()));assertEquals("ACTIVE",profile.getWorkStatus());assertEquals(0,target.getSessionVersion());}
 @Test void adminCannotModifyOutsideAssignments(){var a=account("outside-admin",Role.ADMIN,2);em.persist(Admin.builder().account(a).fullName("Admin").build());em.flush();assertThrows(ApiException.class,()->service.get(target.getId(),a.getUsername()));}
 @Test void reactivationDoesNotUnlockAccount(){service.update(target.getId(),change(profile.getVersion(),"INACTIVE",profile.getHireDate(),today),manager.getUsername());target.setStatus(false);em.flush();var r=service.update(target.getId(),change(profile.getVersion(),"ACTIVE",profile.getHireDate(),null),manager.getUsername());assertNull(r.endDate());assertFalse(target.getStatus());}
 @Test void invalidDateCombinationsAreRejected(){assertThrows(ApiException.class,()->EmploymentRules.validate("ACTIVE",today.plusDays(1),null));assertThrows(ApiException.class,()->EmploymentRules.validate("INACTIVE",today,today.minusDays(1)));assertThrows(ApiException.class,()->EmploymentRules.validate("INACTIVE",today,null));assertThrows(ApiException.class,()->EmploymentRules.validate("INACTIVE",today,today.plusDays(1)));assertThrows(ApiException.class,()->EmploymentRules.validate("ACTIVE",today,today));}
}
