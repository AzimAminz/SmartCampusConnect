package smartcampus.notification.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import smartcampus.notification.model.Notification;
import smartcampus.notification.repository.NotificationRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP Notification Server — R6 (Distributed Messaging, Consumer Side).
 *
 * Listens on port 9090. Receives pipe-delimited messages from other services:
 *   TYPE|RECIPIENT_ID|RECIPIENT_NAME|MESSAGE|RELATED_ENTITY
 *
 * Each received message is persisted to db_notification.notifications table.
 * Runs as a daemon thread so it does not block Spring Boot startup.
 */
@Component
public class NotificationTcpServer {

    /** Ensures the TCP server only binds once per JVM (guard against devtools hot-restart) */
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    @Value("${notify.server.port:9090}")
    private int serverPort;

    @Autowired
    private NotificationRepository notificationRepository;

    @PostConstruct
    public void startServer() {
        if (!STARTED.compareAndSet(false, true)) {
            System.out.println("ℹ️  [TCP-SERVER] Already running, skipping re-bind on port " + serverPort);
            return;
        }

        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
                System.out.println("✅ [TCP-SERVER] Notification server started on port " + serverPort);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (Exception e) {
                System.err.println("❌ [TCP-SERVER] Error: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.setName("tcp-notification-server");
        serverThread.start();
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && !line.isBlank()) {
                System.out.println("📨 [TCP-SERVER] Received: " + line);
                parseAndPersist(line);
            }
        } catch (Exception e) {
            System.err.println("⚠️  [TCP-SERVER] Client error: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Parses pipe-delimited message format:
     *   TYPE|RECIPIENT_ID|RECIPIENT_NAME|MESSAGE|RELATED_ENTITY
     */
    private void parseAndPersist(String raw) {
        try {
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 5) {
                System.err.println("⚠️  [TCP-SERVER] Malformed message: " + raw);
                return;
            }

            Notification.NotificationType type;
            try {
                type = Notification.NotificationType.valueOf(parts[0].trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                type = Notification.NotificationType.SYSTEM_ALERT;
            }

            Notification notification = new Notification(
                    type,
                    parts[1].trim(),
                    parts[2].trim(),
                    parts[3].trim(),
                    parts[4].trim()
            );
            notification.setDeliveryStatus(Notification.DeliveryStatus.SENT);
            notification.setChannel("TCP_SOCKET");
            notificationRepository.save(notification);
            System.out.println("💾 [TCP-SERVER] Persisted notification id=" + notification.getId());

        } catch (Exception e) {
            System.err.println("❌ [TCP-SERVER] Failed to persist: " + e.getMessage());
        }
    }
}
