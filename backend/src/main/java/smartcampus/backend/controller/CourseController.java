package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.backend.model.Course;
import smartcampus.backend.repository.CourseRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Course REST API — satisfies R3 (SOA), R7 (REST CRUD).
 *
 * Endpoints:
 *   GET    /api/courses              — List all courses
 *   GET    /api/courses/{id}         — Get course by id
 *   GET    /api/courses/code/{code}  — Get course by course code
 *   POST   /api/courses              — Create new course
 *   PUT    /api/courses/{id}         — Update course
 *   DELETE /api/courses/{id}         — Delete course
 */
@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    public ResponseEntity<List<Course>> getAll() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Course> course = courseRepository.findById(id);
        if (course.isPresent()) {
            return ResponseEntity.ok(course.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Course not found: " + id));
    }

    @GetMapping("/code/{courseCode}")
    public ResponseEntity<?> getByCode(@PathVariable String courseCode) {
        Optional<Course> course = courseRepository.findByCourseCode(courseCode);
        if (course.isPresent()) {
            return ResponseEntity.ok(course.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Course not found: " + courseCode));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Course course) {
        if (courseRepository.existsByCourseCode(course.getCourseCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Course code already exists: " + course.getCourseCode()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(courseRepository.save(course));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Course updated) {
        Optional<Course> courseOpt = courseRepository.findById(id);
        if (!courseOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Course not found: " + id));
        }
        Course course = courseOpt.get();
        if (updated.getCourseTitle() != null) course.setCourseTitle(updated.getCourseTitle());
        if (updated.getLecturer() != null)    course.setLecturer(updated.getLecturer());
        if (updated.getFaculty() != null)     course.setFaculty(updated.getFaculty());
        if (updated.getCreditHours() != null) course.setCreditHours(updated.getCreditHours());
        if (updated.getMaxCapacity() != null) course.setMaxCapacity(updated.getMaxCapacity());
        if (updated.getSemester() != null)    course.setSemester(updated.getSemester());
        return ResponseEntity.ok(courseRepository.save(course));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!courseRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Course not found: " + id));
        }
        courseRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Course deleted successfully"));
    }
}
