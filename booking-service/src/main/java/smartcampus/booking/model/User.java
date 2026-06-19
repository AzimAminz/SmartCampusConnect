package smartcampus.booking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User entity — shared auth model for booking-service.
 * Reads from db_booking.users table.
 * Session tokens validated by the SOAP service.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 20)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Role { STUDENT, ADMIN }

    @PrePersist
    public void onCreate() { this.createdAt = LocalDateTime.now(); }

    public User() {}

    public User(String userId, Role role, String fullName) {
        this.userId = userId; this.role = role; this.fullName = fullName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String n) { this.fullName = n; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
