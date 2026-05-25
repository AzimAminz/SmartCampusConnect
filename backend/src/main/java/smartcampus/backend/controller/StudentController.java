package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.backend.model.Student;
import smartcampus.backend.repository.StudentRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Student Profile REST API — satisfies R3 (SOA), R7 (REST CRUD).
 *
 * Endpoints:
 *   GET    /api/students           — List all students
 *   GET    /api/students/{id}      — Get student by DB id
 *   GET    /api/students/sid/{sid} — Get student by studentId (e.g. B032310001)
 *   POST   /api/students           — Create new student
 *   PUT    /api/students/{id}      — Update student
 *   DELETE /api/students/{id}      — Delete student
 */
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    // GET /api/students
    @GetMapping
    public ResponseEntity<List<Student>> getAll() {
        return ResponseEntity.ok(studentRepository.findAll());
    }

    // GET /api/students/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isPresent()) {
            return ResponseEntity.ok(student.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Student not found with id: " + id));
    }

    // GET /api/students/sid/{studentId}
    @GetMapping("/sid/{studentId}")
    public ResponseEntity<?> getByStudentId(@PathVariable String studentId) {
        Optional<Student> student = studentRepository.findByStudentId(studentId);
        if (student.isPresent()) {
            return ResponseEntity.ok(student.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Student not found: " + studentId));
    }

    // POST /api/students
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Student student) {
        if (studentRepository.existsByStudentId(student.getStudentId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Student ID already exists: " + student.getStudentId()));
        }
        if (studentRepository.existsByEmail(student.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered: " + student.getEmail()));
        }
        Student saved = studentRepository.save(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT /api/students/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Student updated) {
        Optional<Student> studentOpt = studentRepository.findById(id);
        if (!studentOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Student not found: " + id));
        }
        Student student = studentOpt.get();
        if (updated.getName() != null)        student.setName(updated.getName());
        if (updated.getEmail() != null)       student.setEmail(updated.getEmail());
        if (updated.getProgramme() != null)   student.setProgramme(updated.getProgramme());
        if (updated.getFaculty() != null)     student.setFaculty(updated.getFaculty());
        if (updated.getSemester() != null)    student.setSemester(updated.getSemester());
        if (updated.getGpa() != null)         student.setGpa(updated.getGpa());
        if (updated.getPhoneNumber() != null) student.setPhoneNumber(updated.getPhoneNumber());
        return ResponseEntity.ok(studentRepository.save(student));
    }

    // DELETE /api/students/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!studentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Student not found: " + id));
        }
        studentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Student deleted successfully"));
    }
}
