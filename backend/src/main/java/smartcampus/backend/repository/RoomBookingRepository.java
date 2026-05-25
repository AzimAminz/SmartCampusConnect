package smartcampus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartcampus.backend.model.RoomBooking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Library/Booking Service - Room Booking Data Access Layer
 * Satisfies: R8 (SOAP operations), R3 (SOA)
 */
@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {

    Optional<RoomBooking> findByBookingReference(String bookingReference);

    List<RoomBooking> findByStudentId(String studentId);

    /** Check if a given room+slot+date combination is already booked (for SOAP double-booking fault) */
    boolean existsByRoomNameAndSlotAndBookingDateAndStatus(
            String roomName, String slot, LocalDate bookingDate, RoomBooking.BookingStatus status);

    List<RoomBooking> findByRoomNameAndBookingDate(String roomName, LocalDate bookingDate);
}
