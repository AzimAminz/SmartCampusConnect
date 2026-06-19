package smartcampus.enrolment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Student HTTP Client for enrolment-service.
 * Replaces the original @Autowired StudentRepository with an HTTP call to student-service.
 *
 * Calls: GET /api/students/{id} on student-service (port 8080).
 * Returns a StudentDTO (id, studentId, name) or throws if not found.
 */
@Component
public class StudentClient {

    private final WebClient webClient;

    public StudentClient(@Value("${student.service.url:http://backend:8080}") String studentUrl) {
        this.webClient = WebClient.builder().baseUrl(studentUrl).build();
    }

    /**
     * Verify that a student with the given primary-key ID exists.
     * Returns a Map with { "id", "studentId", "name" } if found, or null if not found.
     */
    public Map<String, Object> findById(Long id) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> student = webClient.get()
                    .uri("/api/students/id/" + id)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Synchronous for validation — student must exist before enrolment
            return student;
        } catch (Exception e) {
            System.err.println("⚠️  [STUDENT-CLIENT] Could not fetch student id=" + id + ": " + e.getMessage());
            return null;
        }
    }
}
