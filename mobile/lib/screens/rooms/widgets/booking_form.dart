import 'package:flutter/material.dart';
import '../../../utils/date_formatter.dart';

class BookingForm extends StatefulWidget {
  final bool isStudent;
  final String currentUserId;
  final String currentUserFullName;
  final bool isLoading;
  final Future<bool> Function({
    required String roomName,
    required String slot,
    required String date,
  }) onCheckAvailability;
  final Future<void> Function({
    required String studentId,
    required String studentName,
    required String roomName,
    required String slot,
    required DateTime date,
    required String purpose,
  }) onSubmitBooking;

  const BookingForm({
    super.key,
    required this.isStudent,
    required this.currentUserId,
    required this.currentUserFullName,
    required this.isLoading,
    required this.onCheckAvailability,
    required this.onSubmitBooking,
  });

  @override
  State<BookingForm> createState() => _BookingFormState();
}

class _BookingFormState extends State<BookingForm> {
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
    "Mini Seminar Room",
  ];

  final List<String> _slots = [
    "08:00 - 10:00",
    "10:00 - 12:00",
    "12:00 - 14:00",
    "14:00 - 16:00",
    "16:00 - 18:00",
  ];

  @override
  void dispose() {
    _purposeController.dispose();
    _adminStudentIdController.dispose();
    _adminStudentNameController.dispose();
    super.dispose();
  }

  Future<void> _checkSlotAvailability() async {
    setState(() {
      _checkingAvailability = true;
      _isAvailable = null;
    });

    try {
      final formattedDate = DateFormatter.formatDate(_selectedDate);
      final avail = await widget.onCheckAvailability(
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
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    }
  }

  void _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final studentId = widget.isStudent ? widget.currentUserId : _adminStudentIdController.text.trim();
    final studentName = widget.isStudent ? widget.currentUserFullName : _adminStudentNameController.text.trim();
    final purpose = _purposeController.text.trim();

    if (studentId.isEmpty || studentName.isEmpty || purpose.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please fill in all booking credentials.'),
          backgroundColor: Colors.redAccent,
        ),
      );
      return;
    }

    try {
      await widget.onSubmitBooking(
        studentId: studentId,
        studentName: studentName,
        roomName: _selectedRoom,
        slot: _selectedSlot,
        date: _selectedDate,
        purpose: purpose,
      );
      // Reset Form State
      _purposeController.clear();
      _adminStudentIdController.clear();
      _adminStudentNameController.clear();
      setState(() {
        _isAvailable = null;
      });
    } catch (_) {
      // Errors handled by caller dialog
    }
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Dynamic Header Description
            Text(
              widget.isStudent
                  ? 'Select your preferred study room, time slot, and date. Booking is verified live in our SOAP database.'
                  : 'As an Administrator, you can reserve slots on behalf of students. Enter their credentials below.',
              style: const TextStyle(
                color: Colors.white54,
                fontSize: 13,
                height: 1.4,
              ),
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
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: BorderSide.none,
                ),
              ),
              items: _rooms
                  .map((r) => DropdownMenuItem(value: r, child: Text(r)))
                  .toList(),
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
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: BorderSide.none,
                ),
              ),
              items: _slots
                  .map((s) => DropdownMenuItem(value: s, child: Text(s)))
                  .toList(),
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
                decoration: BoxDecoration(
                  color: const Color(0xFF1E1E1E),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Date: ${DateFormatter.formatDate(_selectedDate)}',
                      style: const TextStyle(color: Colors.white, fontSize: 15),
                    ),
                    const Icon(
                      Icons.calendar_month_rounded,
                      color: Color(0xFF00BFA5),
                    ),
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
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                    icon: _checkingAvailability
                        ? const SizedBox(
                            height: 16,
                            width: 16,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Icon(
                            Icons.verified_user_outlined,
                            size: 18,
                          ),
                    label: const Text('Check Slot Availability'),
                    onPressed: _checkingAvailability ? null : _checkSlotAvailability,
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
                  color: _isAvailable!
                      ? const Color(0xFF00BFA5).withOpacity(0.12)
                      : Colors.redAccent.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: _isAvailable!
                        ? const Color(0xFF00BFA5).withOpacity(0.3)
                        : Colors.redAccent.withOpacity(0.3),
                  ),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      _isAvailable!
                          ? Icons.check_circle_outline_rounded
                          : Icons.cancel_outlined,
                      color: _isAvailable! ? const Color(0xFF00BFA5) : Colors.redAccent,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      _isAvailable!
                          ? 'Slot is AVAILABLE for Booking'
                          : 'Slot is ALREADY BOOKED',
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
            if (!widget.isStudent) ...[
              const Text(
                'Student Identity Overrides (Admin Input)',
                style: TextStyle(
                  color: Color(0xFF00BFA5),
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                ),
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
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(10),
                    borderSide: BorderSide.none,
                  ),
                ),
                validator: (v) =>
                    !widget.isStudent && (v == null || v.trim().isEmpty) ? 'Required for Admin booking' : null,
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
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(10),
                    borderSide: BorderSide.none,
                  ),
                ),
                validator: (v) =>
                    !widget.isStudent && (v == null || v.trim().isEmpty) ? 'Required for Admin booking' : null,
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
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: BorderSide.none,
                ),
              ),
              validator: (v) => v == null || v.trim().isEmpty ? 'Please input purpose' : null,
            ),
            const SizedBox(height: 24),

            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF3F51B5),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              onPressed: widget.isLoading ? null : _submit,
              child: widget.isLoading
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'CONFIRM BOOKING',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        letterSpacing: 1.1,
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
