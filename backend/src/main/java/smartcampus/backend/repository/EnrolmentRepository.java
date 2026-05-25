package smartcampus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import smartcampus.backend.model.Enrolment;

import java.util.List;
import java.util.Optional;

/**
 * Enrolment Service - Data Access Layer
 * Satisfies: R3 (SOA), R4 (Service Composition), R5 (multithreaded capacity check)
 */
@Repository
public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {

    List<Enrolment> findByStudentId(Long studentId);

    List<Enrolment> findByCourseCode(String courseCode);

    Optional<Enrolment> findByStudentIdAndCourseCode(Long studentId, String courseCode);

    boolean existsByStudentIdAndCourseCode(Long studentId, String courseCode);

    /** Count how many active enrolments exist per course (for reporting) */
    @Query("SELECT e.courseCode, COUNT(e) FROM Enrolment e WHERE e.status = 'ACTIVE' GROUP BY e.courseCode")
    List<Object[]> countActiveEnrolmentsPerCourse();

    long countByCourseCodeAndStatus(String courseCode, Enrolment.EnrolmentStatus status);
}
