package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import smartcampus.backend.repository.StudentRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporting REST API — satisfies R4 (Service Composition / Aggregation).
 */
@RestController
@RequestMapping("/api/reporting")
@CrossOrigin(origins = "*")
public class ReportingController {

    @Autowired private StudentRepository studentRepository;

    private final WebClient enrolmentClient;
    private final WebClient bookingClient;
    private final WebClient notificationClient;

    public ReportingController(
            WebClient.Builder webClientBuilder,
            @Value("${enrolment.service.url:http://enrolment-service:8081}") String enrolmentUrl,
            @Value("${booking.service.url:http://booking-service:8082}") String bookingUrl,
            @Value("${notify.service.url:http://notification-service:8083}") String notifyUrl) {
        this.enrolmentClient = webClientBuilder.baseUrl(enrolmentUrl).build();
        this.bookingClient = webClientBuilder.baseUrl(bookingUrl).build();
        this.notificationClient = webClientBuilder.baseUrl(notifyUrl).build();
    }

    /**
     * GET /api/reporting/stats — System-wide stats composed from microservices
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalStudents", studentRepository.count());

        long totalCourses = 0;
        long totalEnrolments = 0;
        try {
            List<?> courses = enrolmentClient.get().uri("/api/courses").retrieve().bodyToMono(List.class).block();
            totalCourses = courses != null ? courses.size() : 0;
        } catch (Exception e) {}
        stats.put("totalCourses", totalCourses);

        // We can get enrolment count or approximate from lists
        stats.put("totalEnrolments", 0); // Can be enriched from DB if we want, or proxy

        try {
            List<?> notifications = notificationClient.get().uri("/api/notifications").retrieve().bodyToMono(List.class).block();
            stats.put("totalNotifications", notifications != null ? notifications.size() : 0);
        } catch (Exception e) {
            stats.put("totalNotifications", 0);
        }

        stats.put("generatedAt", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/reporting/enrolments-per-course — Proxy to enrolment service
     */
    @GetMapping("/enrolments-per-course")
    public ResponseEntity<?> getEnrolmentsPerCourse() {
        try {
            // Forward directly to enrolment service
            List<?> result = enrolmentClient.get()
                    .uri("/api/enrol/course/all") // Or customize as needed
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Fallback empty list
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET /api/reporting/student/{studentId} — Composes Student + Enrolment + Notification
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentProfile(@PathVariable String studentId) {
        return studentRepository.findByStudentId(studentId).map(student -> {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("student", student);

            try {
                List<?> enrolments = enrolmentClient.get()
                        .uri("/api/enrol/student/" + student.getId())
                        .retrieve()
                        .bodyToMono(List.class)
                        .block();
                profile.put("enrolments", enrolments);
            } catch (Exception e) {
                profile.put("enrolments", List.of());
            }

            try {
                List<?> notifications = notificationClient.get()
                        .uri("/api/notifications/recipient/" + studentId)
                        .retrieve()
                        .bodyToMono(List.class)
                        .block();
                profile.put("notifications", notifications);
            } catch (Exception e) {
                profile.put("notifications", List.of());
            }

            return ResponseEntity.ok(profile);
        }).orElse(ResponseEntity.notFound().build());
    }
}
