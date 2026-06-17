package smartcampus.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import smartcampus.backend.model.Student;
import smartcampus.backend.model.User;
import smartcampus.backend.repository.StudentRepository;
import smartcampus.backend.repository.UserRepository;
import smartcampus.backend.repository.UserSessionRepository;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Student Management Service
 *
 * Satisfies:
 *   R3  — SOA (independent service layer)
 *   R5  — Multithreading: ReentrantLock prevents concurrent clients (web, mobile,
 *          desktop) from generating duplicate Student IDs.
 *   R7  — REST CRUD business logic
 *
 * Student ID Format:  [P][FF][YY][SC][NNN]
 *   P   — Programme code : D (Diploma), B (Bachelor/Degree), P (Prasiswazah)
 *   FF  — Faculty code   : 03-FTMK  04-FTKM  05-FTKEK  06-FTKIP  07-FTKE  08-FPTT
 *   YY  — Year (2 digits): e.g. 26 for 2026
 *   SC  — Sem code       : semester × 10  →  1→10, 2→20, 3→30 …
 *   NNN — Auto-increment sequence (001-999)
 *
 * Example: B032610001  — Bachelor, FTMK, 2026, Sem 1, first student
 */
@Service
public class StudentService {

    // ── Faculty → 2-digit code mapping ─────────────────────────────────────────
    private static final Map<String, String> FACULTY_CODES = Map.of(
            "FTMK",  "03",   // Teknologi Maklumat & Komunikasi
            "FTKM",  "04",   // Kejuruteraan Mekanikal
            "FTKEK", "05",   // Kejuruteraan Elektrik & Elektronik
            "FTKIP", "06",   // Teknologi Kreatif & Warisan
            "FTKE",  "07",   // Kejuruteraan
            "FPTT",  "08"    // Pengurusan Teknologi & Keusahawanan
    );

    // ── Programme → 1-character code mapping ───────────────────────────────────
    private static final Map<String, String> PROGRAMME_CODES = Map.of(
            "D", "D",   // Diploma
            "B", "B",   // Bachelor / Degree
            "P", "P"    // Prasiswazah (Postgrad / Master / PhD)
    );

