import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/auth_service.dart';

class AuthProvider with ChangeNotifier {
  String? _token;
  String? _userId;
  String? _role;
  String? _fullName;
  bool _isLoading = false;
  String? _errorMessage;

  // ---- Getters ----
  bool get isAuthenticated => _token != null;
  bool get isAdmin => _role == 'ADMIN';
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String? get token => _token;
  String? get userId => _userId;
  String? get role => _role;
  String? get fullName => _fullName;

  /**
   * Attempt auto-login using persisted credentials in SharedPreferences on startup.
   */
  Future<bool> tryAutoLogin() async {
    final prefs = await SharedPreferences.getInstance();
    if (!prefs.containsKey('auth_token')) return false;

    _token = prefs.getString('auth_token');
    _userId = prefs.getString('auth_userId');
    _role = prefs.getString('auth_role');
    _fullName = prefs.getString('auth_fullName');

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      // Validate session token with backend /auth/me
      final me = await AuthService.getMe();
      _userId = me['userId'];
      _role = me['role'];
      _fullName = me['fullName'];

      // Persist verified values
      await prefs.setString('auth_userId', _userId!);
      await prefs.setString('auth_role', _role!);
      await prefs.setString('auth_fullName', _fullName!);

      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      // Invalid/expired token -> clear session
      await clearSession();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  /**
   * Performs authentication request using student Matric ID or ADMIN keyword.
   */
  Future<bool> login(String inputUserId) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final data = await AuthService.login(inputUserId);
      
      _token = data['token'];
      _userId = data['userId'];
      _role = data['role'];
      _fullName = data['fullName'];

      // Save to SharedPreferences
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('auth_token', _token!);
      await prefs.setString('auth_userId', _userId!);
      await prefs.setString('auth_role', _role!);
      await prefs.setString('auth_fullName', _fullName!);

      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll("Exception: ", "");
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  /**
   * Logs out the user and clears SharedPreferences.
   */
  Future<void> logout() async {
    _isLoading = true;
    notifyListeners();

    try {
      // Try calling logout endpoint on server (best effort)
      await AuthService.logout();
    } catch (e) {
      // Ignore network errors on logout since we want to clear local state anyway
    }

    await clearSession();
    _isLoading = false;
    notifyListeners();
  }

  /**
   * Private helper to completely wipe local storage values.
   */
  Future<void> clearSession() async {
    _token = null;
    _userId = null;
    _role = null;
    _fullName = null;
    _errorMessage = null;

    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('auth_token');
    await prefs.remove('auth_userId');
    await prefs.remove('auth_role');
    await prefs.remove('auth_fullName');
  }
}
