import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiService {
  static const String baseUrl = "http://localhost:8080/api";

  /**
   * Helper to retrieve headers, automatically injecting the auth token if present.
   */
  static Future<Map<String, String>> _getHeaders() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('auth_token');

    final headers = {
      'Content-Type': 'application/json; charset=utf-8',
      'Accept': 'application/json',
    };

    if (token != null && token.isNotEmpty) {
      headers['X-Auth-Token'] = token;
    }

    return headers;
  }

  /**
   * Centralized JSON response handler.
   */
  static dynamic _handleResponse(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return null;
      return json.decode(utf8.decode(response.bodyBytes));
    } else {
      String errMsg = "Request failed";
      try {
        final decoded = json.decode(utf8.decode(response.bodyBytes));
        if (decoded is Map && decoded.containsKey('error')) {
          errMsg = decoded['error'];
        }
      } catch (e) {
        // Fallback to general status message
        errMsg = "Server returned code ${response.statusCode}";
      }
      throw Exception(errMsg);
    }
  }

  /**
   * GET Request
   */
  static Future<dynamic> get(String endpoint) async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl$endpoint'),
      headers: headers,
    );
    return _handleResponse(response);
  }

  /**
   * POST Request
   */
  static Future<dynamic> post(
    String endpoint,
    Map<String, dynamic>? body,
  ) async {
    final headers = await _getHeaders();
    final response = await http.post(
      Uri.parse('$baseUrl$endpoint'),
      headers: headers,
      body: body != null ? json.encode(body) : null,
    );
    return _handleResponse(response);
  }

  /**
   * PUT Request
   */
  static Future<dynamic> put(String endpoint, Map<String, dynamic> body) async {
    final headers = await _getHeaders();
    final response = await http.put(
      Uri.parse('$baseUrl$endpoint'),
      headers: headers,
      body: json.encode(body),
    );

    return _handleResponse(response);
  }

  /**
   * DELETE Request
   */
  static Future<dynamic> delete(String endpoint) async {
    final headers = await _getHeaders();
    final response = await http.delete(
      Uri.parse('$baseUrl$endpoint'),
      headers: headers,
    );
    return _handleResponse(response);
  }
}
