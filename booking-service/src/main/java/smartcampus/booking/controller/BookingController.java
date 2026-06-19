package smartcampus.booking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.booking.model.Book;
import smartcampus.booking.model.BookLoan;
import smartcampus.booking.model.RoomBooking;
import smartcampus.booking.repository.BookLoanRepository;
import smartcampus.booking.repository.BookRepository;
import smartcampus.booking.repository.RoomBookingRepository;

import java.util.List;

/**
 * REST Endpoint for booking-service (Port 8082).
 * Used internally by student-service (DashboardController) to retrieve booking details.
 */
@RestController
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired private BookRepository bookRepository;
    @Autowired private BookLoanRepository bookLoanRepository;
    @Autowired private RoomBookingRepository roomBookingRepository;

    // --- Book endpoints ---
    @GetMapping("/api/books")
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookRepository.findAll());
    }

    // --- Room Booking endpoints ---
    @GetMapping("/api/bookings")
    public ResponseEntity<List<RoomBooking>> getAllBookings() {
        return ResponseEntity.ok(roomBookingRepository.findAll());
    }

    @GetMapping("/api/bookings/student/{studentId}")
    public ResponseEntity<List<RoomBooking>> getBookingsByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(roomBookingRepository.findByStudentId(studentId));
    }

    // --- Book Loan endpoints ---
    @GetMapping("/api/loans")
    public ResponseEntity<List<BookLoan>> getAllLoans() {
        return ResponseEntity.ok(bookLoanRepository.findAll());
    }

    @GetMapping("/api/loans/student/{studentId}")
    public ResponseEntity<List<BookLoan>> getLoansByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(bookLoanRepository.findByStudentId(studentId));
    }
}
