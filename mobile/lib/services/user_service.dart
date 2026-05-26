import 'api_service.dart';

class UserService {
  /**
   * Fetches personalized student dashboard data (profile, enrolments, bookings, loans, notifications)
   * or administrator summary stats based on the authenticated session token.
   */
  static Future<Map<String, dynamic>> getDashboard() async {
    final response = await ApiService.get('/dashboard');
    return Map<String, dynamic>.from(response);
  }
}
