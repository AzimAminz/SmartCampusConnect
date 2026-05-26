import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../services/soap_service.dart';
import '../../services/user_service.dart';
import '../../widgets/app_drawer.dart';

class RoomBookingScreen extends StatefulWidget {
  const RoomBookingScreen({super.key});

  @override
  State<RoomBookingScreen> createState() => _RoomBookingScreenState();
}

class _RoomBookingScreenState extends State<RoomBookingScreen> {
  List<dynamic> _bookings = [];
  bool _isLoading = false;
  String? _error;

  // Form parameters
  final _formKey = GlobalKey<FormState>();
  String _selectedRoom = "Discussion Room Alpha";
  String _selectedSlot = "08:00 - 10:00";
  DateTime _selectedDate = DateTime.now().add(const Duration(days: 1));
  final _purposeController = TextEditingController();
  
  // Admin-specific booking overrides
  final _adminStudentIdController = TextEditingController();
  final _adminStudentNameController = TextEditingController();

  // Availability variables
  bool? _isAvailable;
  bool _checkingAvailability = false;

  final List<String> _rooms = [
    "Discussion Room Alpha",
    "Discussion Room Beta",
    "Programming Lab B",
    "Multipurpose Hall",
    "Mini Seminar Room"
  ];

  final List<String> _slots = [
    "08:00 - 10:00",
    "10:00 - 12:00",
    "12:00 - 14:00",
    "14:00 - 16:00",
    "16:00 - 18:00"
  ];

  @override
  void initState() {
    super.initState();
    _fetchBookings();
  }

  @override
  void dispose() {
    _purposeController.dispose();
    _adminStudentIdController.dispose();
    _adminStudentNameController.dispose();
    super.dispose();
  }

  String _formatDate(DateTime dt) {
    final year = dt.year;
    final month = String.fromCharCodes([
      ...dt.month.toString().padLeft(2, '0').codeUnits
    ]);
    final day = String.fromCharCodes([
      ...dt.day.toString().padLeft(2, '0').codeUnits
    ]);
    return "$year-$month-$day";
  }

  Future<void> _fetchBookings() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final dash = await UserService.getDashboard();
      final auth = context.read<AuthProvider>();
      
