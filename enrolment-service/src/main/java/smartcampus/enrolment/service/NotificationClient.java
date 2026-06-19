package smartcampus.enrolment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Notification HTTP Client for enrolment-service.
 * Calls POST /api/notify on notification-service (HTTP, fire-and-forget).
 */
@Component
public class NotificationClient {

    private final WebClient webClient;

    public NotificationClient(@Value("${notify.service.url:http://notification-service:8083}") String notifyUrl) {
        this.webClient = WebClient.builder().baseUrl(notifyUrl).build();
    }

    public void send(String type, String recipientId, String recipientName,
                     String message, String relatedEntity) {
        Map<String, String> body = Map.of(
                "type", type,
                "recipientId", recipientId,
                "recipientName", recipientName,
                "message", message,
                "relatedEntity", relatedEntity
        );

        webClient.post()
                .uri("/api/notify")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(r -> System.out.println("📤 [ENROL-NOTIFY] Sent → " + type + " for " + recipientId))
                .doOnError(e -> System.err.println("⚠️  [ENROL-NOTIFY] Failed: " + e.getMessage()))
                .subscribe();
    }
}
