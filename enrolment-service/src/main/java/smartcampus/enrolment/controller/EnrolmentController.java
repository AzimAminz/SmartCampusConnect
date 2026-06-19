package smartcampus.enrolment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.enrolment.model.Enrolment;
import smartcampus.enrolment.service.EnrolmentService;

import java.util.List;
import java.util.Map;

/**
 * Enrolment REST Controller
 *
 * Endpoints:
 *   POST   /api/enrol                          — Enrol student
 *   DELETE /api/enrol/{studentId}/{courseCode} — Drop course
 *   GET    /api/enrol/student/{studentId}      — Get by student
 *   GET    /api/enrol/course/{courseCode}      — Get by course
 *   POST   /api/enrol/load-test/{courseCode}   — R5 concurrency demo
 */
@RestController
@RequestMapping("/api/enrol")
@CrossOrigin(origins = "*")
public class EnrolmentController {

    @Autowired
    private EnrolmentService enrolmentService;

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

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Enrolment>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrolmentService.getByStudent(studentId));
    }

    @GetMapping("/course/{courseCode}")
    public ResponseEntity<List<Enrolment>> getByCourse(@PathVariable String courseCode) {
        return ResponseEntity.ok(enrolmentService.getByCourse(courseCode));
    }

    @PostMapping("/load-test/{courseCode}")
    public ResponseEntity<?> runLoadTest(@PathVariable String courseCode) {
        try {
            return ResponseEntity.ok(enrolmentService.runLoadTest(courseCode));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
