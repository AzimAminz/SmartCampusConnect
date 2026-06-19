package smartcampus.enrolment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import smartcampus.enrolment.model.Enrolment;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {

    List<Enrolment> findByStudentId(Long studentId);

    List<Enrolment> findByCourseCode(String courseCode);

    Optional<Enrolment> findByStudentIdAndCourseCode(Long studentId, String courseCode);

    boolean existsByStudentIdAndCourseCode(Long studentId, String courseCode);

    @Query("SELECT e.courseCode, COUNT(e) FROM Enrolment e WHERE e.status = 'ACTIVE' GROUP BY e.courseCode")
    List<Object[]> countActiveEnrolmentsPerCourse();

    long countByCourseCodeAndStatus(String courseCode, Enrolment.EnrolmentStatus status);
}
