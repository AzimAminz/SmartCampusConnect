import 'api_service.dart';
import '../models/course.dart';

class CourseService {
  /// Fetches all courses in the system.
  static Future<List<Course>> getCourses() async {
    final response = await ApiService.get('/courses');
    if (response is List) {
      return response.map((item) => Course.fromJson(Map<String, dynamic>.from(item))).toList();
    }
    return [];
  }

  /// Enrolls a student in a course by code.
  static Future<Map<String, dynamic>> enrollCourse({
    required int studentId,
    required String courseCode,
  }) async {
    final body = {
      'studentId': studentId,
      'courseCode': courseCode,
    };
    final response = await ApiService.post('/enrol', body);
    if (response is Map) {
      return Map<String, dynamic>.from(response);
    }
    throw Exception('Unexpected enrollment response format.');
  }

  /// Drops a student's enrolled course by code.
  static Future<Map<String, dynamic>> dropCourse({
    required int studentId,
    required String courseCode,
  }) async {
    final response = await ApiService.delete('/enrol/$studentId/$courseCode');
    if (response is Map) {
      return Map<String, dynamic>.from(response);
    }
    throw Exception('Unexpected course dropping response format.');
  }

  /// Creates a new course (Admin only).
  static Future<Course> addCourse(Map<String, dynamic> courseData) async {
    final response = await ApiService.post('/courses', courseData);
    if (response is Map) {
      if (response.containsKey('error')) {
        throw Exception(response['error']);
      }
      return Course.fromJson(Map<String, dynamic>.from(response));
    }
    throw Exception('Unexpected add course response format.');
  }

  /// Updates course details (Admin only).
  static Future<Course> updateCourse(int id, Map<String, dynamic> courseData) async {
    final response = await ApiService.put('/courses/$id', courseData);
    if (response is Map) {
      if (response.containsKey('error')) {
        throw Exception(response['error']);
      }
      return Course.fromJson(Map<String, dynamic>.from(response));
    }
    throw Exception('Unexpected update course response format.');
  }

  /// Deletes a course from the system (Admin only).
  static Future<void> deleteCourse(int id) async {
    final response = await ApiService.delete('/courses/$id');
    if (response is Map && response.containsKey('error')) {
      throw Exception(response['error']);
    }
  }
}
