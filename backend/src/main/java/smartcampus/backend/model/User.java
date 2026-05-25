package smartcampus.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Auth Service — User Entity
 * Maps to table: users
 *
 * Covers both STUDENT and ADMIN roles.
 * Login is done using userId only (no password).
 *   - Student: userId = matric no. (e.g. B032310001)
 *   - Admin:   userId = "ADMIN"
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Matric number for students, "ADMIN" for the system admin. */
    @Column(name = "user_id", nullable = false, unique = true, length = 20)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Role {
        STUDENT,
        ADMIN
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Constructors ----
    public User() {}

    public User(String userId, Role role, String fullName) {
        this.userId   = userId;
        this.role     = role;
        this.fullName = fullName;
    }

    // ---- Getters & Setters ----
    public Long getId()                   { return id; }
    public void setId(Long id)            { this.id = id; }

    public String getUserId()             { return userId; }
    public void setUserId(String userId)  { this.userId = userId; }

    public Role getRole()                 { return role; }
    public void setRole(Role role)        { this.role = role; }

    public String getFullName()           { return fullName; }
    public void setFullName(String n)     { this.fullName = n; }

    public LocalDateTime getCreatedAt()   { return createdAt; }
}
