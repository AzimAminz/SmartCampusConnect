package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import smartcampus.backend.model.User;
import smartcampus.backend.model.UserSession;
import smartcampus.backend.repository.StudentRepository;
import smartcampus.backend.repository.UserSessionRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dashboard REST API — Student Personal View (Microservice-compatible HTTP Aggregator)
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired private UserSessionRepository userSessionRepository;
    @Autowired private StudentRepository studentRepository;

    private final WebClient enrolmentClient;
    private final WebClient bookingClient;
    private final WebClient notificationClient;

    public DashboardController(
            WebClient.Builder webClientBuilder,
            @Value("${enrolment.service.url:http://enrolment-service:8081}") String enrolmentUrl,
            @Value("${booking.service.url:http://booking-service:8082}") String bookingUrl,
            @Value("${notify.service.url:http://notification-service:8083}") String notifyUrl) {
        this.enrolmentClient = webClientBuilder.baseUrl(enrolmentUrl).build();
        this.bookingClient = webClientBuilder.baseUrl(bookingUrl).build();
        this.notificationClient = webClientBuilder.baseUrl(notifyUrl).build();
    }

    @GetMapping
    public ResponseEntity<?> getDashboard(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {

        // --- Auth check ---
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing X-Auth-Token header. Please login first."));
        }

        Optional<UserSession> sessionOpt = userSessionRepository.findByToken(token);
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token. Please login again."));
        }

        UserSession session = sessionOpt.get();
        if (session.isExpired()) {
            userSessionRepository.deleteByToken(token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please login again."));
        }

        // --- Admin Dashboard ---
        if (session.getRole() == User.Role.ADMIN) {
            Map<String, Object> adminDash = new LinkedHashMap<>();
            adminDash.put("role", "ADMIN");
            adminDash.put("fullName", session.getFullName());
            adminDash.put("totalStudents", studentRepository.count());

            // Aggregate statistics from other services
            try {
                // We can fetch courses count, enrolments count, etc., using aggregate calls.
                // Let's call reporting/stats internally or do direct GET requests.
                // For direct requests, we retrieve listings:
                List<?> courses = enrolmentClient.get().uri("/api/courses").retrieve().bodyToMono(List.class).block();
                adminDash.put("totalCourses", courses != null ? courses.size() : 0);
            } catch (Exception e) {
                adminDash.put("totalCourses", 0);
            }

            try {
                // Retrieve bookings from booking-service REST API
                List<?> bookings = bookingClient.get().uri("/api/bookings").retrieve().bodyToMono(List.class).block();
                adminDash.put("totalRoomBookings", bookings != null ? bookings.size() : 0);
                adminDash.put("allRoomBookings", bookings);
            } catch (Exception e) {
                adminDash.put("totalRoomBookings", 0);
                adminDash.put("allRoomBookings", List.of());
            }

            try {
                List<?> loans = bookingClient.get().uri("/api/loans").retrieve().bodyToMono(List.class).block();
                adminDash.put("totalBookLoans", loans != null ? loans.size() : 0);
                adminDash.put("allBookLoans", loans);
            } catch (Exception e) {
                adminDash.put("totalBookLoans", 0);
                adminDash.put("allBookLoans", List.of());
            }

            try {
                List<?> notifications = notificationClient.get().uri("/api/notifications").retrieve().bodyToMono(List.class).block();
                adminDash.put("totalNotifications", notifications != null ? notifications.size() : 0);
            } catch (Exception e) {
                adminDash.put("totalNotifications", 0);
            }

            return ResponseEntity.ok(adminDash);
        }

        // --- Student Dashboard ---
        String studentId = session.getUserId(); // e.g. B032310001

        Optional<smartcampus.backend.model.Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (!studentOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Student profile not found for userId: " + studentId));
        }

        smartcampus.backend.model.Student student = studentOpt.get();
        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("role", "STUDENT");
        dash.put("profile", student);

        // Fetch Enrolments (enrolment-service)
        try {
            List<?> enrolments = enrolmentClient.get()
                    .uri("/api/enrol/student/" + student.getId())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            dash.put("enrolments", enrolments);
        } catch (Exception e) {
            dash.put("enrolments", List.of());
        }

        // Fetch Room Bookings (booking-service)
        try {
            List<?> roomBookings = bookingClient.get()
                    .uri("/api/bookings/student/" + studentId)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            dash.put("roomBookings", roomBookings);
        } catch (Exception e) {
            dash.put("roomBookings", List.of());
        }

        // Fetch Book Loans (booking-service)
        try {
            List<?> bookLoans = bookingClient.get()
                    .uri("/api/loans/student/" + studentId)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            dash.put("bookLoans", bookLoans);
        } catch (Exception e) {
            dash.put("bookLoans", List.of());
        }

        // Fetch Notifications (notification-service)
        try {
            List<?> notifications = notificationClient.get()
                    .uri("/api/notifications/recipient/" + studentId)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            dash.put("notifications", notifications);
        } catch (Exception e) {
            dash.put("notifications", List.of());
        }

        return ResponseEntity.ok(dash);
    }
}
