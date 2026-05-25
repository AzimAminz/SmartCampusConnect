package smartcampus.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.net.Socket;

/**
 * TCP Notification Client — satisfies R6 (Distributed Messaging, Producer side).
 *
 * Called by EnrolmentService, RoomBookingSoapService, and LibrarySoapService
 * to push notification events to the NotificationTcpServer (port 9090).
 *
 * Message format (pipe-delimited):
 *   TYPE|RECIPIENT_ID|RECIPIENT_NAME|MESSAGE|RELATED_ENTITY
 */
@Component
public class NotificationClient {

    @Value("${notify.server.host:localhost}")
    private String serverHost;

    @Value("${notify.server.port:9090}")
    private int serverPort;

    /**
     * Sends a notification asynchronously (fire-and-forget).
     * Failure to deliver does NOT affect the calling business operation.
     */
    public void send(String type, String recipientId, String recipientName,
                     String message, String relatedEntity) {
        Thread sender = new Thread(() -> {
            String payload = type + "|" + recipientId + "|" + recipientName
                    + "|" + message + "|" + relatedEntity;
            try (Socket socket = new Socket(serverHost, serverPort);
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                writer.println(payload);
                System.out.println("📤 [TCP-CLIENT] Sent notification: " + payload);
            } catch (Exception e) {
                System.err.println("⚠️ [TCP-CLIENT] Could not send notification: " + e.getMessage());
            }
        });
        sender.setDaemon(true);
        sender.setName("tcp-notification-client");
        sender.start();
    }
}
