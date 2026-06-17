package smartcampus.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Library / Booking Service - Discussion Room Booking Entity (SOAP)
 * Maps to table: room_bookings
 * Satisfies: R8 (SOAP/WSDL - consumed by JAX-WS RoomBookingSoapService)
 */
@Entity
@Table(
    name = "room_bookings"
)
public class RoomBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 30)
    private String bookingReference; // e.g. BK-20250525-001

    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @Column(name = "student_name", length = 100)
    private String studentName;

    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;   // e.g. DK-A, DK-B, Library Room 1

    @Column(nullable = false, length = 50)
    private String slot;       // e.g. "Monday 9AM-11AM"

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(length = 300)
    private String purpose;    // Reason for booking (optional)

    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public enum BookingStatus {
        CONFIRMED,  // Room is booked successfully
        CANCELLED,  // Booking was cancelled
        COMPLETED   // Session has passed
    }

    @PrePersist
    public void onCreate() {
        this.bookedAt = LocalDateTime.now();
    }

    // ---- Constructors ----
    public RoomBooking() {}

    public RoomBooking(String bookingReference, String studentId, String studentName,
                       String roomName, String slot, LocalDate bookingDate, String purpose) {
        this.bookingReference = bookingReference;
        this.studentId = studentId;
        this.studentName = studentName;
        this.roomName = roomName;
        this.slot = slot;
        this.bookingDate = bookingDate;
        this.purpose = purpose;
        this.status = BookingStatus.CONFIRMED;
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LocalDateTime getBookedAt() { return bookedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
