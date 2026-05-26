import 'course.dart';

class Enrolment {
  final int id;
  final int studentId;
  final String courseCode;
  final String? enrolmentDate;
  final String? semester;
  final String status; // 'ACTIVE', 'DROPPED', etc.
  final Course? course;

  Enrolment({
    required this.id,
    required this.studentId,
    required this.courseCode,
    this.enrolmentDate,
    this.semester,
    required this.status,
    this.course,
  });

  factory Enrolment.fromJson(Map<String, dynamic> json) {
    Course? courseObj;
    if (json['course'] != null && json['course'] is Map) {
      courseObj = Course.fromJson(Map<String, dynamic>.from(json['course']));
    }

    return Enrolment(
      id: json['id'] as int? ?? 0,
      studentId: json['studentId'] as int? ?? 0,
      courseCode: json['courseCode'] as String? ?? (courseObj?.courseCode ?? ''),
      enrolmentDate: json['enrolmentDate'] as String?,
      semester: json['semester']?.toString(),
      status: json['status'] as String? ?? 'ACTIVE',
      course: courseObj,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'studentId': studentId,
      'courseCode': courseCode,
      'enrolmentDate': enrolmentDate,
      'semester': semester,
      'status': status,
      'course': course?.toJson(),
    };
  }
}
