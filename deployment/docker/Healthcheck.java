import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Dependency-free readiness probe: never calls a provider or prints response bodies. */
public final class Healthcheck {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
            if (port < 1 || port > 65535) System.exit(1);
            var request = HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + "/actuator/health/readiness"))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                String status = new String(body.readNBytes(4096), java.nio.charset.StandardCharsets.UTF_8);
                if (response.statusCode() != 200 || !status.matches("(?s).*\"status\"\\s*:\\s*\"UP\".*")) System.exit(1);
            }
        } catch (Exception ignored) {
            System.exit(1);
        }
    }
}
