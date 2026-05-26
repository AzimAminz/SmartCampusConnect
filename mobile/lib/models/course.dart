class Course {
  final int id;
  final String courseCode;
  final String courseTitle;
  final String lecturer;
  final String? faculty;
  final int creditHours;
  final int maxCapacity;
  final int currentCapacity;
  final int enrolledCount;
  final String semester;

  Course({
    required this.id,
    required this.courseCode,
    required this.courseTitle,
    required this.lecturer,
    this.faculty,
    required this.creditHours,
    required this.maxCapacity,
    required this.currentCapacity,
    required this.enrolledCount,
    required this.semester,
  });

  factory Course.fromJson(Map<String, dynamic> json) {
    return Course(
      id: json['id'] as int? ?? 0,
      courseCode: json['courseCode'] as String? ?? '',
      courseTitle: json['courseTitle'] as String? ?? '',
      lecturer: json['lecturer'] as String? ?? '',
      faculty: json['faculty'] as String?,
      creditHours: json['creditHours'] as int? ?? 3,
      maxCapacity: json['maxCapacity'] as int? ?? 30,
      currentCapacity: json['currentCapacity'] as int? ?? 0,
      enrolledCount: json['enrolledCount'] as int? ?? 0,
      semester: json['semester']?.toString() ?? '1',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'courseCode': courseCode,
      'courseTitle': courseTitle,
      'lecturer': lecturer,
      'faculty': faculty,
      'creditHours': creditHours,
      'maxCapacity': maxCapacity,
      'currentCapacity': currentCapacity,
      'enrolledCount': enrolledCount,
      'semester': semester,
    };
  }
}
