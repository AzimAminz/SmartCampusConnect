package smartcampus.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification Service - Notification Log Entity
 * Maps to table: notifications
 * Satisfies: R6 (Distributed Messaging - logs all async events consumed by the TCP server)
 *            R4 (Service Composition - produced by Enrolment & Booking services)
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "recipient_id", nullable = false, length = 20)
    private String recipientId;  // Student ID or staff ID

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "related_entity", length = 50)
    private String relatedEntity; // e.g. course code, booking reference

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    @Column(name = "channel", length = 20)
    private String channel = "TCP_SOCKET"; // Transport mechanism used

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        ENROLMENT_SUCCESS,   // Student enrolled in a course
        ENROLMENT_FAILED,    // Enrolment failed (e.g. full)
        ENROLMENT_DROPPED,   // Student dropped a course
        ROOM_BOOKED,         // Room booking confirmed
        ROOM_CANCELLED,      // Room booking cancelled
        BOOK_BORROWED,       // Library book borrowed
        BOOK_RETURNED,       // Book returned
        BOOK_OVERDUE,        // Book overdue alert
        PAYMENT_DUE,         // Tuition fee reminder
        SYSTEM_ALERT         // Generic system notification
    }

    public enum DeliveryStatus {
        SENT,    // Successfully pushed to TCP server
        FAILED,  // Failed to deliver (connection refused)
        PENDING  // Queued for retry
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Constructors ----
    public Notification() {}

    public Notification(NotificationType type, String recipientId, String recipientName,
                        String message, String relatedEntity) {
        this.type = type;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.message = message;
        this.relatedEntity = relatedEntity;
        this.deliveryStatus = DeliveryStatus.SENT;
        this.channel = "TCP_SOCKET";
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRelatedEntity() { return relatedEntity; }
    public void setRelatedEntity(String relatedEntity) { this.relatedEntity = relatedEntity; }

    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
