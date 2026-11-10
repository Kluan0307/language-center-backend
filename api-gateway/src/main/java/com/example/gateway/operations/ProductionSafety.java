package com.example.gateway.operations;

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
  }
}
