package smartcampus.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartcampus.backend.model.Course;
import smartcampus.backend.model.Enrolment;
import smartcampus.backend.model.Student;
import smartcampus.backend.repository.CourseRepository;
import smartcampus.backend.repository.EnrolmentRepository;
import smartcampus.backend.repository.StudentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Course Enrolment Service — satisfies:
 *   R3  (SOA — independent enrolment service)
 *   R4  (Service Composition — composes Student + Course + Notification)
 *   R5  (Multithreading — ReentrantLock protects currentCapacity shared state)
 */
@Service
public class EnrolmentService {

    /**
     * R5: ReentrantLock guards the course-capacity check+update.
     * Without this lock, two threads could both read capacity=29/30,
     * both pass the "isFull?" check, and both enrol — exceeding max seats.
     */
    private final ReentrantLock enrolmentLock = new ReentrantLock(true); // fair mode

    @Autowired private EnrolmentRepository enrolmentRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private NotificationClient notificationClient;

    // =========================================================================
    // ENROL
    // =========================================================================
    @Transactional
    public Map<String, Object> enrol(Long studentId, String courseCode) {
        // Validate student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        // R5: Lock before checking/modifying shared mutable state (course capacity)
        enrolmentLock.lock();
        try {
            Course course = courseRepository.findByCourseCode(courseCode)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseCode));

            // Check duplicate
            if (enrolmentRepository.existsByStudentIdAndCourseCode(studentId, courseCode)) {
                return Map.of("success", false, "message", "Student is already enrolled in " + courseCode);
            }

            // Check capacity
            if (course.isFull()) {
                // R4: Compose with Notification service — notify failed enrolment
                notificationClient.send("ENROLMENT_FAILED",
                        student.getStudentId(), student.getName(),
                        "Enrolment failed — " + courseCode + " is full (" + course.getMaxCapacity() + "/" + course.getMaxCapacity() + ")",
                        courseCode);
                return Map.of("success", false, "message", "Course is full: " + courseCode
                        + " (" + course.getCurrentCapacity() + "/" + course.getMaxCapacity() + " seats)");
            }

            // Enrol
            Enrolment enrolment = new Enrolment(studentId, courseCode, student.getName(), course.getCourseTitle());
            enrolmentRepository.save(enrolment);

            // Update capacity
            course.setCurrentCapacity(course.getCurrentCapacity() + 1);
            courseRepository.save(course);

            // R4: Compose with Notification service
            notificationClient.send("ENROLMENT_SUCCESS",
                    student.getStudentId(), student.getName(),
                    "Successfully enrolled in " + courseCode + " — " + course.getCourseTitle(),
                    courseCode);

            return Map.of("success", true,
                    "message", "Enrolled successfully",
                    "enrolmentId", enrolment.getId(),
                    "remainingSeats", course.getRemainingSeats() - 1);
        } finally {
            enrolmentLock.unlock(); // Always release lock
        }
    }

    // =========================================================================
    // DROP
    // =========================================================================
    @Transactional
    public Map<String, Object> drop(Long studentId, String courseCode) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

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

            // Decrease capacity
            Course course = courseRepository.findByCourseCode(courseCode).orElse(null);
            if (course != null && course.getCurrentCapacity() > 0) {
                course.setCurrentCapacity(course.getCurrentCapacity() - 1);
                courseRepository.save(course);
            }

            notificationClient.send("ENROLMENT_DROPPED",
                    student.getStudentId(), student.getName(),
                    "Dropped course: " + courseCode,
                    courseCode);

            return Map.of("success", true, "message", "Course dropped successfully");
        } finally {
            enrolmentLock.unlock();
        }
    }

    // =========================================================================
    // GET ENROLMENTS
    // =========================================================================
    public List<Enrolment> getByStudent(Long studentId) {
        return enrolmentRepository.findByStudentId(studentId);
    }

    public List<Enrolment> getByCourse(String courseCode) {
        return enrolmentRepository.findByCourseCode(courseCode);
    }

    // =========================================================================
    // R5 DEMONSTRATION: Load Test (10 threads, 3 seats)
    // =========================================================================
    public Map<String, Object> runLoadTest(String courseCode) throws Exception {
        // Temporarily set course capacity to 3 for demo
        Course course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseCode));

        course.setMaxCapacity(3);
        course.setCurrentCapacity(0);
        courseRepository.save(course);

        // Reset existing active enrolments for this course
        List<Enrolment> existingEnrolments = enrolmentRepository.findByCourseCode(courseCode);
        for (Enrolment e : existingEnrolments) {
            e.setStatus(Enrolment.EnrolmentStatus.DROPPED);
            enrolmentRepository.save(e);
        }

        // Get all students (use first 10 or repeat students)
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            return Map.of("success", false, "message", "No students in database to run load test");
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final Student s = students.get(i % students.size());
            futures.add(executor.submit(() -> {
                try {
                    Map<String, Object> result = enrol(s.getId(), courseCode);
                    return "Thread[" + Thread.currentThread().getName() + "] "
                            + s.getStudentId() + ": " + result.get("message");
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
        long failCount = results.stream().filter(r -> r.contains("full") || r.contains("already")).count();

        return Map.of(
                "summary", "Load test complete: " + successCount + " enrolled, " + failCount + " rejected",
                "maxSeats", 3,
                "threadCount", threadCount,
                "results", results
        );
    }
}
