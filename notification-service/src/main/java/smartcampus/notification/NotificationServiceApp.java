package smartcampus.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Microservice — R6 (Distributed Messaging)
 *
 * Exposes:
 *   - Port 8083 : REST API  (GET /api/notifications, POST /api/notify [internal])
 *   - Port 9090 : TCP Socket Server (producer-consumer pattern)
 */
@SpringBootApplication
public class NotificationServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApp.class, args);
    }
}
