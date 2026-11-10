package com.example.auth;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.auth.controller.AuthController;
import com.example.auth.security.*;
import com.example.auth.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(AuthSecurityUnitTest.Config.class)
@WebAppConfiguration
@org.springframework.test.context.TestPropertySource(
    properties = {
      "jwt.expiration=86400000",
      "jwt.secret=test-only-secret-at-least-thirty-two-bytes"
    })
class AuthSecurityUnitTest {
  @Configuration
  @EnableWebMvc
  @Import({SecurityConfig.class, AuthController.class})
  static class Config {
    @Bean
    AuthService authService() {
      return mock(AuthService.class);
    }

    @Bean
    CustomUserDetailsService users() {
      return mock(CustomUserDetailsService.class);
    }

    @Bean
    JwtUtil jwt() {
      return mock(JwtUtil.class);
    }

    @Bean
    JwtAuthenticationFilter filter(JwtUtil jwt, CustomUserDetailsService users) {
      return new JwtAuthenticationFilter(jwt, users);
    }
  }

  @Autowired WebApplicationContext context;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
  }

  @Test
  @WithMockUser(roles = "SUPER_ADMIN")
  void superAdminCanListAccounts() throws Exception {
    mvc.perform(get("/api/v1/auth/accounts")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void staffCannotListAccounts() throws Exception {
    mvc.perform(get("/api/v1/auth/accounts")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void staffCanListTeachers() throws Exception {
    mvc.perform(get("/api/v1/auth/teachers")).andExpect(status().isOk());
  }

  @Test
  void anonymousCannotListTeachers() throws Exception {
    mvc.perform(get("/api/v1/auth/teachers")).andExpect(status().isUnauthorized());
  }
}
