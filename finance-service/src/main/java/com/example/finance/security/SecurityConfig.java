package com.example.finance.security;

import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @org.springframework.beans.factory.annotation.Value(
      "${app.cors.allowed-origins:http://localhost:5173}")
  private String allowedOrigins;

  @Bean
  SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
    return http.cors(c -> c.configurationSource(cors()))
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .denyAll()
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/", "/error")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/finance/student/me/tuitions","/api/v1/finance/student/me/tuitions/summary","/api/v1/finance/student/me/tuitions/*","/api/v1/finance/student/me/tuitions/*/payment-context","/api/v1/finance/student/me/receipts","/api/v1/finance/student/me/receipts/*").hasRole("STUDENT")
                    .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/finance/payment-providers/vnpay/ipn","/api/v1/finance/payment-providers/vnpay/return").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/finance/student/me/payment-config","/api/v1/finance/student/me/payment-contexts","/api/v1/finance/student/me/payment-orders","/api/v1/finance/student/me/payment-orders/*","/api/v1/finance/student/me/payment-orders/requests/*").hasRole("STUDENT")
                    .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/finance/student/me/payment-orders").hasRole("STUDENT")
                    .anyRequest()
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF", "TEACHER"))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter().write("{\"message\":\"Chưa đăng nhập\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter()
                              .write("{\"message\":\"Bạn không có quyền thực hiện thao tác này\"}");
                        }))
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  FilterRegistrationBean<JwtAuthenticationFilter> disableServletRegistration(
      JwtAuthenticationFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  CorsConfigurationSource cors() {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(
        java.util.Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(v -> !v.isEmpty())
            .toList());
    c.setExposedHeaders(List.of("Content-Disposition", "X-Request-Id"));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Branch-Id", "X-Request-Id"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
