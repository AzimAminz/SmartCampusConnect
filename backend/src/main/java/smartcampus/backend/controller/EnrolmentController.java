package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.backend.model.Enrolment;
import smartcampus.backend.service.EnrolmentService;

import java.util.List;
import java.util.Map;

/**
 * Course Enrolment REST API — satisfies R3 (SOA), R4 (Service Composition), R5 (Multithreading).
 *
 * Endpoints:
 *   POST /api/enrol                          — Enrol a student in a course
 *   DELETE /api/enrol/{studentId}/{course}   — Drop a course
 *   GET  /api/enrol/student/{studentId}      — Get all enrolments for student
 *   GET  /api/enrol/course/{courseCode}      — Get all enrolments for course
 *   POST /api/enrol/load-test/{courseCode}   — R5 demo: 10 threads, 3 seats
 */
@RestController
@RequestMapping("/api/enrol")
@CrossOrigin(origins = "*")
public class EnrolmentController {

    @Autowired
    private EnrolmentService enrolmentService;

    // POST /api/enrol
    // Body: { "studentId": 1, "courseCode": "BITP3123" }
    @PostMapping
    public ResponseEntity<?> enrol(@RequestBody Map<String, Object> body) {
        try {
            Long studentId = Long.valueOf(body.get("studentId").toString());
            String courseCode = body.get("courseCode").toString();
            Map<String, Object> result = enrolmentService.enrol(studentId, courseCode);
            boolean success = (boolean) result.get("success");
            return ResponseEntity.status(success ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/enrol/{studentId}/{courseCode}
    @DeleteMapping("/{studentId}/{courseCode}")
    public ResponseEntity<?> drop(@PathVariable Long studentId, @PathVariable String courseCode) {
        try {
            Map<String, Object> result = enrolmentService.drop(studentId, courseCode);
            boolean success = (boolean) result.get("success");
            return ResponseEntity.status(success ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/enrol/student/{studentId}
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Enrolment>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrolmentService.getByStudent(studentId));
    }

    // GET /api/enrol/course/{courseCode}
    @GetMapping("/course/{courseCode}")
    public ResponseEntity<List<Enrolment>> getByCourse(@PathVariable String courseCode) {
        return ResponseEntity.ok(enrolmentService.getByCourse(courseCode));
    }

    /**
     * R5 Multithreading Demo:
     * Resets course to 3 seats, then fires 10 concurrent threads.
     * Expected: exactly 3 succeed, 7 are rejected ("course is full").
     *
     * POST /api/enrol/load-test/{courseCode}
     */
    @PostMapping("/load-test/{courseCode}")
    public ResponseEntity<?> runLoadTest(@PathVariable String courseCode) {
        try {
            Map<String, Object> result = enrolmentService.runLoadTest(courseCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
