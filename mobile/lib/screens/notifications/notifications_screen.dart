import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../services/api_service.dart';
import '../../widgets/app_drawer.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  List<dynamic> _notifications = [];
  bool _isLoading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchNotifications();
  }

  Future<void> _fetchNotifications() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final auth = context.read<AuthProvider>();
      final isStudent = auth.role == 'STUDENT';
      
      final endpoint = isStudent 
          ? '/notifications/recipient/${auth.userId}' 
          : '/notifications';

      final response = await ApiService.get(endpoint) as List;
      
      setState(() {
        // Sort descending by created time (latest first) if id/timestamp is present
        _notifications = response.reversed.toList();
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _isLoading = false;
      });
    }
  }

  IconData _getIcon(String type) {
    switch (type.toUpperCase()) {
      case 'ROOM_BOOKED':
        return Icons.meeting_room_rounded;
      case 'ROOM_CANCELLED':
        return Icons.cancel_rounded;
      case 'BOOK_BORROWED':
        return Icons.menu_book_rounded;
      case 'BOOK_RETURNED':
        return Icons.keyboard_return_rounded;
      case 'ENROLMENT_SUCCESS':
        return Icons.school_rounded;
      case 'SYSTEM_ALERT':
      default:
        return Icons.notifications_active_rounded;
    }
  }

  Color _getColor(String type) {
    switch (type.toUpperCase()) {
      case 'ROOM_BOOKED':
        return const Color(0xFF00BFA5); // Mint Green
      case 'ROOM_CANCELLED':
        return Colors.redAccent;
      case 'BOOK_BORROWED':
        return const Color(0xFF3F51B5); // Deep Indigo
      case 'BOOK_RETURNED':
        return Colors.green;
      case 'ENROLMENT_SUCCESS':
        return Colors.cyan;
      case 'SYSTEM_ALERT':
      default:
        return Colors.amberAccent;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212), // Matte Obsidian
      appBar: AppBar(
        title: const Text('Notifications Log'),
        backgroundColor: const Color(0xFF1E1E1E),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _fetchNotifications,
          ),
        ],
      ),
      drawer: const AppDrawer(currentRoute: 'notifications'),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF00BFA5)))
          : Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Header description
                  const Text(
                    'Audit log trail of all transaction notifications dispatched by our microservices.',
                    style: TextStyle(color: Colors.white54, fontSize: 13),
                  ),
                  const SizedBox(height: 16),

                  // Error Alert Box
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
                    const SizedBox(height: 12),
                  ],

                  // Notifications list
                  Expanded(
                    child: _notifications.isEmpty
                        ? const Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.notifications_off_rounded, size: 60, color: Colors.white24),
                                SizedBox(height: 16),
                                Text(
                                  'Your notification ledger is empty.',
                                  style: TextStyle(color: Colors.white38, fontSize: 14),
                                ),
                              ],
                            ),
                          )
                        : ListView.builder(
                            itemCount: _notifications.length,
                            itemBuilder: (context, index) {
                              final notif = _notifications[index];
                              final String type = notif['type'] ?? 'SYSTEM_ALERT';
                              final String body = notif['message'] ?? 'No description';
                              final String ref = notif['referenceParameter'] ?? 'N/A';
                              final String recipient = notif['recipientId'] ?? 'N/A';
                              final String studentName = notif['studentName'] ?? 'N/A';
                              final String date = notif['createdAt'] != null 
                                  ? notif['createdAt'].toString().replaceAll('T', ' ').substring(0, 19) 
                                  : 'N/A';

                              final iconColor = _getColor(type);
                              final icon = _getIcon(type);

                              return Card(
                                color: const Color(0xFF1E1E1E),
                                margin: const EdgeInsets.only(bottom: 12),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  side: const BorderSide(color: Colors.white10),
                                ),
                                child: ListTile(
                                  contentPadding: const EdgeInsets.all(16),
                                  leading: Container(
                                    padding: const EdgeInsets.all(8),
                                    decoration: BoxDecoration(
                                      color: iconColor.withOpacity(0.1),
                                      shape: BoxShape.circle,
                                    ),
                                    child: Icon(icon, color: iconColor, size: 24),
                                  ),
                                  title: Text(
                                    body,
                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14),
                                  ),
                                  subtitle: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      const SizedBox(height: 8),
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Expanded(
                                            child: Text(
                                              'Type: ${type.replaceAll('_', ' ')}',
                                              style: TextStyle(color: iconColor, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.5),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ),
                                          const SizedBox(width: 8),
                                          Text(
                                            date,
                                            style: const TextStyle(color: Colors.white38, fontSize: 11),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 4),
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Text(
                                            'Ref: $ref',
                                            style: const TextStyle(color: Colors.white24, fontSize: 11, fontFamily: 'monospace'),
                                          ),
                                          const SizedBox(width: 8),
                                          Expanded(
                                            child: Text(
                                              'Matric: $recipient ($studentName)',
                                              style: const TextStyle(color: Colors.white38, fontSize: 11),
                                              textAlign: TextAlign.end,
                                              overflow: TextOverflow.ellipsis,
                                            ),
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
    );
  }
}
