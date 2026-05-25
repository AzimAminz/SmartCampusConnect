package smartcampus.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Course Enrolment Service - Course Entity
 * Maps to table: courses
 * Satisfies: R3, R5 (capacity is the shared mutable state protected by ReentrantLock)
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", nullable = false, unique = true, length = 20)
    private String courseCode;  // e.g. CS301, BITP3123

    @Column(name = "course_title", nullable = false, length = 150)
    private String courseTitle;

    @Column(length = 100)
    private String lecturer;

    @Column(length = 50)
    private String faculty;

    @Column(name = "credit_hours", nullable = false)
    private Integer creditHours = 3;

    /**
     * Current number of enrolled students.
     * This is the SHARED MUTABLE STATE protected by ReentrantLock in EnrolmentService (R5).
     */
    @Column(name = "current_capacity", nullable = false)
    private Integer currentCapacity = 0;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity = 30;

    @Column(length = 20)
    private String semester;  // e.g. "2024/2025 SEM 1"

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Constructors ----
    public Course() {}

    public Course(String courseCode, String courseTitle, String lecturer, String faculty,
                  Integer creditHours, Integer maxCapacity, String semester) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.lecturer = lecturer;
        this.faculty = faculty;
        this.creditHours = creditHours;
        this.maxCapacity = maxCapacity;
        this.semester = semester;
    }

    // ---- Business Helper ----
    public boolean isFull() {
        return this.currentCapacity >= this.maxCapacity;
    }

    public int getRemainingSeats() {
        return this.maxCapacity - this.currentCapacity;
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getLecturer() { return lecturer; }
    public void setLecturer(String lecturer) { this.lecturer = lecturer; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public Integer getCreditHours() { return creditHours; }
    public void setCreditHours(Integer creditHours) { this.creditHours = creditHours; }

    public Integer getCurrentCapacity() { return currentCapacity; }
    public void setCurrentCapacity(Integer currentCapacity) { this.currentCapacity = currentCapacity; }

    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
