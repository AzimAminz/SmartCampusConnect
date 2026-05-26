import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../services/user_service.dart';
import '../../widgets/app_drawer.dart';
import '../profile/admin_student_management_screen.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  Map<String, dynamic> _dashData = {};
  bool _isLoading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchDashboardData();
  }

  Future<void> _fetchDashboardData() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final response = await UserService.getDashboard();
      if (!mounted) return;
      setState(() {
        _dashData = response;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final isStudent = auth.role == 'STUDENT';
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: const Color(0xFF121212), // Matte Obsidian
      appBar: AppBar(
        title: const Text('SmartCampus Connect'),
        backgroundColor: const Color(0xFF1E1E1E),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _fetchDashboardData,
          ),
        ],
      ),
      drawer: const AppDrawer(currentRoute: 'dashboard'),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF00BFA5)))
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // ---- Welcome Greeting Card ----
                  _buildWelcomeCard(auth),
                  const SizedBox(height: 24),

                  // ---- Error Alert Box ----
                  if (_error != null) ...[
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.redAccent.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: Colors.redAccent.withOpacity(0.3)),
                      ),
                      child: Text(_error!, style: const TextStyle(color: Colors.redAccent, fontSize: 13)),
                    ),
                    const SizedBox(height: 20),
                  ],

                  // ---- Analytics Title ----
                  Text(
                    'Campus Metrics & Analytics',
                    style: theme.textTheme.titleMedium?.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 12),

                  // ---- Dynamic Analytics Grid ----
                  isStudent 
                      ? _buildStudentAnalyticsGrid() 
                      : _buildAdminAnalyticsGrid(),
                  const SizedBox(height: 24),

                  // ---- Bottom Section Panels ----
                  isStudent 
                      ? _buildStudentDetailSections() 
                      : _buildAdminDetailSections(),
                ],
              ),
            ),
    );
  }

  Widget _buildWelcomeCard(AuthProvider auth) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF3F51B5), Color(0xFF303F9F)], // Deep Indigo
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Colors.black26,
            blurRadius: 10,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Welcome back,', style: TextStyle(color: Colors.white70, fontSize: 14)),
          const SizedBox(height: 4),
          Text(
            auth.fullName ?? 'User',
            style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  auth.role ?? 'STUDENT',
                  style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 1.1),
                ),
              ),
              Text(
                'ID: ${auth.userId}',
                style: const TextStyle(color: Colors.white70, fontSize: 13, fontFamily: 'monospace'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // STUDENT Analytics Cards Grid
  Widget _buildStudentAnalyticsGrid() {
    final rawEnrolments = _dashData['enrolments'] as List? ?? [];
    final enrolments = rawEnrolments.where((e) => e['status'] == 'ACTIVE').toList();
    final bookings = _dashData['roomBookings'] as List? ?? [];
    final loans = _dashData['bookLoans'] as List? ?? [];
    final profile = _dashData['profile'] as Map? ?? {};
    final gpa = profile['gpa']?.toString() ?? 'N/A';

    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 16,
      mainAxisSpacing: 16,
      childAspectRatio: 1.35,
      children: [
        _buildMetricCard(
          title: 'CGPA Grade',
          value: gpa,
          subtitle: 'Academic Merit',
          color: const Color(0xFF00BFA5), // Neon Mint
          icon: Icons.auto_awesome_rounded,
        ),
        _buildMetricCard(
          title: 'Courses Enrolled',
          value: '${enrolments.length}',
          subtitle: 'Active Sem classes',
          color: Colors.cyan,
          icon: Icons.school_rounded,
        ),
        _buildMetricCard(
          title: 'Room Bookings',
          value: '${bookings.where((b) => b['status'] == 'CONFIRMED').length}',
          subtitle: 'Active reserved slots',
          color: Colors.amberAccent,
          icon: Icons.meeting_room_rounded,
        ),
        _buildMetricCard(
          title: 'Book Loans',
          value: '${loans.where((l) => l['status'] == 'BORROWED' || l['status'] == 'OVERDUE').length}',
          subtitle: 'Library books out',
          color: Colors.indigoAccent,
          icon: Icons.menu_book_rounded,
        ),
      ],
    );
  }

  // ADMIN Analytics Cards Grid
  Widget _buildAdminAnalyticsGrid() {
    final totalStudents = _dashData['totalStudents']?.toString() ?? '0';
    final totalEnrolments = _dashData['totalEnrolments']?.toString() ?? '0';
    final totalRoomBookings = _dashData['totalRoomBookings']?.toString() ?? '0';
    final totalBookLoans = _dashData['totalBookLoans']?.toString() ?? '0';

    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 16,
      mainAxisSpacing: 16,
      childAspectRatio: 1.35,
      children: [
        _buildMetricCard(
          title: 'Registered Students',
          value: totalStudents,
          subtitle: 'Total Database',
          color: Colors.blueAccent,
          icon: Icons.people_alt_rounded,
        ),
        _buildMetricCard(
          title: 'Course Enrolments',
          value: totalEnrolments,
          subtitle: 'Active registrations',
          color: Colors.cyan,
          icon: Icons.school_rounded,
        ),
        _buildMetricCard(
          title: 'Active Bookings',
          value: totalRoomBookings,
          subtitle: 'SOAP Room slots',
          color: Colors.amberAccent,
          icon: Icons.meeting_room_rounded,
        ),
        _buildMetricCard(
          title: 'Library Loans',
          value: totalBookLoans,
          subtitle: 'Books checked out',
          color: Colors.indigoAccent,
          icon: Icons.menu_book_rounded,
        ),
      ],
    );
  }

  Widget _buildMetricCard({
    required String title,
    required String value,
    required String subtitle,
    required Color color,
    required IconData icon,
  }) {
    return Card(
      color: const Color(0xFF1E1E1E),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: Colors.white10),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  title,
                  style: const TextStyle(color: Colors.white54, fontSize: 11, fontWeight: FontWeight.w600),
                ),
                Icon(icon, color: color.withOpacity(0.8), size: 18),
              ],
            ),
            Text(
              value,
              style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
            ),
            Text(
              subtitle,
              style: const TextStyle(color: Colors.white30, fontSize: 9),
            ),
          ],
        ),
      ),
    );
  }

  // Student details section (active bookings list and enrolled courses)
  Widget _buildStudentDetailSections() {
    final profile = _dashData['profile'] as Map? ?? {};
    final rawEnrolments = _dashData['enrolments'] as List? ?? [];
    final enrolments = rawEnrolments.where((e) => e['status'] == 'ACTIVE').toList();
    final bookings = _dashData['roomBookings'] as List? ?? [];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Academic Status Card
        Card(
          color: const Color(0xFF1E1E1E),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: const BorderSide(color: Colors.white10)),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Academic Identity Profile', style: TextStyle(color: Color(0xFF00BFA5), fontWeight: FontWeight.bold, fontSize: 14)),
                const Divider(color: Colors.white10, height: 20),
                _buildInfoRow('Programme', profile['programme'] ?? 'Bachelor of Computer Science'),
                const Divider(color: Colors.white10, height: 16),
                _buildInfoRow('Faculty / School', profile['faculty'] ?? 'FTMK'),
                const Divider(color: Colors.white10, height: 16),
                _buildInfoRow('Current Semester', 'Semester ${profile['semester'] ?? "1"}'),
                const Divider(color: Colors.white10, height: 16),
                _buildInfoRow('Registered Phone', profile['phoneNumber'] ?? 'N/A'),
              ],
            ),
          ),
        ),
        const SizedBox(height: 20),

        // Quick Courses list
        Card(
          color: const Color(0xFF1E1E1E),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: const BorderSide(color: Colors.white10)),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('My Enrolled Courses', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                    Text('${enrolments.length} Active', style: const TextStyle(color: Colors.white54, fontSize: 12)),
                  ],
                ),
                const Divider(color: Colors.white10, height: 20),
                enrolments.isEmpty
                    ? const Padding(
                        padding: EdgeInsets.symmetric(vertical: 12.0),
                        child: Center(child: Text('Not enrolled in any courses.', style: TextStyle(color: Colors.white38, fontSize: 13))),
                      )
                    : Column(
                        children: enrolments.map<Widget>((e) {
                          final course = e['course'] as Map? ?? {};
                          final String title = course['courseTitle'] ?? e['courseTitle'] ?? 'Unknown Course';
                          final String code = course['courseCode'] ?? e['courseCode'] ?? '';
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 12.0),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      Text(code, style: const TextStyle(color: Colors.white38, fontSize: 11, fontFamily: 'monospace')),
                                    ],
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                  decoration: BoxDecoration(color: Colors.cyan.withOpacity(0.12), borderRadius: BorderRadius.circular(6)),
                                  child: Text('${course['creditHours'] ?? 3} Credits', style: const TextStyle(color: Colors.cyan, fontSize: 11, fontWeight: FontWeight.bold)),
                                ),
                              ],
                            ),
                          );
                        }).toList(),
                      ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 20),

        // Quick Bookings list
        Card(
          color: const Color(0xFF1E1E1E),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: const BorderSide(color: Colors.white10)),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('Upcoming Room Bookings', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                    Text('${bookings.where((b) => b['status'] == 'CONFIRMED').length} Confirmed', style: const TextStyle(color: Colors.white54, fontSize: 12)),
                  ],
                ),
                const Divider(color: Colors.white10, height: 20),
                bookings.isEmpty
                    ? const Padding(
                        padding: EdgeInsets.symmetric(vertical: 12.0),
                        child: Center(child: Text('No active room slots reserved.', style: TextStyle(color: Colors.white38, fontSize: 13))),
                      )
                    : Column(
                        children: bookings.map<Widget>((b) {
                          final room = b['roomName'] ?? '';
                          final slot = b['slot'] ?? '';
                          final date = b['bookingDate']?.toString().split('T')[0] ?? '';
                          final isConfirmed = b['status'] == 'CONFIRMED';

                          return Padding(
                            padding: const EdgeInsets.only(bottom: 12.0),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(room, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      Text('Date: $date | Slot: $slot', style: const TextStyle(color: Colors.white38, fontSize: 12)),
                                    ],
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: isConfirmed ? const Color(0xFF00BFA5).withOpacity(0.12) : Colors.redAccent.withOpacity(0.12),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    b['status'] ?? 'CONFIRMED',
                                    style: TextStyle(
                                      color: isConfirmed ? const Color(0xFF00BFA5) : Colors.redAccent,
                                      fontSize: 10,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          );
                        }).toList(),
                      ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  // Admin overall audit listings
  Widget _buildAdminDetailSections() {
    final bookings = _dashData['allRoomBookings'] as List? ?? [];
    final loans = _dashData['allBookLoans'] as List? ?? [];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // ── Student Management Quick-Action Card ──
        Card(
          color: const Color(0xFF1E1E1E),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: BorderSide(color: const Color(0xFF3F51B5).withValues(alpha: 0.4)),
          ),
          child: InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const AdminStudentManagementScreen()),
            ),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFF3F51B5).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.manage_accounts_rounded, color: Color(0xFF3F51B5), size: 28),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Student Management', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
                        Text(
                          '${_dashData['totalStudents'] ?? 0} registered students',
                          style: const TextStyle(color: Colors.white54, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                  const Icon(Icons.arrow_forward_ios_rounded, color: Colors.white24, size: 16),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: 16),
        // Active Room Bookings Table
        Card(
          color: const Color(0xFF1E1E1E),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: const BorderSide(color: Colors.white10)),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('Global Room Bookings Ledger', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                    Text('${bookings.length} Total', style: const TextStyle(color: Colors.white54, fontSize: 12)),
                  ],
                ),
                const Divider(color: Colors.white10, height: 20),
                bookings.isEmpty
                    ? const Padding(
                        padding: EdgeInsets.symmetric(vertical: 12.0),
                        child: Center(child: Text('No campus room slots currently reserved.', style: TextStyle(color: Colors.white38, fontSize: 13))),
                      )
                    : ListView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        itemCount: bookings.length > 5 ? 5 : bookings.length,
                        itemBuilder: (ctx, idx) {
                          final booking = bookings[idx];
                          final room = booking['roomName'] ?? '';
                          final slot = booking['slot'] ?? '';
                          final date = booking['bookingDate']?.toString().split('T')[0] ?? '';
                          final studName = booking['studentName'] ?? 'N/A';
                          final isConfirmed = booking['status'] == 'CONFIRMED';

                          return Padding(
                            padding: const EdgeInsets.only(bottom: 12.0),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(room, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      Text('Stud: $studName | Date: $date | Slot: $slot', style: const TextStyle(color: Colors.white38, fontSize: 12)),
                                    ],
                                  ),
                                ),
                                Text(
                                  booking['status'] ?? 'CONFIRMED',
                                  style: TextStyle(
                                    color: isConfirmed ? const Color(0xFF00BFA5) : Colors.redAccent,
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 20),

        // Active Book Loans Table
        Card(
          color: const Color(0xFF1E1E1E),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: const BorderSide(color: Colors.white10)),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('Global Library Loans Audit', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                    Text('${loans.length} Total', style: const TextStyle(color: Colors.white54, fontSize: 12)),
                  ],
                ),
                const Divider(color: Colors.white10, height: 20),
                loans.isEmpty
                    ? const Padding(
                        padding: EdgeInsets.symmetric(vertical: 12.0),
                        child: Center(child: Text('No book loans currently registered.', style: TextStyle(color: Colors.white38, fontSize: 13))),
                      )
                    : ListView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        itemCount: loans.length > 5 ? 5 : loans.length,
                        itemBuilder: (ctx, idx) {
                          final loan = loans[idx];
                          final title = loan['bookTitle'] ?? 'Unknown Book';
                          final stud = loan['studentName'] ?? 'N/A';
                          final dueDate = loan['dueDate'] ?? '';
                          final isReturned = loan['status'] == 'RETURNED';

                          return Padding(
                            padding: const EdgeInsets.only(bottom: 12.0),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      Text('Stud: $stud | Due: $dueDate', style: const TextStyle(color: Colors.white38, fontSize: 12)),
                                    ],
                                  ),
                                ),
                                Text(
                                  loan['status'] ?? 'BORROWED',
                                  style: TextStyle(
                                    color: isReturned ? const Color(0xFF00BFA5) : (loan['status'] == 'OVERDUE' ? Colors.redAccent : Colors.white70),
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(color: Colors.white54, fontSize: 13)),
        Text(value, style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
      ],
    );
  }
}
