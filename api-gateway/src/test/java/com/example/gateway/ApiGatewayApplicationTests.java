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
    void allFourApiPrefixesAreRoutedWithoutRewriting() {
        var definitions=routes.getRouteDefinitions().collectList().block(java.time.Duration.ofSeconds(5));
        org.junit.jupiter.api.Assertions.assertNotNull(definitions);
        org.junit.jupiter.api.Assertions.assertEquals(4,definitions.size());
        for(String service:java.util.List.of("auth","academic","finance","teaching")) {
            org.junit.jupiter.api.Assertions.assertTrue(definitions.stream().anyMatch(route ->
                route.getPredicates().stream().anyMatch(predicate -> predicate.getName().equals("Path") &&
                    predicate.getArgs().containsValue("/api/v1/"+service+"/**"))));
        }
    }

    @Test
    void browserPreflightDoesNotRequireLoginOrCallDownstream() {
        org.springframework.test.web.reactive.server.WebTestClient.bindToApplicationContext(context).configureClient().baseUrl("http://localhost:8080").build()
            .options().uri("/api/v1/auth/login").header("Origin","http://localhost:5173")
            .header("Access-Control-Request-Method","POST")
            .header("Access-Control-Request-Headers","Authorization,Content-Type")
            .exchange().expectStatus().isOk().expectHeader().valueEquals("Access-Control-Allow-Origin","http://localhost:5173");
    }

	@Test
	void contextLoads() {
	}

}
