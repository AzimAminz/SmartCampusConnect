package smartcampus.booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Client to call student-service (gateway) for session authentication.
 */
@Component
public class AuthClient {

    private final WebClient webClient;

    public AuthClient(@Value("${student.service.url:http://student-service:8080}") String studentUrl) {
        this.webClient = WebClient.builder().baseUrl(studentUrl).build();
    }

    /**
     * Resolves user session from the student-service using the token.
     * GET /api/auth/me with header X-Auth-Token.
     */
    public Map<String, Object> verifyToken(String token) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> session = webClient.get()
                    .uri("/api/auth/me")
                    .header("X-Auth-Token", token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Synchronous blocking to ensure token validity
            return session;
        } catch (Exception e) {
            System.err.println("⚠️  [AUTH-CLIENT] Token verification failed: " + e.getMessage());
            return null;
        }
    }
}
