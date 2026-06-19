package smartcampus.enrolment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartcampus.enrolment.model.Course;
import smartcampus.enrolment.model.Enrolment;
import smartcampus.enrolment.repository.CourseRepository;
import smartcampus.enrolment.repository.EnrolmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Enrolment Service — satisfies:
 *   R3  (SOA)
 *   R4  (Service Composition — composes Student via HTTP + Notification via HTTP)
 *   R5  (Multithreading — ReentrantLock protects currentCapacity shared state)
 *
 * Key change from monolith:
 *   StudentRepository → StudentClient (HTTP call to student-service:8080)
 *   NotificationClient TCP → NotificationClient HTTP (to notification-service:8083)
 */
@Service
public class EnrolmentService {

    /**
     * R5: ReentrantLock guards course-capacity check+update.
     */
    private final ReentrantLock enrolmentLock = new ReentrantLock(true); // fair mode

    @Autowired private EnrolmentRepository enrolmentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private NotificationClient notificationClient;
    @Autowired private StudentClient studentClient;

    // =========================================================================
    // ENROL
    // =========================================================================
    @Transactional
    public Map<String, Object> enrol(Long studentId, String courseCode) {
        // Validate student via HTTP call to student-service (R4: Service Composition)
        Map<String, Object> student = studentClient.findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }
        String studentMatric = student.getOrDefault("studentId", studentId.toString()).toString();
        String studentName   = student.getOrDefault("name", "Unknown").toString();

        // R5: Lock before checking/modifying shared mutable state (course capacity)
        enrolmentLock.lock();
        try {
            Course course = courseRepository.findByCourseCode(courseCode)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseCode));

            // Check duplicate
            java.util.Optional<Enrolment> existingOpt =
                    enrolmentRepository.findByStudentIdAndCourseCode(studentId, courseCode);
            if (existingOpt.isPresent()) {
                Enrolment existing = existingOpt.get();
                if (existing.getStatus() == Enrolment.EnrolmentStatus.ACTIVE) {
                    return Map.of("success", false, "message", "Student is already enrolled in " + courseCode);
                }
            }

            // Check capacity
            if (course.isFull()) {
                notificationClient.send("ENROLMENT_FAILED",
                        studentMatric, studentName,
                        "Enrolment failed — " + courseCode + " is full (" + course.getMaxCapacity() + "/" + course.getMaxCapacity() + ")",
                        courseCode);
                return Map.of("success", false, "message", "Course is full: " + courseCode
                        + " (" + course.getCurrentCapacity() + "/" + course.getMaxCapacity() + " seats)");
            }

            // Enrol (reactivate if DROPPED, else create new)
            Enrolment enrolment;
            if (existingOpt.isPresent()) {
                enrolment = existingOpt.get();
                enrolment.setStatus(Enrolment.EnrolmentStatus.ACTIVE);
                enrolment.setDroppedAt(null);
                enrolment.setEnrolledAt(java.time.LocalDateTime.now());
            } else {
                enrolment = new Enrolment(studentId, courseCode, studentName, course.getCourseTitle());
            }
            enrolmentRepository.save(enrolment);

            // Update capacity
            course.setCurrentCapacity(course.getCurrentCapacity() + 1);
            courseRepository.save(course);

            // R4: notify
            notificationClient.send("ENROLMENT_SUCCESS",
                    studentMatric, studentName,
                    "Successfully enrolled in " + courseCode + " — " + course.getCourseTitle(),
                    courseCode);

            return Map.of("success", true,
                    "message", "Enrolled successfully",
                    "enrolmentId", enrolment.getId(),
                    "remainingSeats", course.getRemainingSeats() - 1);
        } finally {
            enrolmentLock.unlock();
        }
    }

    // =========================================================================
    // DROP
    // =========================================================================
    @Transactional
    public Map<String, Object> drop(Long studentId, String courseCode) {
        Map<String, Object> student = studentClient.findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }
        String studentMatric = student.getOrDefault("studentId", studentId.toString()).toString();
        String studentName   = student.getOrDefault("name", "Unknown").toString();

        enrolmentLock.lock();
        try {
            Enrolment enrolment = enrolmentRepository.findByStudentIdAndCourseCode(studentId, courseCode)
                    .orElseThrow(() -> new IllegalArgumentException("Enrolment not found"));

            if (enrolment.getStatus() == Enrolment.EnrolmentStatus.DROPPED) {
                return Map.of("success", false, "message", "Already dropped");
            }

            enrolment.setStatus(Enrolment.EnrolmentStatus.DROPPED);
            enrolment.setDroppedAt(java.time.LocalDateTime.now());
            enrolmentRepository.save(enrolment);

            Course course = courseRepository.findByCourseCode(courseCode).orElse(null);
            if (course != null && course.getCurrentCapacity() > 0) {
                course.setCurrentCapacity(course.getCurrentCapacity() - 1);
                courseRepository.save(course);
            }

            notificationClient.send("ENROLMENT_DROPPED",
                    studentMatric, studentName,
                    "Dropped course: " + courseCode, courseCode);

            return Map.of("success", true, "message", "Course dropped successfully");
        } finally {
            enrolmentLock.unlock();
        }
    }

    // =========================================================================
    // QUERIES
    // =========================================================================
    public List<Enrolment> getByStudent(Long studentId) {
        return enrolmentRepository.findByStudentId(studentId);
    }

    public List<Enrolment> getByCourse(String courseCode) {
        return enrolmentRepository.findByCourseCode(courseCode);
    }

    // =========================================================================
    // R5 DEMO LOAD TEST
    // =========================================================================
    public Map<String, Object> runLoadTest(String courseCode) throws Exception {
        Course course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseCode));

        course.setMaxCapacity(3);
        course.setCurrentCapacity(0);
        courseRepository.save(course);

        // Reset existing enrolments
        List<Enrolment> existingEnrolments = enrolmentRepository.findByCourseCode(courseCode);
        for (Enrolment e : existingEnrolments) {
            e.setStatus(Enrolment.EnrolmentStatus.DROPPED);
            enrolmentRepository.save(e);
        }

        // Use fixed student IDs 1-10 for load test
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            final long sId = i;
            futures.add(executor.submit(() -> {
                try {
                    Map<String, Object> result = enrol(sId, courseCode);
                    return "Thread[" + Thread.currentThread().getName() + "] "
                            + "student_" + sId + ": " + result.get("message");
                } catch (Exception ex) {
                    return "Thread[" + Thread.currentThread().getName() + "] ERROR: " + ex.getMessage();
                }
            }));
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        List<String> results = new ArrayList<>();
        for (Future<String> f : futures) {
            results.add(f.get());
        }

        long successCount = results.stream().filter(r -> r.contains("successfully")).count();
        long failCount    = results.stream().filter(r -> r.contains("full") || r.contains("already")).count();

        return Map.of(
                "summary", "Load test complete: " + successCount + " enrolled, " + failCount + " rejected",
                "maxSeats", 3,
                "threadCount", threadCount,
                "results", results
        );
    }
}
