package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.backend.model.Student;
import smartcampus.backend.repository.StudentRepository;
import smartcampus.backend.service.StudentService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private StudentService studentService;

    // GET /api/students - List all
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(studentRepository.findAll());
    }

    // GET /api/students/id/{id} - Get by database ID (internal use)
    @GetMapping("/id/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isPresent()) {
            return ResponseEntity.ok(student.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Student not found with id: " + id));
    }

    // GET /api/students/{studentId} - Get by studentId (public use)
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getByStudentId(@PathVariable String studentId) {
        Optional<Student> student = studentRepository.findByStudentId(studentId);
        if (student.isPresent()) {
            return ResponseEntity.ok(student.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Student not found: " + studentId));
    }

    // POST /api/students - CREATE
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateStudentRequest request) {
        try {
            Student saved = studentService.createStudent(
                request.getName(),
                request.getEmail(),
                request.getProgrammeCode(),
                request.getFaculty(),
                request.getSemester(),
                request.getProgramme(),
                request.getGpa(),
                request.getPhoneNumber()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/students/id/{id} - UPDATE by database ID
    @PutMapping("/{id}")
    public ResponseEntity<?> updateById(
            @PathVariable Long id, 
            @RequestBody UpdateStudentRequest request) {
        try {
            Student updated = studentService.updateStudent(
                id,
                request.getName(),
                request.getEmail(),
                request.getProgramme(),
                request.getFaculty(),
                request.getSemester(),
                request.getGpa(),
                request.getPhoneNumber()
            );
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    

    // DELETE /api/students/id/{id} - DELETE by database ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        try {
            studentService.deleteStudent(id);
            return ResponseEntity.ok(Map.of("message", "Student deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    
  
    // Request DTOs
    static class CreateStudentRequest {
        private String name;
        private String email;
        private String programmeCode;
        private String faculty;
        private String semester;
        private String programme;
        private Double gpa;
        private String phoneNumber;
        
        // Getters
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getProgrammeCode() { return programmeCode; }
        public String getFaculty() { return faculty; }
        public String getSemester() { return semester; }
        public String getProgramme() { return programme; }
        public Double getGpa() { return gpa; }
        public String getPhoneNumber() { return phoneNumber; }
        
        // Setters
        public void setName(String name) { this.name = name; }
        public void setEmail(String email) { this.email = email; }
        public void setProgrammeCode(String programmeCode) { this.programmeCode = programmeCode; }
        public void setFaculty(String faculty) { this.faculty = faculty; }
        public void setSemester(String semester) { this.semester = semester; }
        public void setProgramme(String programme) { this.programme = programme; }
        public void setGpa(Double gpa) { this.gpa = gpa; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }
    
    static class UpdateStudentRequest {
        private String name;
        private String email;
        private String programme;
        private String faculty;
        private String semester;
        private Double gpa;
        private String phoneNumber;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getProgramme() { return programme; }
        public void setProgramme(String programme) { this.programme = programme; }
        
        public String getFaculty() { return faculty; }
        public void setFaculty(String faculty) { this.faculty = faculty; }
        
        public String getSemester() { return semester; }
        public void setSemester(String semester) { this.semester = semester; }
        
        public Double getGpa() { return gpa; }
        public void setGpa(Double gpa) { this.gpa = gpa; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }
}