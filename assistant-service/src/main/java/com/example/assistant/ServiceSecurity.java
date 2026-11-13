package com.example.assistant;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
class ServiceSecurity {
  @Bean
  org.springframework.security.authentication.AuthenticationManager authenticationManager() {
    return authentication -> {
      throw new org.springframework.security.authentication.BadCredentialsException(
          "No public authentication endpoint");
    };
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http, com.example.assistant.chat.AssistantAuthFilter auth) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/assistant/chat",
                        "/api/v1/assistant/config",
                        "/api/v1/assistant/conversations/*")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF", "TEACHER")
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(
            auth,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
                .class)
        .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401)))
        .build();
  }

  @Bean
  org.springframework.boot.web.servlet.FilterRegistrationBean<
          com.example.assistant.chat.AssistantAuthFilter>
      disableAuthServletRegistration(com.example.assistant.chat.AssistantAuthFilter filter) {
    var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
