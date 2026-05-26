import 'api_service.dart';

class AuthService {
  /**
   * Logs in a user using their Matric ID or ADMIN keyword.
   * Returns a Map containing: token, userId, role, fullName, expiresAt.
   */
  static Future<Map<String, dynamic>> login(String userId) async {
    final response = await ApiService.post('/auth/login', {
      'userId': userId.trim(),
    });
    return Map<String, dynamic>.from(response);
  }

  /**
   * Verifies the active session token and returns the current user profile.
   */
  static Future<Map<String, dynamic>> getMe() async {
    final response = await ApiService.get('/auth/me');
    return Map<String, dynamic>.from(response);
  }

  /**
   * Terminates the session token on the backend server.
   */
  static Future<void> logout() async {
    await ApiService.post('/auth/logout', null);
  }
}
