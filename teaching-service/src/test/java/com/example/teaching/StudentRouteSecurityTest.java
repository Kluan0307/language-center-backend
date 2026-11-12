package com.example.teaching;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import com.example.teaching.security.*;
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
  @GetMapping({"/api/v1/teaching/student/me/documents","/api/v1/teaching/student/me/documents/1/download"}) public String documents(){return "ok";}
  @GetMapping("/api/v1/teaching/legacy-canary") public String legacy(){return "ok";}
  @GetMapping("/api/v1/teaching/student/me/profile") public String student(){return "ok";}
  @GetMapping({"/api/v1/teaching/student/me/enrollments/1/grades","/api/v1/teaching/student/me/enrollments/1/final-result"}) public String progress(){return "ok";}
 }
 @Autowired WebApplicationContext context;org.springframework.test.web.servlet.MockMvc mvc;
 @BeforeEach void setup(){mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @Test @WithMockUser(roles="STUDENT") void studentCannotReachLegacy() throws Exception {mvc.perform(get("/api/v1/teaching/legacy-canary")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="STUDENT") void studentDocumentReadsAllowedButSharingWriteDenied() throws Exception {mvc.perform(get("/api/v1/teaching/student/me/documents")).andExpect(status().isOk());mvc.perform(get("/api/v1/teaching/student/me/documents/1/download")).andExpect(status().isOk());mvc.perform(patch("/api/v1/teaching/documents/1/student-sharing")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="TEACHER") void oldRolesStillReachLegacy() throws Exception {mvc.perform(get("/api/v1/teaching/legacy-canary")).andExpect(status().isOk());}
 @Test void anonymousHasNoAccess() throws Exception {mvc.perform(get("/api/v1/teaching/legacy-canary")).andExpect(status().isUnauthorized());}
 @Test @WithMockUser(roles="STUDENT") void studentReadOnlyProgress() throws Exception {mvc.perform(get("/api/v1/teaching/student/me/enrollments/1/grades")).andExpect(status().isOk());mvc.perform(get("/api/v1/teaching/student/me/enrollments/1/final-result")).andExpect(status().isOk());mvc.perform(post("/api/v1/teaching/student/me/enrollments/1/grades")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="TEACHER") void operatorCannotUseStudentEndpoints() throws Exception {mvc.perform(get("/api/v1/teaching/student/me/enrollments/1/grades")).andExpect(status().isForbidden());}
 
}
