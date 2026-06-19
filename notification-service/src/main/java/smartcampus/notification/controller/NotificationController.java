package smartcampus.notification.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.notification.model.Notification;
import smartcampus.notification.repository.NotificationRepository;

import java.util.List;
import java.util.Map;

/**
 * Notification REST Controller
 *
 * PUBLIC endpoints (used by frontend and student-service aggregator):
 *   GET  /api/notifications                  — All notifications
 *   GET  /api/notifications/recipient/{id}   — By recipient
 *   GET  /api/notifications/type/{type}      — By type
 *
 * INTERNAL endpoint (called by other microservices via Docker network):
 *   POST /api/notify  — Receive and persist a notification via HTTP
 *                        Body: { type, recipientId, recipientName, message, relatedEntity }
 */
@RestController
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    // -------------------------------------------------------------------------
    // PUBLIC: GET all notifications
    // -------------------------------------------------------------------------
    @GetMapping("/api/notifications")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    // -------------------------------------------------------------------------
    // PUBLIC: GET by recipient
    // -------------------------------------------------------------------------
    @GetMapping("/api/notifications/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(@PathVariable String recipientId) {
        return ResponseEntity.ok(notificationRepository.findByRecipientId(recipientId));
    }

    // -------------------------------------------------------------------------
    // PUBLIC: GET by type
    // -------------------------------------------------------------------------
    @GetMapping("/api/notifications/type/{type}")
    public ResponseEntity<?> getByType(@PathVariable String type) {
        try {
            Notification.NotificationType notificationType =
                    Notification.NotificationType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(notificationRepository.findByType(notificationType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid type: " + type));
        }
    }

    // -------------------------------------------------------------------------
    // INTERNAL: POST /api/notify — used by booking-service and enrolment-service
    // Body: { "type": "ROOM_BOOKED", "recipientId": "B032310001",
    //         "recipientName": "Ali", "message": "...", "relatedEntity": "BK-123" }
    // -------------------------------------------------------------------------
    @PostMapping("/api/notify")
    public ResponseEntity<?> receiveNotification(@RequestBody Map<String, String> body) {
        try {
            Notification.NotificationType type;
            try {
                type = Notification.NotificationType.valueOf(body.getOrDefault("type", "SYSTEM_ALERT").toUpperCase());
            } catch (IllegalArgumentException e) {
                type = Notification.NotificationType.SYSTEM_ALERT;
            }

            Notification notification = new Notification(
                    type,
                    body.getOrDefault("recipientId", "UNKNOWN"),
                    body.getOrDefault("recipientName", "Unknown"),
                    body.getOrDefault("message", ""),
                    body.getOrDefault("relatedEntity", "")
            );
            notification.setChannel("HTTP_INTERNAL");
            notificationRepository.save(notification);

            System.out.println("📬 [NOTIFY-HTTP] Persisted from inter-service call: " + notification.getId());
            return ResponseEntity.ok(Map.of("notificationId", notification.getId(), "status", "SAVED"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to persist notification: " + e.getMessage()));
        }
    }
}
