package smartcampus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartcampus.backend.model.Course;

import java.util.Optional;

/**
 * Course Service - Data Access Layer
 * Satisfies: R3 (SOA), R5 (shared mutable state - currentCapacity)
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    boolean existsByCourseCode(String courseCode);
}
