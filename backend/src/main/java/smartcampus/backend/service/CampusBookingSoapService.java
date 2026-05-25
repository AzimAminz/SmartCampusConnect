package smartcampus.backend.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import smartcampus.backend.model.Book;
import smartcampus.backend.model.BookLoan;
import smartcampus.backend.model.RoomBooking;
import smartcampus.backend.model.User;
import smartcampus.backend.model.UserSession;
import smartcampus.backend.repository.BookLoanRepository;
import smartcampus.backend.repository.BookRepository;
import smartcampus.backend.repository.RoomBookingRepository;
import smartcampus.backend.repository.UserSessionRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;


/**
 * SOAP Web Service — satisfies R8 (SOAP/WSDL).
 *
 * Exposed at: http://0.0.0.0:8085/ws/booking?wsdl
 *
 * Operations:
 *  1. bookRoom(studentId, studentName, roomName, slot, date, purpose) → bookingRef
 *  2. checkAvailability(roomName, slot, date) → boolean
 *  3. cancelBooking(bookingRef) → boolean
 *  4. borrowBook(studentId, studentName, isbn, title) → loanRef
 *  5. returnBook(loanRef) → boolean
 *
 * SOAP Fault is thrown when a room is already booked (double-booking).
 */
@WebService(
    serviceName = "SmartCampusBookingService",
    portName   = "BookingPort",
    targetNamespace = "http://service.smartcampus/"
)
@Component
public class CampusBookingSoapService {

    // Inject via setter (JAX-WS creates its own instances, Spring wires in)
    private static RoomBookingRepository roomBookingRepository;
    private static BookLoanRepository bookLoanRepository;
    private static BookRepository bookRepository;
    private static UserSessionRepository userSessionRepository;
    private static NotificationClient notificationClient;

    @Autowired
    void setRoomBookingRepository(RoomBookingRepository r) {
        CampusBookingSoapService.roomBookingRepository = r;
    }

    @Autowired
    void setBookLoanRepository(BookLoanRepository b) {
        CampusBookingSoapService.bookLoanRepository = b;
    }

    @Autowired
    void setBookRepository(BookRepository b) {
        CampusBookingSoapService.bookRepository = b;
    }

    @Autowired
    void setUserSessionRepository(UserSessionRepository u) {
        CampusBookingSoapService.userSessionRepository = u;
    }

    @Autowired
    void setNotificationClient(NotificationClient n) {
        CampusBookingSoapService.notificationClient = n;
    }

    // =========================================================================
    // Operation 1: bookRoom
    // =========================================================================
    @WebMethod(operationName = "bookRoom")
    public String bookRoom(
            @WebParam(name = "studentId")   String studentId,
            @WebParam(name = "studentName") String studentName,
            @WebParam(name = "roomName")    String roomName,
            @WebParam(name = "slot")        String slot,
            @WebParam(name = "date")        String date,   // ISO: yyyy-MM-dd
            @WebParam(name = "purpose")     String purpose) {

        LocalDate bookingDate = parseDate(date);

        // R8 SOAP Fault: throw on double-booking
        boolean alreadyBooked = roomBookingRepository.existsByRoomNameAndSlotAndBookingDateAndStatus(
                roomName, slot, bookingDate, RoomBooking.BookingStatus.CONFIRMED);
        if (alreadyBooked) {
            throw new WebServiceException(
                    "SOAP_FAULT: Room '" + roomName + "' slot '" + slot
                    + "' on " + date + " is already booked. Please choose another slot.");
        }

        String reference = generateRef("BK");
        RoomBooking booking = new RoomBooking(reference, studentId, studentName, roomName, slot, bookingDate, purpose);
        roomBookingRepository.save(booking);

        notificationClient.send("ROOM_BOOKED", studentId, studentName,
                "Room booking confirmed: " + roomName + " " + slot + " on " + date, reference);

        return reference;
    }

