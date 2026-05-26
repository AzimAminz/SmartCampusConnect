import 'api_service.dart';
import '../models/student.dart';

class StudentService {
  /// Fetches all student records.
  static Future<List<Student>> getStudents() async {
    final response = await ApiService.get('/students');
    if (response is List) {
      return response.map((item) => Student.fromJson(Map<String, dynamic>.from(item))).toList();
    }
    return [];
  }

  /// Adds a new student record (Admin only).
  static Future<Student> addStudent(Map<String, dynamic> studentData) async {
    final response = await ApiService.post('/students', studentData);
    if (response is Map) {
      if (response.containsKey('error')) {
        throw Exception(response['error']);
      }
      return Student.fromJson(Map<String, dynamic>.from(response));
    }
    throw Exception('Unexpected response format when adding student.');
  }

  /// Updates an existing student record (Admin only).
  static Future<Student> updateStudent(int id, Map<String, dynamic> studentData) async {
    final response = await ApiService.put('/students/$id', studentData);
    if (response is Map) {
      if (response.containsKey('error')) {
        throw Exception(response['error']);
      }
      return Student.fromJson(Map<String, dynamic>.from(response));
    }
    throw Exception('Unexpected response format when updating student.');
  }

  /// Deletes a student record (Admin only).
  static Future<void> deleteStudent(int id) async {
    final response = await ApiService.delete('/students/$id');
    if (response is Map && response.containsKey('error')) {
      throw Exception(response['error']);
    }
  }
}