    /**
     * Fair ReentrantLock — ensures FIFO ordering so no client starves.
     * This lock is held only during the read-then-write ID generation window,
     * preventing any two concurrent requests from computing the same next sequence.
     */
    private final ReentrantLock idGenerationLock = new ReentrantLock(true);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Creates a new student with a server-generated, thread-safe Student ID.
     *
     * The entire "find max sequence → assign next → save" is wrapped in a
     * ReentrantLock so that concurrent requests from web, mobile, and desktop
     * clients can never produce duplicate IDs.
     *
     * @param name          Student full name
     * @param email         Unique email address
     * @param programmeCode "D", "B", or "P"
     * @param faculty       Faculty abbreviation, e.g. "FTMK"
     * @param semester      Current semester as string, e.g. "1", "2"
     * @param programme     Human-readable programme name (optional)
     * @param gpa           GPA (nullable)
     * @param phoneNumber   Phone (nullable)
     * @return Saved {@link Student} entity (includes generated studentId)
     */
    @Transactional
    public Student createStudent(
            String name,
            String email,
            String programmeCode,
            String faculty,
            String semester,
            String programme,
            Double gpa,
            String phoneNumber) {

        // ── Validate inputs ──────────────────────────────────────────────────
        if (name == null || name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        if (email == null || email.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        if (!PROGRAMME_CODES.containsKey(programmeCode.toUpperCase()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid programmeCode. Use D, B, or P.");
        if (!FACULTY_CODES.containsKey(faculty.toUpperCase()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown faculty: " + faculty + ". Valid: " + FACULTY_CODES.keySet());

        String resolvedEmail = email.trim().toLowerCase();
        if (!"auto".equals(resolvedEmail)) {
            if (studentRepository.existsByEmail(resolvedEmail))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Email already registered: " + resolvedEmail);
        }

        // ── Thread-safe ID generation + save ────────────────────────────────
        idGenerationLock.lock();
        try {
            String generatedId = generateNextStudentId(
                    programmeCode.toUpperCase(), faculty.toUpperCase(), semester);

            if ("auto".equals(resolvedEmail)) {
                resolvedEmail = generatedId.toLowerCase() + "@student.utem.edu.my";
                if (studentRepository.existsByEmail(resolvedEmail)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Email already registered: " + resolvedEmail);
                }
            }

            Student student = new Student();
            student.setStudentId(generatedId);
            student.setName(name.trim());
            student.setEmail(resolvedEmail);
            student.setProgramme(programme != null ? programme.trim() : "");
            student.setFaculty(faculty.toUpperCase());
            student.setSemester(semester != null ? semester.trim() : "1");
            student.setGpa(gpa);
            student.setPhoneNumber(phoneNumber != null ? phoneNumber.trim() : null);

            Student saved = studentRepository.save(student);

            // Automatically create a corresponding User account for authentication
            User user = new User(generatedId, User.Role.STUDENT, name.trim());
            userRepository.save(user);

            return saved;

        } finally {
            idGenerationLock.unlock();
        }
    }

    /**
     * Updates an existing student's details, maintaining sync with their User account.
     */
    @Transactional
    public Student updateStudent(
            Long id,
            String name,
            String email,
            String programme,
            String faculty,
            String semester,
            Double gpa,
            String phoneNumber) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with id: " + id));

        String oldStudentId = student.getStudentId();

        if (name != null) {
            student.setName(name.trim());
            // Sync User fullName
            userRepository.findByUserId(oldStudentId).ifPresent(user -> {
                user.setFullName(name.trim());
                userRepository.save(user);
            });
        }
        if (email != null) {
            student.setEmail(email.trim().toLowerCase());
        }
        if (programme != null) student.setProgramme(programme.trim());
        if (faculty != null) student.setFaculty(faculty.toUpperCase());
        if (semester != null) student.setSemester(semester.trim());
        if (gpa != null) student.setGpa(gpa);
        if (phoneNumber != null) student.setPhoneNumber(phoneNumber.trim());

        return studentRepository.save(student);
    }

    /**
     * Deletes a student and cascades deletion to their User account and active login sessions.
     */
    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with id: " + id));

        String studentId = student.getStudentId();

        // 1. Delete student profile
        studentRepository.delete(student);

        // 2. Delete corresponding user account
        userRepository.findByUserId(studentId).ifPresent(user -> userRepository.delete(user));

        // 3. Purge active login sessions
        userSessionRepository.deleteByUserId(studentId);
    }


    // ── Private helpers ─────────────────────────────────────────────────────────

    /**
     * Builds the next Student ID for a given combination.
     * Called only while holding {@code idGenerationLock}.
     *
     * Prefix = [P][FF][YY][SC]  e.g. "B032610"
     * Queries DB for all IDs with that prefix, finds the max 3-digit sequence,
     * and returns prefix + (max + 1) zero-padded to 3 digits.
     */
    private String generateNextStudentId(String programmeCode, String faculty, String semester) {
        String facultyCode = FACULTY_CODES.get(faculty);
        String year        = String.valueOf(LocalDate.now().getYear()).substring(2); // "26"

        // Derive intake semester code: semester × 10 (1→"10", 2→"20", 3→"30" …)
        int semNum;
        try {
            semNum = Math.max(1, Integer.parseInt(semester.trim()));
        } catch (NumberFormatException e) {
            semNum = 1;
        }
        String semCode = String.format("%02d", semNum * 10);

        String prefix = programmeCode + facultyCode + year + semCode; // 7 chars

        // Scan existing IDs with this prefix to find the current max sequence
        List<String> existing = studentRepository.findStudentIdsByPrefix(prefix);
        int maxSeq = 0;
        for (String sid : existing) {
            if (sid.length() >= prefix.length() + 3) {
                try {
                    int seq = Integer.parseInt(
                            sid.substring(prefix.length(), prefix.length() + 3));
                    if (seq > maxSeq) maxSeq = seq;
                } catch (NumberFormatException ignored) {}
            }
        }

        int nextSeq = maxSeq + 1;
        if (nextSeq > 999) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Maximum capacity (999) reached for student group: " + prefix);
        }

        return String.format("%s%03d", prefix, nextSeq); // e.g. "B032610001"
    }
}
