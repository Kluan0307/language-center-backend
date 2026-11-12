package com.example.academic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import com.example.academic.security.*;
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
  @GetMapping({"/api/v1/academic/student/me/document-scopes","/api/v1/academic/student/me/support-requests","/api/v1/academic/student/me/support-requests/7"}) public String resources(){return "ok";}
  @PostMapping({"/api/v1/academic/student/me/support-requests","/api/v1/academic/student/me/support-requests/7/messages","/api/v1/academic/student/me/support-requests/7/close"}) public String supportWrite(){return "ok";}
  @GetMapping("/api/v1/academic/legacy-canary") public String legacy(){return "ok";}
  @GetMapping({"/api/v1/academic/student/me/profile","/api/v1/academic/student/me/enrollments","/api/v1/academic/student/me/enrollments/7","/api/v1/academic/student/me/schedule","/api/v1/academic/student/me/sessions/7","/api/v1/academic/student/me/next-session"}) public String student(){return "ok";}
 }
 @Autowired WebApplicationContext context;org.springframework.test.web.servlet.MockMvc mvc;
 @BeforeEach void setup(){mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @Test @WithMockUser(roles="STUDENT") void studentCannotReachLegacy() throws Exception {mvc.perform(get("/api/v1/academic/legacy-canary")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="STUDENT") void documentScopesAndSupportAllowedButProfileWriteDenied() throws Exception {for(String route:java.util.List.of("document-scopes","support-requests","support-requests/7"))mvc.perform(get("/api/v1/academic/student/me/"+route)).andExpect(status().isOk());for(String route:java.util.List.of("support-requests","support-requests/7/messages","support-requests/7/close"))mvc.perform(post("/api/v1/academic/student/me/"+route)).andExpect(status().isOk());mvc.perform(patch("/api/v1/academic/student/me/profile")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="STAFF") void employeeCannotUseStudentSupportEndpoints() throws Exception {mvc.perform(get("/api/v1/academic/student/me/support-requests")).andExpect(status().isForbidden());mvc.perform(post("/api/v1/academic/student/me/support-requests")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="TEACHER") void oldRolesStillReachLegacy() throws Exception {mvc.perform(get("/api/v1/academic/legacy-canary")).andExpect(status().isOk());}
 @Test void anonymousHasNoAccess() throws Exception {mvc.perform(get("/api/v1/academic/legacy-canary")).andExpect(status().isUnauthorized());}
 @Test @WithMockUser(roles="STUDENT") void studentProfileIsAllowed() throws Exception {mvc.perform(get("/api/v1/academic/student/me/profile")).andExpect(status().isOk());}
 @Test @WithMockUser(roles="ADMIN") void staffCannotUseStudentProjection() throws Exception {mvc.perform(get("/api/v1/academic/student/me/profile")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="STUDENT") void allStudentReadRoutesAllowed() throws Exception {for(String route:java.util.List.of("enrollments","enrollments/7","schedule","sessions/7","next-session"))mvc.perform(get("/api/v1/academic/student/me/"+route)).andExpect(status().isOk());}
 @Test @WithMockUser(roles="ADMIN") void adminCannotReadStudentRoutes() throws Exception {for(String route:java.util.List.of("enrollments","enrollments/7","schedule","sessions/7","next-session"))mvc.perform(get("/api/v1/academic/student/me/"+route)).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="STUDENT") void studentWritesAndUnknownRoutesStayDenied() throws Exception {mvc.perform(post("/api/v1/academic/student/me/enrollments")).andExpect(status().isForbidden());mvc.perform(get("/api/v1/academic/student/me/unknown")).andExpect(status().isForbidden());}
}
