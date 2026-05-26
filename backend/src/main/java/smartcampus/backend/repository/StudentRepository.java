package smartcampus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import smartcampus.backend.model.Student;

import java.util.List;
import java.util.Optional;

/**
 * Student Profile Service - Data Access Layer
 * Satisfies: R3 (SOA), R7 (REST endpoints)
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByEmail(String email);

    boolean existsByStudentId(String studentId);

    boolean existsByEmail(String email);

    /**
     * Find all studentIds that start with a given prefix (used for auto-increment).
     * Example prefix: "B0326" → matches "B032610001", "B032610002", etc.
     */
    @Query("SELECT s.studentId FROM Student s WHERE s.studentId LIKE :prefix%")
    List<String> findStudentIdsByPrefix(@Param("prefix") String prefix);
}
