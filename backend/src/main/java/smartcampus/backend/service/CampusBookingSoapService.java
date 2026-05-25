package smartcampus.backend.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import smartcampus.backend.model.BookLoan;
import smartcampus.backend.model.RoomBooking;
import smartcampus.backend.repository.BookLoanRepository;
import smartcampus.backend.repository.RoomBookingRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;


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
    // Operation 4: borrowBook
    // =========================================================================
    @WebMethod(operationName = "borrowBook")
    public String borrowBook(
            @WebParam(name = "studentId")   String studentId,
            @WebParam(name = "studentName") String studentName,
            @WebParam(name = "isbn")        String isbn,
            @WebParam(name = "title")       String title) {

        boolean alreadyBorrowed = bookLoanRepository.existsByStudentIdAndBookIsbnAndStatus(
                studentId, isbn, BookLoan.LoanStatus.BORROWED);
        if (alreadyBorrowed) {
            throw new WebServiceException(
                    "SOAP_FAULT: Student " + studentId + " has already borrowed ISBN " + isbn);
        }

        String reference = generateRef("LN");
        LocalDate today = LocalDate.now();
        BookLoan loan = new BookLoan(reference, studentId, studentName, isbn, title, today, today.plusDays(14));
        bookLoanRepository.save(loan);

        notificationClient.send("BOOK_BORROWED", studentId, studentName,
                "Book borrowed: '" + title + "' (ISBN: " + isbn + "). Due: " + loan.getDueDate(), reference);

        return reference;
    }

    // =========================================================================
    // Operation 5: returnBook
    // =========================================================================
    @WebMethod(operationName = "returnBook")
    public boolean returnBook(@WebParam(name = "loanRef") String loanRef) {
        return bookLoanRepository.findByLoanReference(loanRef).map(loan -> {
            if (loan.getStatus() == BookLoan.LoanStatus.RETURNED) {
                throw new WebServiceException("SOAP_FAULT: Loan " + loanRef + " is already returned.");
            }
            loan.setReturnDate(LocalDate.now());
            loan.setStatus(BookLoan.LoanStatus.RETURNED);

            // Calculate late fine: RM1 per day overdue
            if (loan.getDueDate().isBefore(LocalDate.now())) {
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
                loan.setFineAmount(daysLate * 1.0);
            }
            bookLoanRepository.save(loan);

            notificationClient.send("BOOK_RETURNED", loan.getStudentId(), loan.getStudentName(),
                    "Book returned: '" + loan.getBookTitle() + "'. Fine: RM" + loan.getFineAmount(), loanRef);
            return true;
        }).orElseThrow(() -> new WebServiceException("SOAP_FAULT: Loan reference not found: " + loanRef));
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
