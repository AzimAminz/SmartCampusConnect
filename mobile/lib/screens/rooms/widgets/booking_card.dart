import 'package:flutter/material.dart';
import '../../../models/room_booking.dart';

class BookingCard extends StatelessWidget {
  final RoomBooking booking;
  final bool isStudent;
  final VoidCallback? onCancel;

  const BookingCard({
    super.key,
    required this.booking,
    required this.isStudent,
    this.onCancel,
  });

  @override
  Widget build(BuildContext context) {
    final isConfirmed = booking.status == 'CONFIRMED';

    return Card(
      color: const Color(0xFF1E1E1E),
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: isConfirmed
              ? Colors.white10
              : Colors.redAccent.withOpacity(0.2),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        booking.roomName,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Slot: ${booking.slot}',
                        style: const TextStyle(
                          color: Color(0xFF00BFA5),
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
                // ---- Status Badge ----
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: isConfirmed
                        ? const Color(0xFF00BFA5).withOpacity(0.12)
                        : Colors.redAccent.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    booking.status,
                    style: TextStyle(
                      color: isConfirmed ? const Color(0xFF00BFA5) : Colors.redAccent,
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Divider(color: Colors.white10, height: 1),
            const SizedBox(height: 12),

            // Details
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Date of Booking:',
                  style: TextStyle(color: Colors.white38, fontSize: 12),
                ),
                Text(
                  booking.bookingDate,
                  style: const TextStyle(color: Colors.white70, fontSize: 12, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Booking Ref:',
                  style: TextStyle(color: Colors.white38, fontSize: 12),
                ),
                Text(
                  booking.bookingReference,
                  style: const TextStyle(
                    color: Color(0xFF00BFA5),
                    fontSize: 12,
                    fontFamily: 'monospace',
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Purpose:',
                  style: TextStyle(color: Colors.white38, fontSize: 12),
                ),
                Text(
                  booking.purpose,
                  style: const TextStyle(color: Colors.white70, fontSize: 12),
                ),
              ],
            ),

            if (!isStudent) ...[
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.black12,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'STUDENT CREDENTIALS (AUDIT):',
                      style: TextStyle(color: Colors.white38, fontSize: 9, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Name: ${booking.studentName}',
                      style: const TextStyle(color: Colors.white70, fontSize: 11),
                    ),
                    Text(
                      'Matric: ${booking.studentId}',
                      style: const TextStyle(color: Color(0xFF00BFA5), fontSize: 11, fontFamily: 'monospace'),
                    ),
                  ],
                ),
              ),
            ],

            if (isConfirmed && onCancel != null) ...[
              const SizedBox(height: 14),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.redAccent.withOpacity(0.12),
                      foregroundColor: Colors.redAccent,
                      elevation: 0,
                      side: const BorderSide(color: Colors.redAccent, width: 0.5),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    ),
                    icon: const Icon(Icons.cancel_schedule_send_rounded, size: 16),
                    label: const Text(
                      'CANCEL BOOKING',
                      style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                    ),
                    onPressed: onCancel,
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
