import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../services/soap_service.dart';
import '../../services/user_service.dart';
import '../../widgets/app_drawer.dart';
import '../../models/room_booking.dart';
import '../../utils/date_formatter.dart';
import 'widgets/booking_form.dart';
import 'widgets/booking_card.dart';

class RoomBookingScreen extends StatefulWidget {
  const RoomBookingScreen({super.key});

  @override
  State<RoomBookingScreen> createState() => _RoomBookingScreenState();
}

class _RoomBookingScreenState extends State<RoomBookingScreen> {
  List<RoomBooking> _bookings = [];
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _fetchBookings();
  }

  Future<void> _fetchBookings() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
    });

    try {
      final dash = await UserService.getDashboard();
      final auth = context.read<AuthProvider>();

      final rawList = (auth.role == 'ADMIN')
          ? dash['allRoomBookings'] as List? ?? []
          : dash['roomBookings'] as List? ?? [];

      if (!mounted) return;
      setState(() {
        _bookings = rawList
            .map((e) => RoomBooking.fromJson(Map<String, dynamic>.from(e)))
            .toList();
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error loading bookings: ${e.toString().replaceAll("Exception: ", "")}'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  // SOAP Check Availability Handler
  Future<bool> _handleCheckAvailability({
    required String roomName,
    required String slot,
    required String date,
  }) async {
    return await SoapService.checkAvailability(
      roomName: roomName,
      slot: slot,
      date: date,
    );
  }

  // SOAP Create Booking Handler
  Future<void> _handleCreateBooking({
    required String studentId,
    required String studentName,
    required String roomName,
    required String slot,
    required DateTime date,
    required String purpose,
  }) async {
    setState(() => _isLoading = true);

    try {
      final formattedDate = DateFormatter.formatDate(date);
      final refCode = await SoapService.bookRoom(
        studentId: studentId,
        studentName: studentName,
        roomName: roomName,
        slot: slot,
        date: formattedDate,
        purpose: purpose,
      );

      if (!mounted) return;
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Row(
            children: [
              Icon(
                Icons.check_circle_outline_rounded,
                color: Color(0xFF00BFA5),
                size: 28,
              ),
              SizedBox(width: 10),
              Text(
                'Booking Confirmed',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Your room slot has been successfully registered.',
                style: TextStyle(color: Colors.white70),
              ),
              const SizedBox(height: 16),
              const Text(
                'Booking Reference Code:',
                style: TextStyle(color: Colors.white38, fontSize: 12),
              ),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.black26,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  refCode,
                  style: const TextStyle(
                    color: Color(0xFF00BFA5),
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    fontFamily: 'monospace',
                    letterSpacing: 1.1,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Great', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      );

      _fetchBookings(); // Refresh bookings list
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text(
            'Booking Failed',
            style: TextStyle(color: Colors.white),
          ),
          content: Text(
            e.toString().replaceAll("Exception: ", ""),
            style: const TextStyle(color: Colors.white70),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('OK', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      );
      rethrow;
    }
  }

  // SOAP Cancel Booking Handler
  Future<void> _cancelBooking(String refCode) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        title: const Text(
          'Cancel Booking',
          style: TextStyle(color: Colors.white),
        ),
        content: Text(
          'Are you sure you want to cancel room booking reference $refCode?',
          style: const TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(
              'Keep Booking',
              style: TextStyle(color: Colors.white54),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(
              'Cancel Booking',
              style: TextStyle(color: Colors.white),
            ),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    if (!mounted) return;
    setState(() => _isLoading = true);

    try {
      final success = await SoapService.cancelBooking(refCode);
      if (!mounted) return;
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Booking cancelled successfully!'),
            backgroundColor: Color(0xFF00BFA5),
          ),
        );
      } else {
        throw Exception('Cancellation request was rejected.');
      }
      _fetchBookings();
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final isStudent = auth.role == 'STUDENT';

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: const Color(0xFF121212), // Matte Obsidian
        appBar: AppBar(
          title: const Text('SOAP Room Bookings'),
          backgroundColor: const Color(0xFF1E1E1E),
          foregroundColor: Colors.white,
          bottom: const TabBar(
            tabs: [
              Tab(icon: Icon(Icons.edit_calendar_rounded), text: 'Book a Slot'),
              Tab(icon: Icon(Icons.list_alt_rounded), text: 'Active Bookings'),
            ],
            indicatorColor: Color(0xFF00BFA5),
            labelColor: Color(0xFF00BFA5),
            unselectedLabelColor: Colors.white54,
          ),
        ),
        drawer: const AppDrawer(currentRoute: 'rooms'),
        body: TabBarView(
          children: [
            // ---- TAB 1: BOOKING FORM ----
            BookingForm(
              isStudent: isStudent,
              currentUserId: auth.userId ?? '',
              currentUserFullName: auth.fullName ?? '',
              isLoading: _isLoading,
              onCheckAvailability: _handleCheckAvailability,
              onSubmitBooking: _handleCreateBooking,
            ),

            // ---- TAB 2: ACTIVE BOOKINGS LIST ----
            _isLoading
                ? const Center(
                    child: CircularProgressIndicator(color: Color(0xFF00BFA5)),
                  )
                : Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // Dynamic Header
                        Text(
                          isStudent
                              ? 'Logs of study slots under your personal Matric account.'
                              : 'Total Campus bookings registry ledger (Admin audit access).',
                          style: const TextStyle(
                            color: Colors.white54,
                            fontSize: 13,
                            height: 1.4,
                          ),
                        ),
                        const SizedBox(height: 16),

                        // ListBuilder
                        Expanded(
                          child: _bookings.isEmpty
                              ? const Center(
                                  child: Text(
                                    'No active bookings found.',
                                    style: TextStyle(
                                      color: Colors.white38,
                                      fontSize: 14,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),
                                )
                              : ListView.builder(
                                  itemCount: _bookings.length,
                                  itemBuilder: (context, index) {
                                    final booking = _bookings[index];
                                    return BookingCard(
                                      booking: booking,
                                      isStudent: isStudent,
                                      onCancel: () => _cancelBooking(booking.bookingReference),
                                    );
                                  },
                                ),
                        ),
                      ],
                    ),
                  ),
          ],
        ),
      ),
    );
  }
}
