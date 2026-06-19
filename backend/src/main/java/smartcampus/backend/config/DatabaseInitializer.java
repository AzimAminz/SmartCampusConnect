package smartcampus.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import smartcampus.backend.model.Student;
import smartcampus.backend.model.User;
import smartcampus.backend.repository.StudentRepository;
import smartcampus.backend.repository.UserRepository;

/**
 * Seeds sample data into the database on first startup for student-service.
 */
@Component
public class DatabaseInitializer {

    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;

    @PostConstruct
    public void seed() {
        seedStudents();
        seedUsers();  // Must run AFTER seedStudents so names are available
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
}
