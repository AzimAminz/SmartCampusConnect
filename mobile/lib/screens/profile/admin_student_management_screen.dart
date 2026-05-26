import 'package:flutter/material.dart';
import '../../services/api_service.dart';
import '../../widgets/app_drawer.dart';

/// Admin Student Management Screen
/// Provides CRUD operations for student records:
///   Search (GET /api/students + client-side filter)
///   Add    (POST /api/students)
///   Update (PUT  /api/students/{id})
///   Delete (DELETE /api/students/{id})
class AdminStudentManagementScreen extends StatefulWidget {
  const AdminStudentManagementScreen({super.key});

  @override
  State<AdminStudentManagementScreen> createState() =>
      _AdminStudentManagementScreenState();
}

class _AdminStudentManagementScreenState
    extends State<AdminStudentManagementScreen> {
  List<dynamic> _allStudents = [];
  List<dynamic> _filteredStudents = [];
  bool _isLoading = false;
  String? _error;
  String _searchQuery = '';
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _fetchStudents();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  // ──────────────────────────────────────────────────────────────
  // DATA FETCH
  // ──────────────────────────────────────────────────────────────
  Future<void> _fetchStudents() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final data = await ApiService.get('/students') as List;
      if (!mounted) return;
      setState(() {
        _allStudents = data;
        _applyFilter();
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  void _applyFilter() {
    final q = _searchQuery.toLowerCase();
    if (q.isEmpty) {
      _filteredStudents = List.from(_allStudents);
    } else {
      _filteredStudents = _allStudents.where((s) {
        final name = (s['name'] ?? '').toString().toLowerCase();
        final sid = (s['studentId'] ?? '').toString().toLowerCase();
        final email = (s['email'] ?? '').toString().toLowerCase();
        final programme = (s['programme'] ?? '').toString().toLowerCase();
        final faculty = (s['faculty'] ?? '').toString().toLowerCase();
        return name.contains(q) ||
            sid.contains(q) ||
            email.contains(q) ||
            programme.contains(q) ||
            faculty.contains(q);
      }).toList();
    }
  }

  // ──────────────────────────────────────────────────────────────
  // FACULTY & PROGRAMME DISPLAY MAPS (display only — ID generation is backend)
  // ──────────────────────────────────────────────────────────────

  /// Maps faculty name → 2-digit code (shown as hint in UI only)
  static const Map<String, String> _facultyCodes = {
    'FTMK': '03', // Teknologi Maklumat & Komunikasi
    'FTKM': '04', // Kejuruteraan Mekanikal
    'FTKEK': '05', // Kejuruteraan Elektrik & Elektronik
    'FTKIP': '06', // Teknologi Pembuatan
    'FTKE': '07', // Kejuruteraan
    'FPTT': '08', // Pengurusan Teknologi & Keusahawanan
  };

  /// Maps display label → API programmeCode (D / B / P)
  static const Map<String, String> _programmeCodes = {
    'Diploma': 'D',
    'Bachelor / Degree': 'B',
    'Prasiswazah (Postgrad)': 'P',
  };

  // ──────────────────────────────────────────────────────────────
  // ADD STUDENT DIALOG
  // ID is generated entirely by the backend (StudentService + ReentrantLock).
  // The mobile app sends:  name, email, programmeCode, faculty, semester, ...
  // The backend returns the saved Student record including the generated studentId.
  // ──────────────────────────────────────────────────────────────
  void _showAddStudentDialog() {
    final nameCtrl = TextEditingController();
    final emailCtrl = TextEditingController();
    final progNameCtrl = TextEditingController(); // e.g. "Bachelor of CS"
    final semCtrl = TextEditingController(
      text: '1',
    ); // current semester (also used for ID)
    final gpaCtrl = TextEditingController(text: '0.00');
    final phoneCtrl = TextEditingController();

    String selectedProgrammeType = 'Bachelor / Degree';
    String selectedFaculty = 'FTMK';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx2, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: Colors.white10),
          ),
          title: Row(
            children: const [
              Icon(Icons.person_add_rounded, color: Color(0xFF00BFA5)),
              SizedBox(width: 10),
              Text(
                'Add New Student',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ],
          ),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // ── ID Preview Banner ─────────────────────────
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    margin: const EdgeInsets.only(bottom: 16),
                    decoration: BoxDecoration(
                      color: const Color(0xFF00BFA5).withValues(alpha: 0.07),
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(
                        color: const Color(0xFF00BFA5).withValues(alpha: 0.25),
                      ),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.auto_awesome_rounded,
                          color: Color(0xFF00BFA5),
                          size: 16,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Student ID — Auto-generated by server',
                                style: TextStyle(
                                  color: Color(0xFF00BFA5),
                                  fontWeight: FontWeight.bold,
                                  fontSize: 11,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Format: [P][FF][YY][SC][NNN]  •  e.g. B032610001',
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.35),
                                  fontSize: 10,
                                  fontFamily: 'monospace',
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  // ── Programme Type Dropdown ───────────────────
                  const Text(
                    'Programme Type *',
                    style: TextStyle(
                      color: Colors.white54,
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 6),
                  _buildDropdown(
                    label: 'Programme Type',
                    icon: Icons.school_rounded,
                    value: selectedProgrammeType,
                    items: _programmeCodes.keys.toList(),
                    onChanged: (val) {
                      if (val != null)
                        setDialogState(() => selectedProgrammeType = val);
                    },
                  ),
                  const SizedBox(height: 12),

                  // ── Faculty Dropdown ──────────────────────────
                  const Text(
                    'Faculty *',
                    style: TextStyle(
                      color: Colors.white54,
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 6),
                  _buildDropdown(
                    label: 'Faculty',
                    icon: Icons.account_balance_rounded,
                    value: selectedFaculty,
                    items: _facultyCodes.keys.toList(),
                    onChanged: (val) {
                      if (val != null)
                        setDialogState(() => selectedFaculty = val);
                    },
                  ),
                  const SizedBox(height: 16),

                  // ── Student Details ───────────────────────────
                  const Text(
                    'Student Details',
                    style: TextStyle(
                      color: Colors.white54,
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),

                  _buildDialogField(
                    nameCtrl,
                    'Full Name',
                    Icons.person_outline_rounded,
                    required: true,
                  ),
                  _buildDialogField(
                    emailCtrl,
                    'Email Address',
                    Icons.email_outlined,
                    required: true,
                  ),
                  _buildDialogField(
                    progNameCtrl,
                    'Programme Name (e.g. Bachelor of CS)',
                    Icons.school_outlined,
                  ),
                  _buildDialogField(
                    semCtrl,
                    'Current Semester  ← used in ID',
                    Icons.calendar_today_outlined,
                    isNumber: true,
                  ),
                  _buildDialogField(
                    gpaCtrl,
                    'GPA (0.00 – 4.00)',
                    Icons.grade_outlined,
                    isDecimal: true,
                  ),
                  _buildDialogField(
                    phoneCtrl,
                    'Phone Number',
                    Icons.phone_outlined,
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text(
                'Cancel',
                style: TextStyle(color: Colors.white54),
              ),
            ),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF00BFA5),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              icon: const Icon(Icons.save_rounded, size: 16),
              label: const Text(
                'Add Student',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              onPressed: () async {
                final name = nameCtrl.text.trim();
                final email = emailCtrl.text.trim();
                final sem = semCtrl.text.trim();

                if (name.isEmpty || email.isEmpty) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    const SnackBar(
                      content: Text('Name and Email are required.'),
                      backgroundColor: Colors.orange,
                    ),
                  );
                  return;
                }

                // Send to backend — backend generates Student ID via ReentrantLock
                final body = {
                  'name': name,
                  'email': email,
                  'programmeCode':
                      _programmeCodes[selectedProgrammeType]!, // "D"/"B"/"P"
                  'faculty': selectedFaculty,
                  'semester': sem.isEmpty ? '1' : sem,
                  'programme': progNameCtrl.text.trim(),
                  'gpa': double.tryParse(gpaCtrl.text.trim()) ?? 0.0,
                  'phoneNumber': phoneCtrl.text.trim(),
                };

                try {
                  final created = await ApiService.post('/students', body);
                  final generatedId = created?['studentId'] ?? '';
                  if (ctx.mounted) Navigator.of(ctx).pop();
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text('Student created! ID: $generatedId'),
                        backgroundColor: const Color(0xFF00BFA5),
                      ),
                    );
                    _fetchStudents();
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content: Text(
                          e.toString().replaceAll('Exception: ', ''),
                        ),
                        backgroundColor: Colors.redAccent,
                      ),
                    );
                  }
                }
              },
            ),
          ],
        ),
      ),
    );
  }

  // ── Dropdown widget ─────────────────────────────────────────────────────────
  Widget _buildDropdown({
    required String label,
    required IconData icon,
    required String value,
    required List<String> items,
    required ValueChanged<String?> onChanged,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.04),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white10),
      ),
      child: Row(
        children: [
          Icon(icon, color: Colors.white38, size: 16),
          const SizedBox(width: 8),
          Expanded(
            child: DropdownButtonHideUnderline(
              child: DropdownButton<String>(
                value: value,
                isExpanded: true,
                dropdownColor: const Color(0xFF2C2C2C),
                style: const TextStyle(color: Colors.white, fontSize: 13),
                icon: const Icon(Icons.arrow_drop_down, color: Colors.white38),
                items: items
                    .map(
                      (item) => DropdownMenuItem(
                        value: item,
                        child: Text(
                          item,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 13,
                          ),
                        ),
                      ),
                    )
                    .toList(),
                onChanged: onChanged,
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ──────────────────────────────────────────────────────────────
  // EDIT STUDENT DIALOG
  // ──────────────────────────────────────────────────────────────
  void _showEditStudentDialog(dynamic student) {
    final id = student['id'];
    final nameCtrl = TextEditingController(text: student['name'] ?? '');
    final emailCtrl = TextEditingController(text: student['email'] ?? '');
    final progCtrl = TextEditingController(text: student['programme'] ?? '');
    final semCtrl = TextEditingController(
      text: student['semester']?.toString() ?? '1',
    );
    final gpaCtrl = TextEditingController(
      text: student['gpa']?.toString() ?? '0.00',
    );
    final phoneCtrl = TextEditingController(text: student['phoneNumber'] ?? '');

    // Variable untuk faculty - ambil dari data student
    String selectedFaculty = student['faculty'] ?? 'FTMK';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        // <--- PENTING: StatefulBuilder untuk setDialogState
        builder: (ctx2, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: Colors.white10),
          ),
          title: Row(
            children: [
              const Icon(Icons.edit_rounded, color: Color(0xFF3F51B5)),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Edit: ${student['studentId']}',
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Student ID — read-only
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    margin: const EdgeInsets.only(bottom: 10),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.04),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.white10),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.badge_outlined,
                          color: Colors.white30,
                          size: 16,
                        ),
                        const SizedBox(width: 10),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Student ID (Read-Only)',
                              style: TextStyle(
                                color: Colors.white30,
                                fontSize: 10,
                              ),
                            ),
                            Text(
                              student['studentId'] ?? '',
                              style: const TextStyle(
                                color: Colors.white54,
                                fontFamily: 'monospace',
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  Container(
                    alignment: Alignment.centerLeft, // ← Kiri
                    child: const Text(
                      'Faculty *',
                      style: TextStyle(
                        color: Colors.white54,
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  _buildDropdown(
                    label: 'Faculty',
                    icon: Icons.account_balance_rounded,
                    value: selectedFaculty,
                    items: _facultyCodes.keys.toList(),
                    onChanged: (val) {
                      if (val != null) {
                        setDialogState(() {
                          selectedFaculty = val; // Update nilai faculty
                        });
                      }
                    },
                  ),
                  const SizedBox(height: 12),
                  _buildDialogField(
                    nameCtrl,
                    'Full Name',
                    Icons.person_outline_rounded,
                    required: true,
                  ),
                  _buildDialogField(
                    emailCtrl,
                    'Email Address',
                    Icons.email_outlined,
                    required: true,
                  ),
                  _buildDialogField(
                    progCtrl,
                    'Programme Name',
                    Icons.school_outlined,
                  ),

                  _buildDialogField(
                    semCtrl,
                    'Semester',
                    Icons.calendar_today_outlined,
                    isNumber: true,
                  ),
                  _buildDialogField(
                    gpaCtrl,
                    'GPA (0.00 - 4.00)',
                    Icons.grade_outlined,
                    isDecimal: true,
                  ),
                  _buildDialogField(
                    phoneCtrl,
                    'Phone Number',
                    Icons.phone_outlined,
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text(
                'Cancel',
                style: TextStyle(color: Colors.white54),
              ),
            ),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF3F51B5),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              icon: const Icon(Icons.save_rounded, size: 16),
              label: const Text(
                'Save Changes',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              onPressed: () async {
                final name = nameCtrl.text.trim();
                final email = emailCtrl.text.trim();

                if (name.isEmpty || email.isEmpty) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    const SnackBar(
                      content: Text('Name and Email are required.'),
                      backgroundColor: Colors.orange,
                    ),
                  );
                  return;
                }

                // Gunakan selectedFaculty dari state
                final body = {
                  'name': name,
                  'email': email,
                  'programme': progCtrl.text.trim(),
                  'faculty': selectedFaculty,
                  'semester': semCtrl.text.trim().isEmpty
                      ? '1'
                      : semCtrl.text.trim(),
                  'gpa': double.tryParse(gpaCtrl.text.trim()) ?? 0.0,
                  'phoneNumber': phoneCtrl.text.trim(),
                };

                try {
                  await ApiService.put('/students/$id', body);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Student updated successfully!'),
                        backgroundColor: Color(0xFF00BFA5),
                      ),
                    );
                    _fetchStudents();
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content: Text(
                          e.toString().replaceAll('Exception: ', ''),
                        ),
                        backgroundColor: Colors.redAccent,
                      ),
                    );
                  }
                }
              },
            ),
          ],
        ),
      ),
    );
  }

  // ──────────────────────────────────────────────────────────────
  // DELETE STUDENT
  // ──────────────────────────────────────────────────────────────
  Future<void> _deleteStudent(dynamic student) async {
    final id = student['id'];
    final name = student['name'] ?? 'Unknown';
    final sid = student['studentId'] ?? '';

    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: Colors.redAccent.withValues(alpha: 0.4)),
        ),
        title: Row(
          children: const [
            Icon(Icons.warning_rounded, color: Colors.redAccent),
            SizedBox(width: 10),
            Text(
              'Delete Student',
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
              'This action is permanent and cannot be undone.',
              style: TextStyle(color: Colors.white70, fontSize: 13),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.redAccent.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(
                  color: Colors.redAccent.withValues(alpha: 0.2),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Text(
                    sid,
                    style: const TextStyle(
                      color: Colors.white54,
                      fontSize: 12,
                      fontFamily: 'monospace',
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(
              'Cancel',
              style: TextStyle(color: Colors.white54),
            ),
          ),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.redAccent,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
            icon: const Icon(Icons.delete_forever_rounded, size: 16),
            label: const Text(
              'Delete',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
          ),
        ],
      ),
    );

    if (confirm != true || !mounted) return;

    setState(() => _isLoading = true);
    try {
      await ApiService.delete('/students/$id');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('$name deleted successfully.'),
          backgroundColor: const Color(0xFF00BFA5),
        ),
      );
      _fetchStudents();
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(e.toString().replaceAll('Exception: ', '')),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  // ──────────────────────────────────────────────────────────────
  // VIEW STUDENT DETAIL BOTTOM SHEET
  // ──────────────────────────────────────────────────────────────
  void _showStudentDetail(dynamic student) {
    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF1E1E1E),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Handle bar
            Center(
              child: Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: 20),
                decoration: BoxDecoration(
                  color: Colors.white24,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            Row(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundColor: const Color(
                    0xFF3F51B5,
                  ).withValues(alpha: 0.2),
                  child: Text(
                    (student['name'] ?? 'S')[0].toUpperCase(),
                    style: const TextStyle(
                      color: Color(0xFF3F51B5),
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        student['name'] ?? 'Unknown',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        student['studentId'] ?? '',
                        style: const TextStyle(
                          color: Color(0xFF00BFA5),
                          fontSize: 13,
                          fontFamily: 'monospace',
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            const Divider(color: Colors.white10),
            _buildDetailRow(
              Icons.email_outlined,
              'Email',
              student['email'] ?? 'N/A',
            ),
            _buildDetailRow(
              Icons.school_outlined,
              'Programme',
              student['programme'] ?? 'N/A',
            ),
            _buildDetailRow(
              Icons.account_balance_outlined,
              'Faculty',
              student['faculty'] ?? 'N/A',
            ),
            _buildDetailRow(
              Icons.calendar_today_outlined,
              'Semester',
              'Semester ${student['semester'] ?? 'N/A'}',
            ),
            _buildDetailRow(
              Icons.grade_outlined,
              'GPA',
              student['gpa']?.toString() ?? 'N/A',
            ),
            _buildDetailRow(
              Icons.phone_outlined,
              'Phone',
              student['phoneNumber'] ?? 'N/A',
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFF3F51B5),
                      side: const BorderSide(color: Color(0xFF3F51B5)),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    icon: const Icon(Icons.edit_rounded, size: 16),
                    label: const Text('Edit'),
                    onPressed: () {
                      Navigator.of(ctx).pop();
                      _showEditStudentDialog(student);
                    },
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.redAccent,
                      side: const BorderSide(color: Colors.redAccent),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    icon: const Icon(Icons.delete_outline_rounded, size: 16),
                    label: const Text('Delete'),
                    onPressed: () {
                      Navigator.of(ctx).pop();
                      _deleteStudent(student);
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // ──────────────────────────────────────────────────────────────
  // BUILD
  // ──────────────────────────────────────────────────────────────
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1E1E1E),
        foregroundColor: Colors.white,
        title: const Text('Student Management'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Refresh',
            onPressed: _fetchStudents,
          ),
        ],
      ),
      drawer: const AppDrawer(currentRoute: 'students'),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: const Color(0xFF00BFA5),
        foregroundColor: Colors.white,
        icon: const Icon(Icons.person_add_rounded),
        label: const Text(
          'Add Student',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        onPressed: _showAddStudentDialog,
      ),
      body: Column(
        children: [
          // ── Header Stats ──────────────────────────────────────
          Container(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
            child: Row(
              children: [
                _buildStatPill(
                  label: 'Total Students',
                  value: '${_allStudents.length}',
                  color: const Color(0xFF3F51B5),
                  icon: Icons.people_alt_rounded,
                ),
                const SizedBox(width: 10),
                _buildStatPill(
                  label: 'Showing',
                  value: '${_filteredStudents.length}',
                  color: const Color(0xFF00BFA5),
                  icon: Icons.filter_list_rounded,
                ),
              ],
            ),
          ),

          // ── Search Bar ────────────────────────────────────────
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              style: const TextStyle(color: Colors.white),
              onChanged: (val) {
                setState(() {
                  _searchQuery = val.trim();
                  _applyFilter();
                });
              },
              decoration: InputDecoration(
                hintText: 'Search by name, ID, email, programme...',
                hintStyle: const TextStyle(color: Colors.white38),
                prefixIcon: const Icon(
                  Icons.search_rounded,
                  color: Color(0xFF00BFA5),
                ),
                suffixIcon: _searchQuery.isNotEmpty
                    ? IconButton(
                        icon: const Icon(
                          Icons.clear_rounded,
                          color: Colors.white54,
                        ),
                        onPressed: () {
                          _searchController.clear();
                          setState(() {
                            _searchQuery = '';
                            _applyFilter();
                          });
                        },
                      )
                    : null,
                filled: true,
                fillColor: Colors.white.withValues(alpha: 0.04),
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(color: Colors.white10),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(color: Colors.white10),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(
                    color: Color(0xFF00BFA5),
                    width: 1.5,
                  ),
                ),
              ),
            ),
          ),

          // ── Content ───────────────────────────────────────────
          Expanded(
            child: _isLoading
                ? const Center(
                    child: CircularProgressIndicator(color: Color(0xFF00BFA5)),
                  )
                : _error != null
                ? _buildErrorState()
                : _filteredStudents.isEmpty
                ? _buildEmptyState()
                : ListView.builder(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
                    itemCount: _filteredStudents.length,
                    itemBuilder: (ctx, index) =>
                        _buildStudentCard(_filteredStudents[index], index),
                  ),
          ),
        ],
      ),
    );
  }

  // ──────────────────────────────────────────────────────────────
  // STUDENT CARD WIDGET
  // ──────────────────────────────────────────────────────────────
  Widget _buildStudentCard(dynamic student, int index) {
    final name = student['name'] ?? 'Unknown';
    final sid = student['studentId'] ?? '';
    final email = student['email'] ?? '';
    final faculty = student['faculty'] ?? 'N/A';
    final gpa = student['gpa']?.toString() ?? 'N/A';
    final sem = student['semester']?.toString() ?? '1';

    // Avatar color based on index
    final colors = [
      const Color(0xFF3F51B5),
      const Color(0xFF00BFA5),
      Colors.purpleAccent,
      Colors.amberAccent,
      Colors.cyanAccent,
    ];
    final avatarColor = colors[index % colors.length];

    return Card(
      color: const Color(0xFF1E1E1E),
      margin: const EdgeInsets.only(bottom: 10),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: Colors.white10),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => _showStudentDetail(student),
        child: Padding(
          padding: const EdgeInsets.all(14.0),
          child: Row(
            children: [
              // Avatar
              CircleAvatar(
                radius: 24,
                backgroundColor: avatarColor.withValues(alpha: 0.15),
                child: Text(
                  name.isNotEmpty ? name[0].toUpperCase() : 'S',
                  style: TextStyle(
                    color: avatarColor,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(width: 14),
              // Student Info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      name,
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 15,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      sid,
                      style: const TextStyle(
                        color: Color(0xFF00BFA5),
                        fontSize: 12,
                        fontFamily: 'monospace',
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      email,
                      style: const TextStyle(
                        color: Colors.white54,
                        fontSize: 11,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        _buildMiniChip(
                          '$faculty',
                          Colors.white24,
                          Colors.white70,
                        ),
                        const SizedBox(width: 6),
                        _buildMiniChip(
                          'Sem $sem',
                          const Color(0xFF3F51B5).withValues(alpha: 0.3),
                          const Color(0xFF3F51B5),
                        ),
                        const SizedBox(width: 6),
                        _buildMiniChip(
                          'GPA: $gpa',
                          const Color(0xFF00BFA5).withValues(alpha: 0.2),
                          const Color(0xFF00BFA5),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              // Action buttons
              Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: const Icon(
                      Icons.edit_rounded,
                      size: 18,
                      color: Color(0xFF3F51B5),
                    ),
                    tooltip: 'Edit',
                    onPressed: () => _showEditStudentDialog(student),
                    padding: const EdgeInsets.all(4),
                    constraints: const BoxConstraints(),
                  ),
                  const SizedBox(height: 6),
                  IconButton(
                    icon: const Icon(
                      Icons.delete_outline_rounded,
                      size: 18,
                      color: Colors.redAccent,
                    ),
                    tooltip: 'Delete',
                    onPressed: () => _deleteStudent(student),
                    padding: const EdgeInsets.all(4),
                    constraints: const BoxConstraints(),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ──────────────────────────────────────────────────────────────
  // HELPER WIDGETS
  // ──────────────────────────────────────────────────────────────
  Widget _buildStatPill({
    required String label,
    required String value,
    required Color color,
    required IconData icon,
  }) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: color.withValues(alpha: 0.3)),
        ),
        child: Row(
          children: [
            Icon(icon, color: color, size: 18),
            const SizedBox(width: 10),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  value,
                  style: TextStyle(
                    color: color,
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  label,
                  style: const TextStyle(color: Colors.white54, fontSize: 10),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMiniChip(String label, Color bgColor, Color textColor) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(5),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: textColor,
          fontSize: 10,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildDialogField(
    TextEditingController ctrl,
    String label,
    IconData icon, {
    bool required = false,
    bool isNumber = false,
    bool isDecimal = false,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextField(
        controller: ctrl,
        style: const TextStyle(color: Colors.white),
        keyboardType: isDecimal
            ? const TextInputType.numberWithOptions(decimal: true)
            : isNumber
            ? TextInputType.number
            : TextInputType.text,
        decoration: InputDecoration(
          labelText: required ? '$label *' : label,
          labelStyle: TextStyle(
            color: required ? Colors.white70 : Colors.white54,
            fontSize: 13,
          ),
          prefixIcon: Icon(icon, color: Colors.white38, size: 18),
          filled: true,
          fillColor: Colors.white.withValues(alpha: 0.04),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 12,
            vertical: 10,
          ),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: Colors.white10),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: Colors.white10),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: Color(0xFF00BFA5), width: 1.2),
          ),
        ),
      ),
    );
  }

  Widget _buildDetailRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Icon(icon, color: Colors.white38, size: 16),
          const SizedBox(width: 10),
          Text(
            '$label: ',
            style: const TextStyle(color: Colors.white54, fontSize: 13),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.cloud_off_rounded,
              color: Colors.white24,
              size: 48,
            ),
            const SizedBox(height: 16),
            Text(
              _error!,
              style: const TextStyle(color: Colors.redAccent, fontSize: 13),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF00BFA5),
                foregroundColor: Colors.white,
              ),
              icon: const Icon(Icons.refresh_rounded, size: 16),
              label: const Text('Retry'),
              onPressed: _fetchStudents,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.person_search_rounded,
            color: Colors.white24,
            size: 56,
          ),
          const SizedBox(height: 16),
          Text(
            _searchQuery.isNotEmpty
                ? 'No students match "$_searchQuery"'
                : 'No students registered yet.',
            style: const TextStyle(color: Colors.white38, fontSize: 14),
            textAlign: TextAlign.center,
          ),
          if (_searchQuery.isNotEmpty) ...[
            const SizedBox(height: 12),
            TextButton(
              onPressed: () {
                _searchController.clear();
                setState(() {
                  _searchQuery = '';
                  _applyFilter();
                });
              },
              child: const Text(
                'Clear Search',
                style: TextStyle(color: Color(0xFF00BFA5)),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
