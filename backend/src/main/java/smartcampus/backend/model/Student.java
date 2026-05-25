package smartcampus.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Student Profile Service - Core Entity
 * Maps to table: students
 * Satisfies: R3 (SOA - independent service), R7 (REST CRUD endpoints)
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "student_id", nullable = false, unique = true, length = 20)
    private String studentId; // e.g. B032310001

    @Column(length = 100)
    private String programme;  // e.g. Bachelor of Computer Science

    @Column(length = 20)
    private String faculty;    // e.g. FTMK

    @Column(nullable = false, length = 10)
    private String semester = "1"; // Current semester

    @Column(precision = 4, scale = 2)
    private Double gpa;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- Constructors ----
    public Student() {}

    public Student(String name, String email, String studentId, String programme, String faculty, String semester, Double gpa, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.studentId = studentId;
        this.programme = programme;
        this.faculty = faculty;
        this.semester = semester;
        this.gpa = gpa;
        this.phoneNumber = phoneNumber;
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getProgramme() { return programme; }
    public void setProgramme(String programme) { this.programme = programme; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
