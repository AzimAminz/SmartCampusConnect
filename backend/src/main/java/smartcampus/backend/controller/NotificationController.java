package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.backend.model.Notification;
import smartcampus.backend.repository.NotificationRepository;

import java.util.List;

/**
 * Notification REST API — satisfies R6 (view notification logs).
 *
 * Endpoints:
 *   GET /api/notifications                    — All notifications
 *   GET /api/notifications/recipient/{id}     — By recipient
 *   GET /api/notifications/type/{type}        — By type
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(@PathVariable String recipientId) {
        return ResponseEntity.ok(notificationRepository.findByRecipientId(recipientId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<?> getByType(@PathVariable String type) {
        try {
            Notification.NotificationType notificationType = Notification.NotificationType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(notificationRepository.findByType(notificationType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Invalid type: " + type + ". Valid types: ENROLMENT_SUCCESS, ROOM_BOOKED, etc."));
        }
    }
}
