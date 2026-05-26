class Student {
  final int id;
  final String studentId;
  final String name;
  final String email;
  final String? programme;
  final String? faculty;
  final String? semester;
  final double gpa;
  final String? phoneNumber;
  final String? status;

  Student({
    required this.id,
    required this.studentId,
    required this.name,
    required this.email,
    this.programme,
    this.faculty,
    this.semester,
    required this.gpa,
    this.phoneNumber,
    this.status,
  });

  factory Student.fromJson(Map<String, dynamic> json) {
    return Student(
      id: json['id'] as int? ?? 0,
      studentId: json['studentId'] as String? ?? '',
      name: json['name'] as String? ?? '',
      email: json['email'] as String? ?? '',
      programme: json['programme'] as String?,
      faculty: json['faculty'] as String?,
      semester: json['semester']?.toString(),
      gpa: (json['gpa'] as num? ?? 0.0).toDouble(),
      phoneNumber: json['phoneNumber'] as String?,
      status: json['status'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'studentId': studentId,
      'name': name,
      'email': email,
      'programme': programme,
      'faculty': faculty,
      'semester': semester,
      'gpa': gpa,
      'phoneNumber': phoneNumber,
      'status': status,
    };
  }
}
