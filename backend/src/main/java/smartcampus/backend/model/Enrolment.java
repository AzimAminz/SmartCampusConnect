package smartcampus.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Course Enrolment Service - Enrolment Entity (Join table with metadata)
 * Maps to table: enrolments
 * Satisfies: R3 (SOA), R4 (Service Composition - triggers Notification)
 */
@Entity
@Table(
    name = "enrolments",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_course",
        columnNames = {"student_id", "course_code"}
    )
)
public class Enrolment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_code", nullable = false, length = 20)
    private String courseCode;

    @Column(name = "student_name", length = 100)
    private String studentName; // Denormalized for quick display

    @Column(name = "course_title", length = 150)
    private String courseTitle; // Denormalized for quick display

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EnrolmentStatus status = EnrolmentStatus.ACTIVE;

    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "dropped_at")
    private LocalDateTime droppedAt;

    public enum EnrolmentStatus {
        ACTIVE,    // Currently enrolled
        DROPPED,   // Student dropped the course
        COMPLETED  // Semester ended
    }

    @PrePersist
    public void onCreate() {
        this.enrolledAt = LocalDateTime.now();
    }

    // ---- Constructors ----
    public Enrolment() {}

    public Enrolment(Long studentId, String courseCode, String studentName, String courseTitle) {
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.studentName = studentName;
        this.courseTitle = courseTitle;
        this.status = EnrolmentStatus.ACTIVE;
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public EnrolmentStatus getStatus() { return status; }
    public void setStatus(EnrolmentStatus status) { this.status = status; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDateTime getDroppedAt() { return droppedAt; }
    public void setDroppedAt(LocalDateTime droppedAt) { this.droppedAt = droppedAt; }
}
