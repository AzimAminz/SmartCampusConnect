import 'package:flutter/material.dart';
import '../../services/student_service.dart';
import '../../widgets/app_drawer.dart';
import '../../models/student.dart';
import 'widgets/student_card.dart';
import 'widgets/student_dialogs.dart';

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
  List<Student> _allStudents = [];
  List<Student> _filteredStudents = [];
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
      final data = await StudentService.getStudents();
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
        final name = s.name.toLowerCase();
        final sid = s.studentId.toLowerCase();
        final email = s.email.toLowerCase();
        final programme = (s.programme ?? '').toLowerCase();
        final faculty = (s.faculty ?? '').toLowerCase();
        return name.contains(q) ||
            sid.contains(q) ||
            email.contains(q) ||
            programme.contains(q) ||
            faculty.contains(q);
      }).toList();
    }
  }

  // ──────────────────────────────────────────────────────────────
  // ADD STUDENT DIALOG
  // ID is generated entirely by the backend.
  // ──────────────────────────────────────────────────────────────
  void _showAddStudentDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AddStudentDialog(
        onAdd: (studentData) async {
          final created = await StudentService.addStudent(studentData);
          final generatedId = created.studentId;
          _fetchStudents();
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Student created! ID: $generatedId'),
                backgroundColor: const Color(0xFF00BFA5),
              ),
            );
          }
        },
      ),
    );
  }

  // ──────────────────────────────────────────────────────────────
  // EDIT STUDENT DIALOG
  // ──────────────────────────────────────────────────────────────
  void _showEditStudentDialog(Student student) {
    showDialog(
      context: context,
      builder: (ctx) => EditStudentDialog(
        student: student,
        onSave: (studentData) async {
          await StudentService.updateStudent(student.id, studentData);
          _fetchStudents();
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Student updated successfully!'),
                backgroundColor: Color(0xFF00BFA5),
              ),
            );
          }
        },
      ),
    );
  }

  // ──────────────────────────────────────────────────────────────
  // DELETE STUDENT
  // ──────────────────────────────────────────────────────────────
  Future<void> _deleteStudent(Student student) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: Colors.redAccent.withOpacity(0.4)),
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
                color: Colors.redAccent.withOpacity(0.08),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(
                  color: Colors.redAccent.withOpacity(0.2),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    student.name,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Text(
                    student.studentId,
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
      await StudentService.deleteStudent(student.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('${student.name} deleted successfully.'),
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
  void _showStudentDetail(Student student) {
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
                  backgroundColor: const Color(0xFF3F51B5).withOpacity(0.2),
                  child: Text(
                    (student.name.isNotEmpty ? student.name[0] : 'S').toUpperCase(),
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
                        student.name,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        student.studentId,
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
            _buildDetailRow(Icons.email_outlined, 'Email', student.email),
            _buildDetailRow(Icons.school_outlined, 'Programme', student.programme ?? 'N/A'),
            _buildDetailRow(Icons.account_balance_outlined, 'Faculty', student.faculty ?? 'N/A'),
            _buildDetailRow(Icons.calendar_today_outlined, 'Semester', 'Semester ${student.semester ?? 'N/A'}'),
            _buildDetailRow(Icons.grade_outlined, 'GPA', student.gpa.toStringAsFixed(2)),
            _buildDetailRow(Icons.phone_outlined, 'Phone', student.phoneNumber ?? 'N/A'),
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
                fillColor: Colors.white.withOpacity(0.04),
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
                            itemBuilder: (ctx, index) => StudentCard(
                              student: _filteredStudents[index],
                              index: index,
                              onTap: () => _showStudentDetail(_filteredStudents[index]),
                              onEdit: () => _showEditStudentDialog(_filteredStudents[index]),
                              onDelete: () => _deleteStudent(_filteredStudents[index]),
                            ),
                          ),
          ),
        ],
      ),
    );
  }

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
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: color.withOpacity(0.3)),
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
