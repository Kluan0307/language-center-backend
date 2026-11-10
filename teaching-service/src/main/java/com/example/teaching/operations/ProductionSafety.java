package com.example.teaching.operations;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSafety {
  private final Environment env;

  public ProductionSafety(Environment env) {
    this.env = env;
  }

  @PostConstruct
  void validate() {
    String origins = env.getRequiredProperty("app.cors.allowed-origins");
    if (origins.isBlank() || origins.contains("*"))
      throw new IllegalStateException("Explicit CORS origins required in prod");
    if (!"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")))
      throw new IllegalStateException("Prod requires DDL validate");
    if (Boolean.parseBoolean(env.getProperty("spring.jpa.show-sql", "false")))
      throw new IllegalStateException("SQL logging must be off in prod");
    if (Boolean.parseBoolean(env.getProperty("auth.bootstrap.enabled", "false")))
      throw new IllegalStateException("Bootstrap must be off in prod");
    if (env.getRequiredProperty("jwt.secret")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8)
            .length
        < 32) throw new IllegalStateException("JWT secret must have at least 32 bytes");
    if (!java.nio.file.Path.of(env.getRequiredProperty("teaching.storage.path")).isAbsolute())
      throw new IllegalStateException("Prod document storage must use an absolute path");
  }
}