    // =========================================================================
    // Operation 2: checkAvailability
    // =========================================================================
    @WebMethod(operationName = "checkAvailability")
    public boolean checkAvailability(
            @WebParam(name = "roomName") String roomName,
            @WebParam(name = "slot")     String slot,
            @WebParam(name = "date")     String date) {

        LocalDate bookingDate = parseDate(date);
        boolean booked = roomBookingRepository.existsByRoomNameAndSlotAndBookingDateAndStatus(
                roomName, slot, bookingDate, RoomBooking.BookingStatus.CONFIRMED);
        return !booked; // true = available
    }

    // =========================================================================
    // Operation 3: cancelBooking
    // =========================================================================
    @WebMethod(operationName = "cancelBooking")
    public boolean cancelBooking(@WebParam(name = "bookingRef") String bookingRef) {
        return roomBookingRepository.findByBookingReference(bookingRef).map(booking -> {
            if (booking.getStatus() == RoomBooking.BookingStatus.CANCELLED) {
                throw new WebServiceException("SOAP_FAULT: Booking " + bookingRef + " is already cancelled.");
            }
            booking.setStatus(RoomBooking.BookingStatus.CANCELLED);
            booking.setCancelledAt(java.time.LocalDateTime.now());
            roomBookingRepository.save(booking);

            notificationClient.send("ROOM_CANCELLED", booking.getStudentId(), booking.getStudentName(),
                    "Room booking cancelled: " + booking.getRoomName() + " " + booking.getSlot(), bookingRef);
            return true;
        }).orElseThrow(() -> new WebServiceException("SOAP_FAULT: Booking reference not found: " + bookingRef));
    }

    // =========================================================================
    // Operation 4: borrowBook (ADMIN only)
    // =========================================================================
    @WebMethod(operationName = "borrowBook")
    public String borrowBook(
            @WebParam(name = "token")       String token,
            @WebParam(name = "studentId")   String studentId,
            @WebParam(name = "studentName") String studentName,
            @WebParam(name = "isbn")        String isbn,
            @WebParam(name = "dueDate")     String dueDateStr) { // Admin sets custom due date

        verifyAdmin(token);

        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new WebServiceException("SOAP_FAULT: Book with ISBN " + isbn + " not found in the catalog."));

        if (book.getStatus() == Book.BookStatus.BORROWED) {
            throw new WebServiceException("SOAP_FAULT: Book '" + book.getTitle() + "' (ISBN: " + isbn + ") is currently borrowed by another student.");
        }

        LocalDate dueDate = parseDate(dueDateStr);
        if (dueDate.isBefore(LocalDate.now())) {
            throw new WebServiceException("SOAP_FAULT: Due date cannot be in the past.");
        }

        // Generate loan reference
        String reference = generateRef("LN");
        LocalDate today = LocalDate.now();

        // Create Loan
        BookLoan loan = new BookLoan(reference, studentId, studentName, isbn, book.getTitle(), today, dueDate);
        bookLoanRepository.save(loan);

        // Update book status in master catalog
        book.setStatus(Book.BookStatus.BORROWED);
        bookRepository.save(book);

        notificationClient.send("BOOK_BORROWED", studentId, studentName,
                "Book borrowed: '" + book.getTitle() + "' (ISBN: " + isbn + "). Due: " + dueDateStr, reference);