      setState(() {
        if (auth.role == 'ADMIN') {
          _bookings = dash['allRoomBookings'] as List? ?? [];
        } else {
          _bookings = dash['roomBookings'] as List? ?? [];
        }
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _isLoading = false;
      });
    }
  }

  // Operation Check Availability
  Future<void> _checkAvailability() async {
    setState(() {
      _checkingAvailability = true;
      _isAvailable = null;
    });

    try {
      final formattedDate = _formatDate(_selectedDate);
      final avail = await SoapService.checkAvailability(
        roomName: _selectedRoom,
        slot: _selectedSlot,
        date: formattedDate,
      );
      setState(() {
        _isAvailable = avail;
        _checkingAvailability = false;
      });
    } catch (e) {
      setState(() {
        _checkingAvailability = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'), backgroundColor: Colors.redAccent),
      );
    }
  }

  // Operation Create Booking
  Future<void> _submitBooking() async {
    if (!_formKey.currentState!.validate()) return;

    final auth = context.read<AuthProvider>();
    final isStudent = auth.role == 'STUDENT';
    
    final studentId = isStudent ? auth.userId! : _adminStudentIdController.text.trim();
    final studentName = isStudent ? auth.fullName! : _adminStudentNameController.text.trim();
    final purpose = _purposeController.text.trim();
    final formattedDate = _formatDate(_selectedDate);

    if (studentId.isEmpty || studentName.isEmpty || purpose.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please fill in all booking credentials.'), backgroundColor: Colors.redAccent),
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      final refCode = await SoapService.bookRoom(
        studentId: studentId,
        studentName: studentName,
        roomName: _selectedRoom,
        slot: _selectedSlot,
        date: formattedDate,
        purpose: purpose,
      );

      // Reset Form
      _purposeController.clear();
      _adminStudentIdController.clear();
      _adminStudentNameController.clear();
      _isAvailable = null;

      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Row(
            children: [
              Icon(Icons.check_circle_outline_rounded, color: Color(0xFF00BFA5), size: 28),
              SizedBox(width: 10),
              Text('Booking Confirmed', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Your room slot has been successfully registered.', style: TextStyle(color: Colors.white70)),
              const SizedBox(height: 16),
              const Text('Booking Reference Code:', style: TextStyle(color: Colors.white38, fontSize: 12)),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: Colors.black26, borderRadius: BorderRadius.circular(8)),
                child: Text(
                  refCode,
                  style: const TextStyle(color: Color(0xFF00BFA5), fontSize: 16, fontWeight: FontWeight.bold, fontFamily: 'monospace', letterSpacing: 1.1),
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
      setState(() => _isLoading = false);
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text('Booking Failed', style: TextStyle(color: Colors.white)),
          content: Text(e.toString().replaceAll("Exception: ", ""), style: const TextStyle(color: Colors.white70)),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('OK', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      );
    }
  }

  // Operation Cancel Booking
  Future<void> _cancelBooking(String refCode) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        title: const Text('Cancel Booking', style: TextStyle(color: Colors.white)),
        content: Text('Are you sure you want to cancel room booking reference $refCode?', style: const TextStyle(color: Colors.white70)),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Keep Booking', style: TextStyle(color: Colors.white54)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Cancel Booking', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    setState(() => _isLoading = true);

    try {
      final success = await SoapService.cancelBooking(refCode);
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Booking cancelled successfully!'), backgroundColor: Color(0xFF00BFA5)),
        );
      } else {
        throw Exception('Cancellation request was rejected.');
      }
      _fetchBookings();
    } catch (e) {
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'), backgroundColor: Colors.redAccent),
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
            SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Dynamic Header Description
                    Text(
                      isStudent 
                          ? 'Select your preferred study room, time slot, and date. Booking is verified live in our SOAP database.' 
                          : 'As an Administrator, you can reserve slots on behalf of students. Enter their credentials below.',
                      style: const TextStyle(color: Colors.white54, fontSize: 13, height: 1.4),
                    ),
                    const SizedBox(height: 20),

                    // ---- Room Selection ----
                    DropdownButtonFormField<String>(
                      value: _selectedRoom,
                      dropdownColor: const Color(0xFF1E1E1E),
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        labelText: 'Select Room Name',
                        labelStyle: const TextStyle(color: Colors.white70),
                        filled: true,
                        fillColor: const Color(0xFF1E1E1E),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
                      ),
                      items: _rooms.map((r) => DropdownMenuItem(value: r, child: Text(r))).toList(),
                      onChanged: (val) {
                        if (val != null) {
                          setState(() {
                            _selectedRoom = val;
                            _isAvailable = null;
                          });
                        }
                      },
                    ),
                    const SizedBox(height: 16),

                    // ---- Slot Selection ----
                    DropdownButtonFormField<String>(
                      value: _selectedSlot,
                      dropdownColor: const Color(0xFF1E1E1E),
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        labelText: 'Select Time Slot',
                        labelStyle: const TextStyle(color: Colors.white70),
                        filled: true,
                        fillColor: const Color(0xFF1E1E1E),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
                      ),
                      items: _slots.map((s) => DropdownMenuItem(value: s, child: Text(s))).toList(),
                      onChanged: (val) {
                        if (val != null) {
                          setState(() {
                            _selectedSlot = val;
                            _isAvailable = null;
                          });
                        }
                      },
                    ),
                    const SizedBox(height: 16),

                    // ---- Date Selection ----
                    InkWell(
                      onTap: () async {
                        final picked = await showDatePicker(
                          context: context,
                          initialDate: _selectedDate,
                          firstDate: DateTime.now(),
                          lastDate: DateTime.now().add(const Duration(days: 30)),
                          builder: (context, child) => Theme(
                            data: Theme.of(context).copyWith(
                              colorScheme: const ColorScheme.dark(
                                primary: Color(0xFF00BFA5),
                                onPrimary: Colors.white,
                                surface: Color(0xFF1E1E1E),
                              ),
                            ),
                            child: child!,
                          ),
                        );
                        if (picked != null) {
                          setState(() {
                            _selectedDate = picked;
                            _isAvailable = null;
                          });
                        }
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                        decoration: BoxDecoration(color: const Color(0xFF1E1E1E), borderRadius: BorderRadius.circular(10)),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Date: ${_formatDate(_selectedDate)}',
                              style: const TextStyle(color: Colors.white, fontSize: 15),
                            ),
                            const Icon(Icons.calendar_month_rounded, color: Color(0xFF00BFA5)),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),

                    // ---- Check Availability Button ----
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton.icon(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.white.withOpacity(0.05),
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                            ),
                            icon: _checkingAvailability 
                                ? const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                : const Icon(Icons.verified_user_outlined, size: 18),
                            label: const Text('Check Slot Availability'),
                            onPressed: _checkingAvailability ? null : _checkAvailability,
                          ),
                        ),
                      ],
                    ),
                    
                    // Availability Status Indicator
                    if (_isAvailable != null) ...[
                      const SizedBox(height: 12),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: _isAvailable! ? const Color(0xFF00BFA5).withOpacity(0.12) : Colors.redAccent.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                            color: _isAvailable! ? const Color(0xFF00BFA5).withOpacity(0.3) : Colors.redAccent.withOpacity(0.3),
                          ),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              _isAvailable! ? Icons.check_circle_outline_rounded : Icons.cancel_outlined,
                              color: _isAvailable! ? const Color(0xFF00BFA5) : Colors.redAccent,
                              size: 20,
                            ),
                            const SizedBox(width: 8),
                            Text(
                              _isAvailable! ? 'Slot is AVAILABLE for Booking' : 'Slot is ALREADY BOOKED',
                              style: TextStyle(
                                color: _isAvailable! ? const Color(0xFF00BFA5) : Colors.redAccent,
                                fontWeight: FontWeight.bold,
                                fontSize: 13,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                    const SizedBox(height: 24),

                    const Divider(color: Colors.white10, height: 32),

                    // ---- Booking Credentials Form ----
                    if (!isStudent) ...[
                      const Text(
                        'Student Identity Overrides (Admin Input)',
                        style: TextStyle(color: Color(0xFF00BFA5), fontWeight: FontWeight.bold, fontSize: 14),
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _adminStudentIdController,
                        style: const TextStyle(color: Colors.white),
                        decoration: InputDecoration(
                          labelText: 'Student Matric ID',
                          labelStyle: const TextStyle(color: Colors.white70),
                          filled: true,
                          fillColor: const Color(0xFF1E1E1E),
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
                        ),
                        validator: (v) => !isStudent && (v == null || v.trim().isEmpty) ? 'Required for Admin booking' : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _adminStudentNameController,
                        style: const TextStyle(color: Colors.white),
                        decoration: InputDecoration(
                          labelText: 'Student Full Name',
                          labelStyle: const TextStyle(color: Colors.white70),
                          filled: true,
                          fillColor: const Color(0xFF1E1E1E),
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
                        ),
                        validator: (v) => !isStudent && (v == null || v.trim().isEmpty) ? 'Required for Admin booking' : null,
                      ),
                      const SizedBox(height: 16),
                    ],

                    TextFormField(
                      controller: _purposeController,
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        labelText: 'Booking Purpose / Description',
                        labelStyle: const TextStyle(color: Colors.white70),
                        filled: true,
                        fillColor: const Color(0xFF1E1E1E),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
                      ),
                      validator: (v) => v == null || v.trim().isEmpty ? 'Please input purpose' : null,
                    ),
                    const SizedBox(height: 24),

                    ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF3F51B5), // Deep Indigo
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                      onPressed: _isLoading ? null : _submitBooking,
                      child: _isLoading 
                          ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                          : const Text('CONFIRM BOOKING', style: TextStyle(fontWeight: FontWeight.bold, letterSpacing: 1.1)),
                    ),
                  ],
                ),
              ),
            ),

            // ---- TAB 2: ACTIVE BOOKINGS LIST ----
            _isLoading
                ? const Center(child: CircularProgressIndicator(color: Color(0xFF00BFA5)))
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
                          style: const TextStyle(color: Colors.white54, fontSize: 13, height: 1.4),
                        ),
                        const SizedBox(height: 16),

                        // ListBuilder
                        Expanded(
                          child: _bookings.isEmpty
                              ? const Center(
                                  child: Text(
                                    'No active bookings found.',
                                    style: TextStyle(color: Colors.white38, fontSize: 14),
                                    textAlign: TextAlign.center,
                                  ),
                                )
                              : ListView.builder(
                                  itemCount: _bookings.length,
                                  itemBuilder: (context, index) {
                                    final booking = _bookings[index];
                                    final String ref = booking['bookingReference'];
                                    final String room = booking['roomName'];
                                    final String slot = booking['slot'];
                                    final String date = booking['bookingDate'] is String 
                                        ? booking['bookingDate'] 
                                        : booking['bookingDate'].toString().split('T')[0];
                                    final String purpose = booking['purpose'] ?? 'Study session';
                                    final String status = booking['status'] ?? 'CONFIRMED';
                                    final String bStudentId = booking['studentId'] ?? 'N/A';
                                    final String bStudentName = booking['studentName'] ?? 'N/A';
                                    
                                    final isConfirmed = status == 'CONFIRMED';

                                    return Card(
                                      color: const Color(0xFF1E1E1E),
                                      margin: const EdgeInsets.only(bottom: 12),
                                      shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        side: BorderSide(
                                          color: isConfirmed ? Colors.white10 : Colors.redAccent.withOpacity(0.2),
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
                                                        room,
                                                        style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                                                      ),
                                                      const SizedBox(height: 4),
                                                      Text(
                                                        'Slot: $slot',
                                                        style: const TextStyle(color: Color(0xFF00BFA5), fontSize: 13, fontWeight: FontWeight.w600),
                                                      ),
                                                    ],
                                                  ),
                                                ),
                                                // ---- Status Badge ----
                                                Container(
                                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                                  decoration: BoxDecoration(
                                                    color: isConfirmed 
                                                        ? const Color(0xFF00BFA5).withOpacity(0.15) 
                                                        : Colors.redAccent.withOpacity(0.15),
                                                    borderRadius: BorderRadius.circular(8),
                                                    border: Border.all(
                                                      color: isConfirmed 
                                                          ? const Color(0xFF00BFA5).withOpacity(0.4) 
                                                          : Colors.redAccent.withOpacity(0.4),
                                                    ),
                                                  ),
                                                  child: Text(
                                                    status,
                                                    style: TextStyle(
                                                      color: isConfirmed ? const Color(0xFF00BFA5) : Colors.redAccent,
                                                      fontSize: 10,
                                                      fontWeight: FontWeight.bold,
                                                    ),
                                                  ),
                                                ),
                                              ],
                                            ),
                                            const Divider(color: Colors.white10, height: 24),
                                            Text('Date: $date', style: const TextStyle(color: Colors.white70, fontSize: 13)),
                                            Text('Purpose: $purpose', style: const TextStyle(color: Colors.white54, fontSize: 13)),
                                            
                                            // Admin Audit view (Who booked it)
                                            if (!isStudent || bStudentId != auth.userId) ...[
                                              const SizedBox(height: 4),
                                              Text(
                                                'Booked by: $bStudentName ($bStudentId)',
                                                style: const TextStyle(color: Colors.white38, fontSize: 13, fontWeight: FontWeight.w500),
                                              ),
                                            ],

                                            const Divider(color: Colors.white10, height: 20),

                                            Row(
                                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                              children: [
                                                Text(
                                                  'Ref: $ref',
                                                  style: const TextStyle(color: Colors.white24, fontSize: 11, fontFamily: 'monospace', letterSpacing: 1.1),
                                                ),
                                                if (isConfirmed)
                                                  ElevatedButton(
                                                    style: ElevatedButton.styleFrom(
                                                      backgroundColor: Colors.redAccent.withOpacity(0.15),
                                                      foregroundColor: Colors.redAccent,
                                                      side: const BorderSide(color: Colors.redAccent, width: 0.5),
                                                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                                    ),
                                                    onPressed: () => _cancelBooking(ref),
                                                    child: const Text('CANCEL BOOKING', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11)),
                                                  ),
                                              ],
                                            ),
                                          ],
                                        ),
                                      ),
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
