package com.example.academic.operations;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyTest {
  MockEnvironment valid() {
    return new MockEnvironment()
        .withProperty("app.cors.allowed-origins", "https://language.example")
        .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
        .withProperty("jwt.secret", "test-only-long-secret-at-least-32-bytes")
        .withProperty(
            "teaching.storage.path",
            java.nio.file.Path.of("target/documents").toAbsolutePath().toString());
  }

  @Test
  void acceptsExplicitSafeConfiguration() {
    assertDoesNotThrow(() -> new ProductionSafety(valid()).validate());
  }

  @Test
  void rejectsWildcardCors() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ProductionSafety(valid().withProperty("app.cors.allowed-origins", "*")).validate());
  }

  @Test
  void rejectsSchemaMutation() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ProductionSafety(valid().withProperty("spring.jpa.hibernate.ddl-auto", "update"))
                .validate());
  }

  @Test
  void rejectsAccidentalBootstrap() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ProductionSafety(valid().withProperty("auth.bootstrap.enabled", "true"))
                .validate());
  }
}
