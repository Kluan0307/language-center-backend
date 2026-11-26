package com.example.academic;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import com.example.academic.security.*;
import com.example.academic.website.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(WebsiteSecurityTest.Config.class) @WebAppConfiguration
@org.springframework.test.context.TestPropertySource(properties="website.form.secret=test-only-website-form-secret-with-32-characters")
class WebsiteSecurityTest {
  @Configuration @EnableWebMvc @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity @Import({SecurityConfig.class,WebsiteCatalogController.class,WebsiteIntakeController.class,WebsiteTeacherController.class,com.example.academic.common.GlobalExceptionHandler.class})
  static class Config {
    @Bean com.example.academic.media.MediaAssetService media(){return mock(com.example.academic.media.MediaAssetService.class);}
    @Bean WebsiteCatalogService service(){return mock(WebsiteCatalogService.class);}
    @Bean WebsiteIntakeService intake(){return mock(WebsiteIntakeService.class);}
    @Bean WebsiteIntakeLimiter limiter(){return new WebsiteIntakeLimiter();}
    @Bean WebsiteTeacherService teachers(){return mock(WebsiteTeacherService.class);}
    @Bean JwtAuthenticationFilter jwt(){return new JwtAuthenticationFilter("test-only-secret-at-least-thirty-two-bytes",mock(SessionDirectory.class));}
  }
  @Autowired WebApplicationContext context;
  @Autowired WebsiteCatalogService service;
  @Autowired WebsiteIntakeService intake;
  @Autowired WebsiteTeacherService teachers;
  MockMvc mvc;
  @BeforeEach void setup(){reset(service,intake,teachers);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
  @Test void anonymousCanOnlyReadPublicCatalog() throws Exception {
    when(service.list(WebsitePublication.Kind.PROGRAM,0,24,null,null,null,null,null)).thenReturn(new WebsiteDto.Page(List.of(),0,24,false));
    mvc.perform(get("/api/v1/academic/public/catalog/PROGRAM")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
    mvc.perform(get("/api/v1/academic/programs")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/academic/website/publications/PROGRAM")).andExpect(status().isUnauthorized());
    mvc.perform(put("/api/v1/academic/public/catalog/PROGRAM")).andExpect(status().isUnauthorized());
  }
  @Test void internalPublicationManagementRequiresSuperadmin() throws Exception {
    for(String role:List.of("STAFF","TEACHER","STUDENT"))mvc.perform(get("/api/v1/academic/website/publications/PROGRAM").with(user("test").roles(role))).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/academic/website/publications/PROGRAM").with(user("test").roles("SUPER_ADMIN"))).andExpect(status().isOk());
  }
  @Test void publicationWriteIsProtectedByMethodSecurity() throws Exception {
    String body="""
      {"sourceId":1,"version":0,"slug":"tieng-anh","state":"DRAFT","summary":"Mô tả","showTuition":false}
      """;
    mvc.perform(put("/api/v1/academic/website/publications/PROGRAM").with(user("test").roles("STAFF")).contentType("application/json").content(body)).andExpect(status().isForbidden());
    mvc.perform(put("/api/v1/academic/website/publications/PROGRAM").with(user("test").roles("SUPER_ADMIN")).contentType("application/json").content(body)).andExpect(status().isOk());
    verify(service).edit(eq(WebsitePublication.Kind.PROGRAM),any());
  }
  final String formBody="""
    {"requestId":"11111111-1111-4111-8111-111111111111","fullName":"Nguyen An","phone":"0901234567","branchSlug":"chi-nhanh-1","consent":true}
    """;
  @Test void intakeRequiresPrivateBffKeyButNoUserAccount() throws Exception {
    mvc.perform(post("/api/v1/academic/public/consultations").contentType("application/json").content(formBody)).andExpect(status().isUnauthorized());
    verifyNoInteractions(intake);
    when(intake.submit(any())).thenReturn(WebsiteIntakeDto.Accepted.received());
    mvc.perform(post("/api/v1/academic/public/consultations").header("X-Website-Key","test-only-website-form-secret-with-32-characters").contentType("application/json").content(formBody)).andExpect(status().isAccepted()).andExpect(jsonPath("$.message").exists()).andExpect(jsonPath("$.leadId").doesNotExist());
  }
  @Test void consentAndHoneypotAreValidatedBeforeLeadCreation() throws Exception {
    for(String bad:List.of(formBody.replace("\"consent\":true","\"consent\":false"),formBody.replace("\"consent\":true","\"consent\":true,\"website\":\"spam\"")))mvc.perform(post("/api/v1/academic/public/consultations").header("X-Website-Key","test-only-website-form-secret-with-32-characters").contentType("application/json").content(bad)).andExpect(status().isBadRequest());
    verifyNoInteractions(intake);
  }
  @Test void onlyApprovedTeacherReadIsPublic() throws Exception {
    when(teachers.list(0,12,null)).thenReturn(new WebsiteTeacherService.Page(List.of(),0,12,false));
    mvc.perform(get("/api/v1/academic/public/teachers")).andExpect(status().isOk());
    mvc.perform(get("/api/v1/academic/website/teachers")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/academic/website/teachers").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/academic/website/teachers").with(user("superadmin").roles("SUPER_ADMIN"))).andExpect(status().isOk());
  }
}
