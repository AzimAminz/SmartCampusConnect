package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.backend.model.Course;
import smartcampus.backend.repository.CourseRepository;
import smartcampus.backend.repository.EnrolmentRepository;
import smartcampus.backend.repository.NotificationRepository;
import smartcampus.backend.repository.StudentRepository;

import java.util.*;

/**
 * Reporting REST API — satisfies R4 (Service Composition).
 *
 * This controller COMPOSES data from multiple services:
 *   - Student Service
 *   - Course Service
 *   - Enrolment Service
 *   - Notification Service
 *
 * Endpoints:
 *   GET /api/reporting/stats                  — System-wide stats
 *   GET /api/reporting/enrolments-per-course  — Enrolment counts per course
 *   GET /api/reporting/student/{studentId}    — Full profile for one student
 */
@RestController
@RequestMapping("/api/reporting")
@CrossOrigin(origins = "*")
public class ReportingController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrolmentRepository enrolmentRepository;
    @Autowired private NotificationRepository notificationRepository;

    /**
     * R4: Composes data from ALL services into a single dashboard response.
     * GET /api/reporting/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalStudents",       studentRepository.count());
        stats.put("totalCourses",        courseRepository.count());
        stats.put("totalEnrolments",     enrolmentRepository.count());
        stats.put("totalNotifications",  notificationRepository.count());

        // Courses with available seats
        List<Course> allCourses = courseRepository.findAll();
        long availableCourses = allCourses.stream().filter(c -> !c.isFull()).count();
        stats.put("coursesWithAvailableSeats", availableCourses);

        stats.put("generatedAt", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(stats);
    }

    /**
     * R4: Composes Enrolment + Course data to show enrolment counts per course.
     * GET /api/reporting/enrolments-per-course
     */
    @GetMapping("/enrolments-per-course")
    public ResponseEntity<List<Map<String, Object>>> getEnrolmentsPerCourse() {
        List<Object[]> raw = enrolmentRepository.countActiveEnrolmentsPerCourse();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : raw) {
            String courseCode = (String) row[0];
            Long count = (Long) row[1];

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("courseCode", courseCode);
            entry.put("activeEnrolments", count);

            // Enrich with course details (service composition)
            courseRepository.findByCourseCode(courseCode).ifPresent(course -> {
                entry.put("courseTitle", course.getCourseTitle());
                entry.put("lecturer", course.getLecturer());
                entry.put("maxCapacity", course.getMaxCapacity());
                entry.put("remainingSeats", course.getRemainingSeats());
            });

            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * R4: Composes Student + Enrolment + Notification data for one student.
     * GET /api/reporting/student/{studentId}   (studentId = B032310001)
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentProfile(@PathVariable String studentId) {
        return studentRepository.findByStudentId(studentId).map(student -> {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("student", student);
            profile.put("enrolments", enrolmentRepository.findByStudentId(student.getId()));
            profile.put("notifications", notificationRepository.findByRecipientId(studentId));
            return ResponseEntity.ok(profile);
        }).orElse(ResponseEntity.notFound().build());
    }
}
