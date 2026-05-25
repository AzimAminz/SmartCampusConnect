package smartcampus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartcampus.backend.model.Notification;

import java.util.List;

/**
 * Notification Service - Data Access Layer
 * Satisfies: R6 (Distributed Messaging - persists all TCP events)
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientId(String recipientId);

    List<Notification> findByType(Notification.NotificationType type);

    List<Notification> findByDeliveryStatus(Notification.DeliveryStatus status);
}
