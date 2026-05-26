package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.backend.model.User;
import smartcampus.backend.model.UserSession;
import smartcampus.backend.model.RoomBooking;
import smartcampus.backend.model.BookLoan;
import smartcampus.backend.repository.*;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dashboard REST API — Student Personal View
 *
 * GET /api/dashboard — Returns all personal data for the logged-in student
 *   Requires header: X-Auth-Token
 *
 * Composes data from:
 *   - Student profile
 *   - Active enrolments
 *   - Room bookings
 *   - Book loans
 *   - Notifications
 *
 * Satisfies R4 (Service Composition from login perspective)
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired private UserSessionRepository userSessionRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private EnrolmentRepository enrolmentRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private RoomBookingRepository roomBookingRepository;
    @Autowired private BookLoanRepository bookLoanRepository;

    /**
     * GET /api/dashboard
     * Returns personalised dashboard for the logged-in student.
     * Admin gets a summary of all system counts.
     */
    @GetMapping
    public ResponseEntity<?> getDashboard(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {

        // --- Auth check ---
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing X-Auth-Token header. Please login first."));
        }

        Optional<UserSession> sessionOpt = userSessionRepository.findByToken(token);
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token. Please login again."));
        }

        UserSession session = sessionOpt.get();
        if (session.isExpired()) {
            userSessionRepository.deleteByToken(token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please login again."));
        }

        // --- Admin Dashboard ---
        if (session.getRole() == User.Role.ADMIN) {
            Map<String, Object> adminDash = new LinkedHashMap<>();
            adminDash.put("role", "ADMIN");
            adminDash.put("fullName", session.getFullName());
            adminDash.put("totalStudents",      studentRepository.count());
            adminDash.put("totalEnrolments",    enrolmentRepository.count());
            adminDash.put("totalRoomBookings",  roomBookingRepository.countByStatus(RoomBooking.BookingStatus.CONFIRMED));
            adminDash.put("totalBookLoans",     bookLoanRepository.countByStatus(BookLoan.LoanStatus.BORROWED) + bookLoanRepository.countByStatus(BookLoan.LoanStatus.OVERDUE));

            adminDash.put("totalNotifications", notificationRepository.count());
            adminDash.put("allBookLoans",       bookLoanRepository.findAll());
            adminDash.put("allRoomBookings",    roomBookingRepository.findAll());
            return ResponseEntity.ok(adminDash);
        }

        // --- Student Dashboard ---
        String studentId = session.getUserId();  // e.g. B032310001

        Optional<smartcampus.backend.model.Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (!studentOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Student profile not found for userId: " + studentId
                            + ". Please ensure your student record exists in the system."));
        }

        smartcampus.backend.model.Student student = studentOpt.get();
        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("role",          "STUDENT");
        dash.put("profile",       student);
        dash.put("enrolments",    enrolmentRepository.findByStudentId(student.getId()));
        dash.put("roomBookings",  roomBookingRepository.findByStudentId(studentId));
        dash.put("bookLoans",     bookLoanRepository.findByStudentId(studentId));
        dash.put("notifications", notificationRepository.findByRecipientId(studentId));
        return ResponseEntity.ok(dash);
    }
}
