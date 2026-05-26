import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../main.dart';
import '../screens/dashboard/dashboard_screen.dart';
import '../screens/courses/course_enrolment_screen.dart';
import '../screens/rooms/room_booking_screen.dart';
import '../screens/notifications/notifications_screen.dart';
import '../screens/library/search_books_screen.dart';
import '../screens/library/admin_manage_books_screen.dart';
import '../screens/library/loan_history_screen.dart';
import '../screens/profile/admin_student_management_screen.dart';

class AppDrawer extends StatelessWidget {
  final String currentRoute;

  const AppDrawer({super.key, required this.currentRoute});

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final isAdmin = authProvider.isAdmin;
    final fullName = authProvider.fullName ?? 'User';
    final role = authProvider.role ?? 'STUDENT';
    final userId = authProvider.userId ?? 'N/A';

    return Drawer(
      backgroundColor: const Color(0xFF121212), // Matte Obsidian
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // ---- Header: User Identity & Badges ----
          UserAccountsDrawerHeader(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [Color(0xFF3F51B5), Color(0xFF303F9F)], // Deep Indigo
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
            ),
            currentAccountPicture: CircleAvatar(
              backgroundColor: Colors.white24,
              child: Text(
                fullName.isNotEmpty ? fullName[0].toUpperCase() : 'U',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            accountName: Text(
              fullName,
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
                color: Colors.white,
              ),
            ),
            accountEmail: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  userId,
                  style: const TextStyle(color: Colors.white70, fontSize: 13),
                ),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    role,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 1.1,
                    ),
                  ),
                ),
              ],
            ),
          ),

          // ---- Navigation Items ----
          Expanded(
            child: ListView(
              padding: EdgeInsets.zero,
              children: [
                _buildDrawerItem(
                  context: context,
                  icon: Icons.dashboard_rounded,
                  title: 'Dashboard & Analytics',
                  route: 'dashboard',
                  onTap: () => _navigate(context, const DashboardScreen()),
                ),
                _buildDrawerItem(
                  context: context,
                  icon: Icons.school_rounded,
                  title: 'Course Enrolment (REST)',
                  route: 'courses',
                  onTap: () =>
                      _navigate(context, const CourseEnrolmentScreen()),
                ),
                _buildDrawerItem(
                  context: context,
                  icon: Icons.meeting_room_rounded,
                  title: 'Room Bookings (SOAP)',
                  route: 'rooms',
                  onTap: () => _navigate(context, const RoomBookingScreen()),
                ),
                const Divider(color: Colors.white10),
                _buildDrawerItem(
                  context: context,
                  icon: Icons.menu_book_rounded,
                  title: 'Search Books Catalog',
                  route: 'search_books',
                  onTap: () => _navigate(context, const SearchBooksScreen()),
                ),
                _buildDrawerItem(
                  context: context,
                  icon: Icons.history_rounded,
                  title: isAdmin
                      ? 'Student Loan Lookup'
                      : 'My Borrowing History',
                  route: 'loan_history',
                  onTap: () => _navigate(context, const LoanHistoryScreen()),
                ),
                if (isAdmin)
                  _buildDrawerItem(
                    context: context,
                    icon: Icons.admin_panel_settings_rounded,
                    title: 'Library Manager Console',
                    route: 'library_admin',
                    onTap: () =>
                        _navigate(context, const AdminManageBooksScreen()),
                  ),
                if (isAdmin)
                  _buildDrawerItem(
                    context: context,
                    icon: Icons.manage_accounts_rounded,
                    title: 'Student Management',
                    route: 'students',
                    onTap: () => _navigate(
                      context,
                      const AdminStudentManagementScreen(),
                    ),
                  ),
                const Divider(color: Colors.white10),
                _buildDrawerItem(
                  context: context,
                  icon: Icons.notifications_active_rounded,
                  title: 'Notifications Log',
                  route: 'notifications',
                  onTap: () => _navigate(context, const NotificationsScreen()),
                ),
              ],
            ),
          ),

          // ---- Bottom Footer: Logout ----
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF1E1E1E),
                foregroundColor: Colors.redAccent,
                side: const BorderSide(color: Colors.redAccent, width: 0.5),
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              icon: const Icon(Icons.logout_rounded, size: 18),
              label: const Text(
                'LOGOUT',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1.1,
                ),
              ),
              onPressed: () => _showLogoutConfirmDialog(context),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDrawerItem({
    required BuildContext context,
    required IconData icon,
    required String title,
    required String route,
    required VoidCallback onTap,
  }) {
    final isSelected = currentRoute == route;
    return ListTile(
      leading: Icon(
        icon,
        color: isSelected
            ? const Color(0xFF00BFA5)
            : Colors.white70, // Mint green for selected
      ),
      title: Text(
        title,
        style: TextStyle(
          color: isSelected ? Colors.white : Colors.white70,
          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
        ),
      ),
      selected: isSelected,
      selectedTileColor: const Color(0xFF00BFA5).withOpacity(0.08),
      onTap: onTap,
    );
  }

  void _navigate(BuildContext context, Widget screen) {
    Navigator.of(
      context,
    ).pushReplacement(MaterialPageRoute(builder: (_) => screen));
  }

  void _showLogoutConfirmDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        title: const Text(
          'Confirm Logout',
          style: TextStyle(color: Colors.white),
        ),
        content: const Text(
          'Are you sure you want to end your session?',
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text(
              'Cancel',
              style: TextStyle(color: Colors.white54),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () {
              Navigator.of(ctx).pop();
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const AuthGate()),
                (route) => false,
              );
              context.read<AuthProvider>().logout();
            },
            child: const Text('Logout', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}
