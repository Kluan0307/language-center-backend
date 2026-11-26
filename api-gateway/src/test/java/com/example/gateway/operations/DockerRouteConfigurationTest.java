package com.example.gateway.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

class DockerRouteConfigurationTest {
  private GatewayProperties routes(boolean docker, Map<String, Object> overrides) throws IOException {
    var environment = new StandardEnvironment();
    // Isolate configuration binding from the developer's environment and private config files.
    environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    environment.getPropertySources().addLast(new ResourcePropertySource("base", "classpath:application.properties"));
    if (docker) environment.getPropertySources().addFirst(new ResourcePropertySource("docker", "classpath:application-docker.properties"));
    environment.getPropertySources().addFirst(new MapPropertySource("runtime", overrides));
    return Binder.get(environment).bind("spring.cloud.gateway.server.webflux", Bindable.of(GatewayProperties.class)).get();
  }

  private void predicatesRemain(GatewayProperties properties) {
    assertThat(properties.getRoutes()).hasSize(5);
    var names = List.of("auth", "academic", "teaching", "finance", "assistant");
    for (int index = 0; index < names.size(); index++) {
      var route = properties.getRoutes().get(index);
      assertThat(route.getId()).isEqualTo(names.get(index) + "-service");
      assertThat(route.getPredicates()).hasSize(1);
      assertThat(route.getPredicates().getFirst().getName()).isEqualTo("Path");
      assertThat(route.getPredicates().getFirst().getArgs()).containsValue("/api/v1/" + names.get(index) + "/**");
    }
    assertThat(String.valueOf(properties.getRoutes().get(4).getMetadata().get("response-timeout"))).isEqualTo("40000");
  }

  @Test void localDefaultsStayOnLoopback() throws IOException {
    var properties = routes(false, Map.of());
    predicatesRemain(properties);
    assertThat(properties.getRoutes().getFirst().getUri().toString()).isEqualTo("http://localhost:8081");
    assertThat(properties.getRoutes().get(4).getUri().toString()).isEqualTo("http://localhost:8086");
  }

  @Test void dockerUsesServiceDnsWithoutReplacingRouteDefinitions() throws IOException {
    var properties = routes(true, Map.of());
    predicatesRemain(properties);
    assertThat(properties.getRoutes().getFirst().getUri().toString()).isEqualTo("http://auth-service:8081");
    assertThat(properties.getRoutes().get(4).getUri().toString()).isEqualTo("http://assistant-service:8086");
  }

  @Test void runtimeUrlsStillTakePriority() throws IOException {
    var properties = routes(true, Map.of("AUTH_URL", "http://auth-test:19081", "ASSISTANT_URL", "http://assistant-test:19086"));
    predicatesRemain(properties);
    assertThat(properties.getRoutes().getFirst().getUri().toString()).isEqualTo("http://auth-test:19081");
    assertThat(properties.getRoutes().get(4).getUri().toString()).isEqualTo("http://assistant-test:19086");
  }
}
