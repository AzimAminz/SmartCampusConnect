package smartcampus.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartcampus.booking.model.RoomBooking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {

    Optional<RoomBooking> findByBookingReference(String bookingReference);

    List<RoomBooking> findByStudentId(String studentId);

    long countByStatus(RoomBooking.BookingStatus status);

    boolean existsByRoomNameAndSlotAndBookingDateAndStatus(
            String roomName, String slot, LocalDate bookingDate, RoomBooking.BookingStatus status);

    List<RoomBooking> findByRoomNameAndBookingDate(String roomName, LocalDate bookingDate);
}
