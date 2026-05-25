package smartcampus.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import smartcampus.backend.model.Course;
import smartcampus.backend.model.Student;
import smartcampus.backend.model.User;
import smartcampus.backend.model.Book;
import smartcampus.backend.repository.CourseRepository;
import smartcampus.backend.repository.StudentRepository;
import smartcampus.backend.repository.UserRepository;
import smartcampus.backend.repository.BookRepository;

/**
 * Seeds sample data into the database on first startup.
 * Only inserts if tables are empty (idempotent).
 * Satisfies: R2 (distributed data initialisation), R9 (demo readiness).
 */
@Component
public class DatabaseInitializer {

    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;

    @PostConstruct
    public void seed() {
        seedStudents();
        seedCourses();
        seedUsers();  // Must run AFTER seedStudents so names are available
        seedBooks();
    }

    private void seedStudents() {
        if (studentRepository.count() > 0) return;

        studentRepository.save(new Student(
                "Muhammad Azim bin Aminudin",
                "azim@student.utem.edu.my",
                "B032310001", "Bachelor of Computer Science (Hons)", "FTMK", "1", 3.80, "0123456781"));

        studentRepository.save(new Student(
                "Nur Aisyah binti Ahmad",
                "aisyah@student.utem.edu.my",
                "B032310002", "Bachelor of Computer Science (Hons)", "FTMK", "1", 3.65, "0123456782"));

        studentRepository.save(new Student(
                "Muhammad Hafiz bin Razali",
                "hafiz@student.utem.edu.my",
                "B032310003", "Bachelor of Computer Science (Hons)", "FTMK", "1", 3.72, "0123456783"));

        studentRepository.save(new Student(
                "Siti Nurhaliza binti Zainal",
                "nurhaliza@student.utem.edu.my",
                "B032310004", "Bachelor of Computer Science (Hons)", "FTMK", "1", 3.55, "0123456784"));

        studentRepository.save(new Student(
                "Ahmad Faris bin Othman",
                "faris@student.utem.edu.my",
                "B032310005", "Bachelor of Computer Science (Hons)", "FTMK", "1", 3.90, "0123456785"));

        System.out.println("✅ [DB-INIT] Seeded 5 sample students");
    }

    private void seedCourses() {
        if (courseRepository.count() > 0) return;

        courseRepository.save(new Course(
                "BITP3123", "Distributed Application Development",
                "Dr. Ramli", "FTMK", 3, 30, "2024/2025 SEM 1"));

        courseRepository.save(new Course(
                "BITP3143", "Mobile Application Development",
                "Dr. Farahwahida", "FTMK", 3, 30, "2024/2025 SEM 1"));

        courseRepository.save(new Course(
                "BITM3073", "Network Security",
                "Dr. Azuan", "FTMK", 3, 25, "2024/2025 SEM 1"));

        courseRepository.save(new Course(
                "BITU3033", "Software Engineering",
                "Dr. Ruzaini", "FTMK", 3, 35, "2024/2025 SEM 1"));

        courseRepository.save(new Course(
                "BITP3113", "Web Application Development",
                "Dr. Shahrol", "FTMK", 3, 30, "2024/2025 SEM 1"));

        System.out.println("✅ [DB-INIT] Seeded 5 sample courses");
    }

    /**
     * Seeds the users table:
     *   - 1 ADMIN account (userId = "ADMIN")
     *   - 1 STUDENT account per seeded student (userId = matric no.)
     *
     * These accounts are used by the /api/auth/login endpoint.
     * No password required — login by userId only.
     */
    private void seedUsers() {
        if (userRepository.count() > 0) return;

        // Admin account
        userRepository.save(new User("ADMIN", User.Role.ADMIN, "System Administrator"));

        // Sync student accounts from students table
        studentRepository.findAll().forEach(student ->
            userRepository.save(new User(
                    student.getStudentId(),
                    User.Role.STUDENT,
                    student.getName()))
        );

        System.out.println("✅ [DB-INIT] Seeded users: 1 admin + " + studentRepository.count() + " students");
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) return;

        bookRepository.save(new Book("9780134685991", "Effective Java", "Joshua Bloch", "Software Engineering"));
        bookRepository.save(new Book("9780132350884", "Clean Code", "Robert C. Martin", "Software Engineering"));
        bookRepository.save(new Book("9780135957059", "The Pragmatic Programmer", "David Thomas", "Software Engineering"));
        bookRepository.save(new Book("9780321356680", "Design Patterns", "Erich Gamma", "Computer Science"));
        bookRepository.save(new Book("9780134092669", "Introduction to Algorithms", "Thomas H. Cormen", "Algorithms"));

        System.out.println("✅ [DB-INIT] Seeded 5 sample books");
    }
}
