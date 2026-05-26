class RoomBooking {
  final int? id;
  final String bookingReference;
  final String studentId;
  final String studentName;
  final String roomName;
  final String slot;
  final String bookingDate; // yyyy-MM-dd
  final String purpose;
  final String status; // 'CONFIRMED', 'CANCELLED', etc.

  RoomBooking({
    this.id,
    required this.bookingReference,
    required this.studentId,
    required this.studentName,
    required this.roomName,
    required this.slot,
    required this.bookingDate,
    required this.purpose,
    required this.status,
  });

  factory RoomBooking.fromJson(Map<String, dynamic> json) {
    // Normalise bookingDate string representation
    String dateStr = '';
    if (json['bookingDate'] != null) {
      final rawDate = json['bookingDate'].toString();
      dateStr = rawDate.contains('T') ? rawDate.split('T')[0] : rawDate;
    }

    return RoomBooking(
      id: json['id'] as int?,
      bookingReference: json['bookingReference'] as String? ?? '',
      studentId: json['studentId'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      roomName: json['roomName'] as String? ?? '',
      slot: json['slot'] as String? ?? '',
      bookingDate: dateStr,
      purpose: json['purpose'] as String? ?? 'Study session',
      status: json['status'] as String? ?? 'CONFIRMED',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'bookingReference': bookingReference,
      'studentId': studentId,
      'studentName': studentName,
      'roomName': roomName,
      'slot': slot,
      'bookingDate': bookingDate,
      'purpose': purpose,
      'status': status,
    };
  }
}
