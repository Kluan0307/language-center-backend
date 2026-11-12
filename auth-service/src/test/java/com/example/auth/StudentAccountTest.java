package com.example.auth;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import com.example.auth.service.*;
import com.example.auth.student.*;
import com.example.auth.dto.*;
import com.example.auth.security.AccountPrincipal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:student-account-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class StudentAccountTest {
 @Autowired StudentAccountService service; @Autowired AuthService auth; @Autowired AccountSecurityService security;
 @Autowired AccountRepository accounts; @Autowired SuperAdminRepository supers; @Autowired StudentProvisioningRepository provisions;
 @Autowired PasswordEncoder encoder; @Autowired JdbcTemplate jdbc; @Autowired WebApplicationContext context;
 MockMvc mvc; Account sa;
 @BeforeEach void setup(){jdbc.execute("CREATE TABLE IF NOT EXISTS students(id BIGINT PRIMARY KEY,account_id BIGINT UNIQUE,branch_id BIGINT NOT NULL,full_name VARCHAR(255),email VARCHAR(255),phone VARCHAR(30),version BIGINT NOT NULL)");jdbc.update("INSERT INTO students(id,branch_id,full_name,phone,version) VALUES(9001,11,'Student A','0900000000',0),(9002,22,'Student B','0900000001',0)");sa=accounts.saveAndFlush(Account.builder().username("sa-"+UUID.randomUUID()).role(Role.SUPER_ADMIN).password(encoder.encode("original-pass")).status(true).build());var profile=new SuperAdmin();profile.setAccount(sa);profile.setFullName("SA");supers.saveAndFlush(profile);actor(sa);mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 void actor(Account a){var p=new AccountPrincipal(a);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities()));}
 StudentAccountService.Create request(){return new StudentAccountService.Create(UUID.randomUUID(),9001L,0L,"hv-"+UUID.randomUUID().toString().substring(0,8),"temporary-pass","temporary-pass");}
 LoginResponse login(String username,String password){var r=new LoginRequest();r.setUsername(username);r.setPassword(password);return auth.login(r);}
 @Test void createsLinkWithoutBranchAssignmentsAndReplays(){var r=request();var v=service.create(r,sa.getUsername());assertTrue(v.linked());assertEquals(1,v.studentVersion());assertEquals(v.accountId(),service.create(r,sa.getUsername()).accountId());assertEquals(v.accountId(),service.request(r.requestId().toString(),sa.getUsername()).accountId());var a=accounts.findById(v.accountId()).orElseThrow();assertEquals(Role.STUDENT,a.getRole());assertTrue(a.activeBranchIds().isEmpty());assertTrue(a.isMustChangePassword());assertNull(auth.getByUsername(a.getUsername()).getEmail());assertNull(auth.getByUsername(a.getUsername()).getPhone());assertFalse(a.getPassword().contains("temporary-pass"));assertTrue(login(a.getUsername(),"temporary-pass").isMustChangePassword());}
 @Test void staleProfileDoesNotCreateAccount(){var r=request();var stale=new StudentAccountService.Create(r.requestId(),r.studentId(),4L,r.username(),r.temporaryPassword(),r.confirmPassword());assertThrows(com.example.auth.common.ApiException.class,()->service.create(stale,sa.getUsername()));assertFalse(accounts.existsByUsername(r.username()));}
 @Test void duplicateStudentOrChangedIntentIsRejected(){var r=request();service.create(r,sa.getUsername());assertThrows(com.example.auth.common.ApiException.class,()->service.create(request(),sa.getUsername()));assertThrows(com.example.auth.common.ApiException.class,()->service.create(new StudentAccountService.Create(r.requestId(),r.studentId(),0L,r.username(),"different-pass","different-pass"),sa.getUsername()));}
 @Test void adminScopeAndStaffAreDenied(){var admin=Account.builder().username("admin-test").role(Role.ADMIN).password("hash").status(true).build();var b=new AccountBranch();b.setAccount(admin);b.setBranchId(22L);b.setStatus(true);b.setPrimary(true);b.setAssignedAt(java.time.LocalDateTime.now());admin.getBranchAssignments().add(b);accounts.saveAndFlush(admin);assertThrows(com.example.auth.common.ApiException.class,()->service.get(9001L,admin.getUsername()));admin.setRole(Role.STAFF);accounts.saveAndFlush(admin);assertThrows(com.example.auth.common.ApiException.class,()->service.get(9002L,admin.getUsername()));}
 @Test void activationRevocationAndLegacyRoutes() throws Exception {
  var r=request();var v=service.create(r,sa.getUsername());var login=login(r.username(),r.temporaryPassword());SecurityContextHolder.clearContext();
  mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+login.getToken())).andExpect(status().isOk());
  mvc.perform(get("/api/v1/auth/accounts").header("Authorization","Bearer "+login.getToken())).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/auth/me/password").header("Authorization","Bearer "+login.getToken()).contentType("application/json").content("{\"currentPassword\":\"temporary-pass\",\"newPassword\":\"student-new-pass\",\"confirmPassword\":\"student-new-pass\"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+login.getToken())).andExpect(status().isUnauthorized());
  var active=login(r.username(),"student-new-pass");SecurityContextHolder.clearContext();
  for(String url:List.of("/api/v1/auth/unknown-route","/api/v1/auth/accounts","/api/v1/auth/teachers","/api/v1/auth/student-accounts?studentId=9002"))mvc.perform(get(url).header("Authorization","Bearer "+active.getToken())).andExpect(status().isForbidden());
  actor(sa);service.status(v.accountId(),new StudentAccountService.Status(false,v.version(),"verified lock"),sa.getUsername());SecurityContextHolder.clearContext();
  mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+active.getToken())).andExpect(status().isUnauthorized());
 }
 @Test void resetKeepsLockAndRequiresVersion(){var r=request();var v=service.create(r,sa.getUsername());var locked=service.status(v.accountId(),new StudentAccountService.Status(false,v.version(),"verified lock"),sa.getUsername());var reset=service.reset(v.accountId(),new StudentAccountService.Reset("reset-pass-123","reset-pass-123",locked.version(),"verified caller"),sa.getUsername());assertFalse(reset.active());assertTrue(reset.mustChangePassword());assertThrows(com.example.auth.common.ApiException.class,()->service.status(v.accountId(),new StudentAccountService.Status(true,v.version(),"stale version"),sa.getUsername()));}
 @Test void unlinkedStudentCannotLogin(){var a=accounts.saveAndFlush(Account.builder().username("unlinked-student").role(Role.STUDENT).password(encoder.encode("unlinked-pass")).status(true).branchId(11L).build());assertThrows(com.example.auth.common.ApiException.class,()->login(a.getUsername(),"unlinked-pass"));}
}
