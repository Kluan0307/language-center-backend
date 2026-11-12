package com.example.finance;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import com.example.finance.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
@SpringJUnitConfig(StudentRouteSecurityTest.Config.class) @WebAppConfiguration
class StudentRouteSecurityTest {
 @Configuration @EnableWebMvc @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity @Import({SecurityConfig.class,Canary.class}) static class Config {
  @Bean JwtAuthenticationFilter filter(){return new JwtAuthenticationFilter("test-only-secret-at-least-thirty-two-bytes",mock(SessionDirectory.class));}
 }
 @RestController static class Canary {
  @GetMapping({"/api/v1/finance/student/me/tuitions","/api/v1/finance/student/me/tuitions/summary","/api/v1/finance/student/me/tuitions/1","/api/v1/finance/student/me/tuitions/1/payment-context","/api/v1/finance/student/me/receipts","/api/v1/finance/student/me/receipts/1"}) public String finance(){return "ok";}
  @GetMapping("/api/v1/finance/legacy-canary") public String legacy(){return "ok";}
  @GetMapping("/api/v1/finance/student/me/profile") public String student(){return "ok";}
 }
 @Autowired WebApplicationContext context;org.springframework.test.web.servlet.MockMvc mvc;
 @BeforeEach void setup(){mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @Test @WithMockUser(roles="STUDENT") void studentCannotReachLegacy() throws Exception {mvc.perform(get("/api/v1/finance/legacy-canary")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="TEACHER") void oldRolesStillReachLegacy() throws Exception {mvc.perform(get("/api/v1/finance/legacy-canary")).andExpect(status().isOk());}
 @Test void anonymousHasNoAccess() throws Exception {mvc.perform(get("/api/v1/finance/legacy-canary")).andExpect(status().isUnauthorized());}
 @Test @WithMockUser(roles="STUDENT") void studentReadAllowedButWritesDenied() throws Exception {for(String path:java.util.List.of("tuitions","tuitions/summary","tuitions/1","receipts","receipts/1"))mvc.perform(get("/api/v1/finance/student/me/"+path)).andExpect(status().isOk());mvc.perform(post("/api/v1/finance/student/me/receipts")).andExpect(status().isForbidden());mvc.perform(post("/api/v1/finance/payments")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="STAFF") void staffCannotUseStudentFinanceProjection() throws Exception {mvc.perform(get("/api/v1/finance/student/me/tuitions")).andExpect(status().isForbidden());}
 
}
