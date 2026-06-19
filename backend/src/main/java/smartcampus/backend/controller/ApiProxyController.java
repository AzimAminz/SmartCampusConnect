package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Proxy Controller to transparently route frontend HTTP requests to microservices.
 * Prevents any breaking changes to the Flutter app by keeping API routes identical.
 */
@RestController
@CrossOrigin(origins = "*")
public class ApiProxyController {

    private final WebClient enrolmentClient;
    private final WebClient bookingClient;
    private final WebClient notificationClient;

    public ApiProxyController(
            WebClient.Builder webClientBuilder,
            @Value("${enrolment.service.url:http://enrolment-service:8081}") String enrolmentUrl,
            @Value("${booking.service.url:http://booking-service:8082}") String bookingUrl,
            @Value("${notify.service.url:http://notification-service:8083}") String notifyUrl) {
        this.enrolmentClient = webClientBuilder.baseUrl(enrolmentUrl).build();
        this.bookingClient = webClientBuilder.baseUrl(bookingUrl).build();
        this.notificationClient = webClientBuilder.baseUrl(notifyUrl).build();
    }

    // =========================================================================
    // 1. Enrolment Proxy
    // =========================================================================
    @PostMapping("/api/enrol")
    public ResponseEntity<?> proxyEnrol(@RequestBody Map<String, Object> body) {
        try {
            Object res = enrolmentClient.post()
                    .uri("/api/enrol")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAs(Map.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/enrol/{studentId}/{courseCode}")
    public ResponseEntity<?> proxyDrop(@PathVariable Long studentId, @PathVariable String courseCode) {
        try {
            Object res = enrolmentClient.delete()
                    .uri("/api/enrol/" + studentId + "/" + courseCode)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(res);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAs(Map.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/enrol/load-test/{courseCode}")
    public ResponseEntity<?> proxyLoadTest(@PathVariable String courseCode) {
        try {
            Object res = enrolmentClient.post()
                    .uri("/api/enrol/load-test/" + courseCode)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(res);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAs(Map.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 2. Course Proxy
    // =========================================================================
    @GetMapping("/api/courses")
    public ResponseEntity<?> proxyGetCourses() {
        return proxyGet(enrolmentClient, "/api/courses");
    }

    @GetMapping("/api/courses/{id}")
    public ResponseEntity<?> proxyGetCourseById(@PathVariable Long id) {
        return proxyGet(enrolmentClient, "/api/courses/" + id);
    }

    @GetMapping("/api/courses/code/{courseCode}")
    public ResponseEntity<?> proxyGetCourseByCode(@PathVariable String courseCode) {
        return proxyGet(enrolmentClient, "/api/courses/code/" + courseCode);
    }

    @PostMapping("/api/courses")
    public ResponseEntity<?> proxyCreateCourse(@RequestBody Map<String, Object> body) {
        try {
            Object res = enrolmentClient.post()
                    .uri("/api/courses")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAs(Map.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/courses/{id}")
    public ResponseEntity<?> proxyUpdateCourse(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Object res = enrolmentClient.put()
                    .uri("/api/courses/" + id)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(res);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAs(Map.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/courses/{id}")
    public ResponseEntity<?> proxyDeleteCourse(@PathVariable Long id) {
        try {
            Object res = enrolmentClient.delete()
                    .uri("/api/courses/" + id)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(res);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAs(Map.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 3. Notification Proxy
    // =========================================================================
    @GetMapping("/api/notifications")
    public ResponseEntity<?> proxyGetNotifications() {
        return proxyGet(notificationClient, "/api/notifications");
    }

    @GetMapping("/api/notifications/recipient/{recipientId}")
    public ResponseEntity<?> proxyGetNotificationsByRecipient(@PathVariable String recipientId) {
        return proxyGet(notificationClient, "/api/notifications/recipient/" + recipientId);
    }

    @PostMapping("/api/notifications/read/{id}")
    public ResponseEntity<?> proxyMarkAsRead(@PathVariable Long id) {
        try {
            Object res = notificationClient.post()
                    .uri("/api/notifications/read/" + id)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(res);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAs(Map.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // Helper method for GET requests
    private ResponseEntity<?> proxyGet(WebClient client, String path) {
        try {
            Object res = client.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(res);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAs(Map.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
