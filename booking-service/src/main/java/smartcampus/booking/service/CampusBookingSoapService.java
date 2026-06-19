package smartcampus.booking.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import smartcampus.booking.model.Book;
import smartcampus.booking.model.BookLoan;
import smartcampus.booking.model.RoomBooking;
import smartcampus.booking.model.User;
import smartcampus.booking.model.UserSession;
import smartcampus.booking.repository.BookLoanRepository;
import smartcampus.booking.repository.BookRepository;
import smartcampus.booking.repository.RoomBookingRepository;
import smartcampus.booking.repository.UserSessionRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * SOAP Web Service — R8 (SOAP/WSDL).
 *
 * Exposed at: http://0.0.0.0:8085/ws/booking?wsdl
 *
 * Notification is now sent via HTTP to notification-service (port 8083)
 * instead of TCP socket (port 9090) — same behavior, microservice transport.
 */
@WebService(
    serviceName = "SmartCampusBookingService",
    portName    = "BookingPort",
    targetNamespace = "http://service.smartcampus/"
)
@Component
public class CampusBookingSoapService {

    private static RoomBookingRepository roomBookingRepository;
    private static BookLoanRepository bookLoanRepository;
    private static BookRepository bookRepository;
    private static AuthClient authClient;
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
    void setAuthClient(AuthClient a) {
        CampusBookingSoapService.authClient = a;
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
            @WebParam(name = "date")        String date,
            @WebParam(name = "purpose")     String purpose) {

        LocalDate bookingDate = parseDate(date);

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
        return !booked;
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
            @WebParam(name = "dueDate")     String dueDateStr) {

        verifyAdmin(token);

        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new WebServiceException("SOAP_FAULT: Book with ISBN " + isbn + " not found."));

        if (book.getStatus() == Book.BookStatus.BORROWED) {
            throw new WebServiceException("SOAP_FAULT: Book '" + book.getTitle() + "' is currently borrowed.");
        }

        LocalDate dueDate = parseDate(dueDateStr);
        if (dueDate.isBefore(LocalDate.now())) {
            throw new WebServiceException("SOAP_FAULT: Due date cannot be in the past.");
        }

        String reference = generateRef("LN");
        BookLoan loan = new BookLoan(reference, studentId, studentName, isbn, book.getTitle(), LocalDate.now(), dueDate);
        bookLoanRepository.save(loan);

        book.setStatus(Book.BookStatus.BORROWED);
        bookRepository.save(book);

        notificationClient.send("BOOK_BORROWED", studentId, studentName,
                "Book borrowed: '" + book.getTitle() + "'. Due: " + dueDateStr, reference);

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

            if (loan.getDueDate().isBefore(today)) {
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), today);
                loan.setFineAmount(daysLate * 1.0);
            }
            bookLoanRepository.save(loan);

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
            throw new WebServiceException("SOAP_FAULT: Book with ISBN " + isbn + " already exists.");
        }

        bookRepository.save(new Book(isbn, title, author, category));

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
    // Operation 9: getStudentLoanHistory
    // =========================================================================
    @WebMethod(operationName = "getStudentLoanHistory")
    public List<BookLoan> getStudentLoanHistory(
            @WebParam(name = "token")     String token,
            @WebParam(name = "studentId") String studentId) {

        UserSession session = verifyUser(token);

        if (session.getRole() == User.Role.STUDENT && !session.getUserId().equalsIgnoreCase(studentId)) {
            throw new WebServiceException("SOAP_FAULT: Access Denied. Students can only view their own history.");
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
        Map<String, Object> sessionMap = authClient.verifyToken(token);
        if (sessionMap == null) {
            throw new WebServiceException("SOAP_FAULT: Invalid authentication token.");
        }
        String userId = (String) sessionMap.get("userId");
        String roleStr = (String) sessionMap.get("role");
        String fullName = (String) sessionMap.get("fullName");

        User.Role role = User.Role.valueOf(roleStr);
        return new UserSession(token, userId, role, fullName);
    }

    private UserSession verifyAdmin(String token) {
        UserSession session = verifyUser(token);
        if (session.getRole() != User.Role.ADMIN) {
            throw new WebServiceException("SOAP_FAULT: Access Denied. Administrator role required.");
        }
        return session;
    }

    // =========================================================================
    // Helpers
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
