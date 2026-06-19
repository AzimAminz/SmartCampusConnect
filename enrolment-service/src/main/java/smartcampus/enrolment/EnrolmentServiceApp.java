package smartcampus.enrolment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Enrolment Microservice — R3 (SOA), R4 (Service Composition), R5 (Multithreading)
 *
 * Exposes:
 *   - Port 8081 : REST API (courses, enrolments, load test)
 *
 * Calls:
 *   - student-service:8080  — to validate student exists
 *   - notification-service:8083 — to send enrolment notifications
 */
@SpringBootApplication
public class EnrolmentServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(EnrolmentServiceApp.class, args);
    }
}
