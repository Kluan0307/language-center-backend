package com.example.auth.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  @org.springframework.beans.factory.annotation.Value(
      "${app.cors.allowed-origins:http://localhost:5173}")
  private String allowedOrigins;

  @Bean
  org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter>
      disableServletRegistration(JwtAuthenticationFilter filter) {
    var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  private final CustomUserDetailsService userDetailsService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(
        java.util.Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(v -> !v.isEmpty())
            .toList());
    config.setExposedHeaders(List.of("Content-Disposition", "X-Request-Id"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .denyAll()
                    .requestMatchers("/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/student-accounts", "/api/v1/auth/student-accounts/**", "/api/v1/auth/student-account-provisionings/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                    .requestMatchers(org.springframework.http.HttpMethod.PUT,"/api/v1/auth/student/me/devices/*").hasRole("STUDENT")
                    .requestMatchers(org.springframework.http.HttpMethod.DELETE,"/api/v1/auth/student/me/devices/*").hasRole("STUDENT")
                    .requestMatchers("/api/v1/auth/me", "/api/v1/auth/me/password", "/api/v1/auth/me/logout-all")
                    .authenticated()
                    .requestMatchers("/api/v1/auth/accounts", "/api/v1/auth/accounts/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                    .requestMatchers("/api/v1/auth/teacher-profiles/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                    .requestMatchers("/api/v1/auth/teachers", "/api/v1/auth/teachers/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                    .anyRequest()
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF", "TEACHER"))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter().write("{\"message\":\"Unauthenticated\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter().write("{\"message\":\"Forbidden\"}");
                        }))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
