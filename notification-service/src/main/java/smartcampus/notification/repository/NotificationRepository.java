package smartcampus.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartcampus.notification.model.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientId(String recipientId);

    List<Notification> findByType(Notification.NotificationType type);

    List<Notification> findByDeliveryStatus(Notification.DeliveryStatus status);
}
