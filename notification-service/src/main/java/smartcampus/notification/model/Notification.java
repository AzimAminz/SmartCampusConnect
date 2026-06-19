package smartcampus.notification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification Entity — maps to table: notifications in db_notification
 * Satisfies: R6 (Distributed Messaging - logs all async events consumed by TCP server)
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
    private String recipientId;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "related_entity", length = 50)
    private String relatedEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    @Column(name = "channel", length = 20)
    private String channel = "TCP_SOCKET";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        ENROLMENT_SUCCESS,
        ENROLMENT_FAILED,
        ENROLMENT_DROPPED,
        ROOM_BOOKED,
        ROOM_CANCELLED,
        BOOK_BORROWED,
        BOOK_RETURNED,
        BOOK_OVERDUE,
        PAYMENT_DUE,
        SYSTEM_ALERT
    }

    public enum DeliveryStatus {
        SENT, FAILED, PENDING
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
