package com.example.gateway;

import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.*;

@Configuration
public class SecurityConfig {
  @org.springframework.beans.factory.annotation.Value(
      "${app.cors.allowed-origins:http://localhost:5173}")
  private String allowedOrigins;

  @Bean
  SecurityWebFilterChain security(ServerHttpSecurity http) {
    return http.csrf(c -> c.disable())
        .httpBasic(c -> c.disable())
        .formLogin(c -> c.disable())
        .securityContextRepository(
            org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
                .getInstance())
        .cors(c -> c.configurationSource(cors()))
        .authorizeExchange(
            a ->
                a.pathMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .pathMatchers("/actuator/**")
                    .denyAll()
                    .anyExchange()
                    .permitAll())
        .build();
  }

  @Bean
  CorsConfigurationSource cors() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(
        java.util.Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(v -> !v.isEmpty())
            .toList());
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Branch-Id", "X-Request-Id"));
    c.setExposedHeaders(List.of("Content-Disposition", "X-Request-Id"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
