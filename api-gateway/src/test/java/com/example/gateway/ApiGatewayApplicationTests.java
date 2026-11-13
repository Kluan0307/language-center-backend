package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApiGatewayApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.cloud.gateway.route.RouteDefinitionLocator routes;

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.context.ApplicationContext context;

    @Test
    void allFiveApiPrefixesAreRoutedWithoutRewriting() {
        var definitions=routes.getRouteDefinitions().collectList().block(java.time.Duration.ofSeconds(5));
        org.junit.jupiter.api.Assertions.assertNotNull(definitions);
        org.junit.jupiter.api.Assertions.assertEquals(5,definitions.size());
        for(String service:java.util.List.of("auth","academic","finance","teaching","assistant")) {
            org.junit.jupiter.api.Assertions.assertTrue(definitions.stream().anyMatch(route ->
                route.getPredicates().stream().anyMatch(predicate -> predicate.getName().equals("Path") &&
                    predicate.getArgs().containsValue("/api/v1/"+service+"/**"))));
        }
    }

    @Test void assistantTimeoutIsRouteSpecific(){
        var definitions=routes.getRouteDefinitions().collectList().block(java.time.Duration.ofSeconds(5));
        var assistant=definitions.stream().filter(r->r.getId().equals("assistant-service")).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(40000,Long.parseLong(String.valueOf(assistant.getMetadata().get("response-timeout"))));
        org.junit.jupiter.api.Assertions.assertTrue(definitions.stream().filter(r->!r.getId().equals("assistant-service")).noneMatch(r->r.getMetadata().containsKey("response-timeout")));
    }

    @Test
    void browserPreflightDoesNotRequireLoginOrCallDownstream() {
        org.springframework.test.web.reactive.server.WebTestClient.bindToApplicationContext(context).configureClient().baseUrl("http://localhost:8080").build()
            .options().uri("/api/v1/auth/login").header("Origin","http://localhost:5173")
            .header("Access-Control-Request-Method","POST")
            .header("Access-Control-Request-Headers","Authorization,Content-Type")
            .exchange().expectStatus().isOk().expectHeader().valueEquals("Access-Control-Allow-Origin","http://localhost:5173");
    }

    @Test void assistantCorsExposesRateLimitDelayWithoutOpeningOrigins() {
        var source=context.getBean(org.springframework.web.cors.reactive.CorsConfigurationSource.class);
        var allowed=org.springframework.mock.http.server.reactive.MockServerHttpRequest.options("/api/v1/assistant/chat")
            .header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST").build();
        var config=source.getCorsConfiguration(org.springframework.mock.web.server.MockServerWebExchange.from(allowed));
        org.junit.jupiter.api.Assertions.assertTrue(config.getExposedHeaders().contains("Retry-After"));
        org.junit.jupiter.api.Assertions.assertEquals("http://localhost:5173",config.checkOrigin("http://localhost:5173"));
        org.junit.jupiter.api.Assertions.assertNull(config.checkOrigin("https://outside.invalid"));
    }

	@Test
	void contextLoads() {
	}

}