        return reference;
    }

    // =========================================================================
    // Operation 5: returnBook (ADMIN only)
    // =========================================================================
    @WebMethod(operationName = "returnBook")
    public boolean returnBook(
            @WebParam(name = "token")   String token,
            @WebParam(name = "loanRef") String loanRef) {

        verifyAdmin(token);

        return bookLoanRepository.findByLoanReference(loanRef).map(loan -> {
            if (loan.getStatus() == BookLoan.LoanStatus.RETURNED) {
                throw new WebServiceException("SOAP_FAULT: Loan " + loanRef + " is already returned.");
            }
            LocalDate today = LocalDate.now();
            loan.setReturnDate(today);
            loan.setStatus(BookLoan.LoanStatus.RETURNED);

            // Calculate late fine: RM1 per day overdue
            if (loan.getDueDate().isBefore(today)) {
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), today);
                loan.setFineAmount(daysLate * 1.0);
            }
            bookLoanRepository.save(loan);

            // Set book back to AVAILABLE in catalog
            bookRepository.findByIsbn(loan.getBookIsbn()).ifPresent(book -> {
                book.setStatus(Book.BookStatus.AVAILABLE);
                bookRepository.save(book);
            });

            notificationClient.send("BOOK_RETURNED", loan.getStudentId(), loan.getStudentName(),
                    "Book returned: '" + loan.getBookTitle() + "'. Fine: RM" + loan.getFineAmount(), loanRef);
            return true;
        }).orElseThrow(() -> new WebServiceException("SOAP_FAULT: Loan reference not found: " + loanRef));
    }

    // =========================================================================
    // Operation 6: addBook (ADMIN only)
    // =========================================================================
    @WebMethod(operationName = "addBook")
    public boolean addBook(
            @WebParam(name = "token")    String token,
            @WebParam(name = "isbn")     String isbn,
            @WebParam(name = "title")    String title,
            @WebParam(name = "author")   String author,
            @WebParam(name = "category") String category) {

        verifyAdmin(token);

        if (bookRepository.findByIsbn(isbn).isPresent()) {
            throw new WebServiceException("SOAP_FAULT: Book with ISBN " + isbn + " already exists in the catalog.");
        }

        Book book = new Book(isbn, title, author, category);
        bookRepository.save(book);

        notificationClient.send("SYSTEM_ALERT", "ADMIN", "Library Manager",
                "New book added: '" + title + "' (ISBN: " + isbn + ") by " + author, isbn);

        return true;
    }

    // =========================================================================
    // Operation 7: searchBooks (OPEN)
    // =========================================================================
    @WebMethod(operationName = "searchBooks")
    public List<Book> searchBooks(@WebParam(name = "query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return bookRepository.findAll();
        }
        return bookRepository.searchBooks(query);
    }

    // =========================================================================
    // Operation 8: getBookLoanHistory (ADMIN only)
    // =========================================================================
    @WebMethod(operationName = "getBookLoanHistory")
    public List<BookLoan> getBookLoanHistory(
            @WebParam(name = "token") String token,
            @WebParam(name = "isbn")  String isbn) {

        verifyAdmin(token);

        return bookLoanRepository.findByBookIsbn(isbn);
    }

    // =========================================================================
    // Operation 9: getStudentLoanHistory (STUDENT or ADMIN)
    // =========================================================================
    @WebMethod(operationName = "getStudentLoanHistory")
    public List<BookLoan> getStudentLoanHistory(
            @WebParam(name = "token")     String token,
            @WebParam(name = "studentId") String studentId) {

        UserSession session = verifyUser(token);

        // Security check: If student, they can only search their own history
        if (session.getRole() == User.Role.STUDENT && !session.getUserId().equalsIgnoreCase(studentId)) {
            throw new WebServiceException("SOAP_FAULT: Access Denied. Students can only view their own borrowing history.");
        }

        return bookLoanRepository.findByStudentId(studentId);
    }

    // =========================================================================
    // Security Helpers
    // =========================================================================
    private UserSession verifyUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new WebServiceException("SOAP_FAULT: Authentication token is required.");
        }
        UserSession session = userSessionRepository.findByToken(token)
                .orElseThrow(() -> new WebServiceException("SOAP_FAULT: Invalid authentication token. Please login first."));
        if (session.isExpired()) {
            throw new WebServiceException("SOAP_FAULT: Session expired. Please login again.");
        }
        return session;
    }

    private UserSession verifyAdmin(String token) {
        UserSession session = verifyUser(token);
        if (session.getRole() != User.Role.ADMIN) {
            throw new WebServiceException("SOAP_FAULT: Access Denied. Administrator role required for this operation.");
        }
        return session;
    }

    // =========================================================================
    // Helper
    // =========================================================================
    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new WebServiceException("SOAP_FAULT: Invalid date format. Use yyyy-MM-dd. Got: " + date);
        }
    }

    private String generateRef(String prefix) {
        return prefix + "-" + LocalDate.now().toString().replace("-", "")
                + "-" + String.format("%03d", (int)(Math.random() * 1000));
    }
}
