package smartcampus.booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Notification HTTP Client — used by booking-service to call notification-service.
 *
 * Replaces the original TCP socket client.
 * Fire-and-forget: failures do NOT affect the booking business operation.
 *
 * Target endpoint: POST http://notification-service:8083/api/notify
 */
@Component
public class NotificationClient {

    private final WebClient webClient;

    public NotificationClient(@Value("${notify.service.url:http://notification-service:8083}") String notifyUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(notifyUrl)
                .build();
    }

    /**
     * Sends a notification asynchronously (fire-and-forget via HTTP to notification-service).
     */
    public void send(String type, String recipientId, String recipientName,
                     String message, String relatedEntity) {
        Map<String, String> body = Map.of(
                "type",          type,
                "recipientId",   recipientId,
                "recipientName", recipientName,
                "message",       message,
                "relatedEntity", relatedEntity
        );

        webClient.post()
                .uri("/api/notify")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(resp -> System.out.println("📤 [NOTIFY-CLIENT] Sent → " + type + " for " + recipientId))
                .doOnError(err -> System.err.println("⚠️  [NOTIFY-CLIENT] Failed: " + err.getMessage()))
                .subscribe(); // Non-blocking fire-and-forget
    }
}
