import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../services/user_service.dart';
import '../../services/course_service.dart';
import '../../widgets/app_drawer.dart';
import '../../models/course.dart';
import 'widgets/course_card.dart';
import 'widgets/student_enrolment_header.dart';
import 'widgets/course_dialogs.dart';

class CourseEnrolmentScreen extends StatefulWidget {
  const CourseEnrolmentScreen({super.key});

  @override
  State<CourseEnrolmentScreen> createState() => _CourseEnrolmentScreenState();
}

class _CourseEnrolmentScreenState extends State<CourseEnrolmentScreen> {
  List<Course> _allCourses = [];
  List<dynamic> _myEnrolments = [];
  Map<String, dynamic>? _studentProfile;
  bool _isLoading = false;
  String? _error;
  int? _studentDbId; // Student's DB primary key Long

  String _searchQuery = '';
  String _enrolmentFilter = 'ALL'; // 'ALL', 'ENROLLED', 'NOT_ENROLLED'
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _fetchData();
  }

  Future<void> _fetchData() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final auth = context.read<AuthProvider>();

      // 1. Fetch all courses in the system using CourseService
      final coursesResponse = await CourseService.getCourses();

      // 2. Fetch student dashboard data if student
      List<dynamic> myEnrolments = [];
      Map<String, dynamic>? studentProfile;
      int? studentDbId;

      if (auth.role == 'STUDENT') {
        final dash = await UserService.getDashboard();
        studentProfile = dash['profile'];
        studentDbId = studentProfile?['id'];
        final rawEnrolments = dash['enrolments'] as List? ?? [];
        myEnrolments = rawEnrolments
            .where((e) => e['status'] == 'ACTIVE')
            .toList();
      }

      if (!mounted) return;
      setState(() {
        _allCourses = coursesResponse;
        _myEnrolments = myEnrolments;
        _studentProfile = studentProfile;
        _studentDbId = studentDbId;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _isLoading = false;
      });
    }
  }

  // Check if student is already enrolled in this course code
  bool _isEnrolled(String courseCode) {
    return _myEnrolments.any((e) {
      if (e is Map) {
        String? code;
        if (e.containsKey('course') && e['course'] is Map) {
          code = e['course']['courseCode']?.toString();
        } else {
          code = e['courseCode']?.toString();
        }
        final isActive = e['status'] == 'ACTIVE';
        return code == courseCode && isActive;
      }
      return false;
    });
  }

  // Handle student Enroll Course
  Future<void> _enrollCourse(String courseCode) async {
    if (_studentDbId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Error: Student Profile database ID not loaded.'),
          backgroundColor: Colors.redAccent,
        ),
      );
      return;
    }

    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: Colors.white10),
        ),
        title: Row(
          children: const [
            Icon(Icons.school_rounded, color: Color(0xFF00BFA5)),
            SizedBox(width: 10),
            Text(
              'Confirm Enrolment',
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
        content: Text(
          'Are you sure you want to enrol in course $courseCode?',
          style: const TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(
              'Cancel',
              style: TextStyle(color: Colors.white54),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF3F51B5),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(
              'Confirm',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    if (!mounted) return;
    setState(() => _isLoading = true);
    try {
      final response = await CourseService.enrollCourse(
        studentId: _studentDbId!,
        courseCode: courseCode,
      );
      if (!mounted) return;
      if (response['success'] == true) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(response['message'] ?? 'Successfully enrolled!'),
            backgroundColor: const Color(0xFF00BFA5),
          ),
        );
      } else if (response.containsKey('error')) {
        throw Exception(response['error']);
      } else {
        throw Exception(
          'Enrolment failed. Course might be full or credits exceeded.',
        );
      }
      _fetchData();
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text(
            'Enrolment Failed',
            style: TextStyle(color: Colors.white),
          ),
          content: Text(
            e.toString().replaceAll("Exception: ", ""),
            style: const TextStyle(color: Colors.white70),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              style: TextButton.styleFrom(foregroundColor: Colors.white),
              child: const Text('Close'),
            ),
          ],
        ),
      );
    }
  }

  // Handle student Drop Course
  Future<void> _dropCourse(String courseCode) async {
    if (_studentDbId == null) return;

    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        title: const Text('Drop Course', style: TextStyle(color: Colors.white)),
        content: Text(
          'Are you sure you want to drop course $courseCode?',
          style: const TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(
              'Cancel',
              style: TextStyle(color: Colors.white54),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Drop', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    if (!mounted) return;
    setState(() => _isLoading = true);
    try {
      final response = await CourseService.dropCourse(
        studentId: _studentDbId!,
        courseCode: courseCode,
      );
      if (!mounted) return;
      if (response['success'] == true) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              response['message'] ?? 'Successfully dropped course!',
            ),
            backgroundColor: const Color(0xFF00BFA5),
          ),
        );
      } else if (response.containsKey('error')) {
        throw Exception(response['error']);
      }
      _fetchData();
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  // Handle Admin Add New Course
  void _showAddCourseDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AddCourseDialog(
        onAdd: (courseData) async {
          await CourseService.addCourse(courseData);
          _fetchData();
        },
      ),
    );
  }

  // Handle Admin Edit Course Details
  void _showEditCourseDialog(Course course) {
    showDialog(
      context: context,
      builder: (ctx) => EditCourseDialog(
        course: course,
        onSave: (courseData) async {
          await CourseService.updateCourse(course.id, courseData);
          _fetchData();
        },
      ),
    );
  }

  // Handle Admin Delete Course
  Future<void> _deleteCourse(int id, String courseCode) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        title: const Text(
          'Delete Course',
          style: TextStyle(color: Colors.white),
        ),
        content: Text(
          'Are you sure you want to permanently delete course $courseCode?',
          style: const TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(
              'Cancel',
              style: TextStyle(color: Colors.white54),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Delete', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    if (!mounted) return;
    setState(() => _isLoading = true);
    try {
      await CourseService.deleteCourse(id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Course deleted successfully!'),
          backgroundColor: Color(0xFF00BFA5),
        ),
      );
      _fetchData();
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final isStudent = auth.role == 'STUDENT';

    var filteredCourses = _allCourses.where((course) {
      final query = _searchQuery.toLowerCase();
      if (query.isEmpty) return true;
      final code = course.courseCode.toLowerCase();
      final title = course.courseTitle.toLowerCase();
      final lecturer = course.lecturer.toLowerCase();
      final faculty = (course.faculty ?? '').toLowerCase();

      return code.contains(query) ||
          title.contains(query) ||
          lecturer.contains(query) ||
          faculty.contains(query);
    }).toList();

    if (isStudent) {
      if (_enrolmentFilter == 'ENROLLED') {
        filteredCourses = filteredCourses
            .where((course) => _isEnrolled(course.courseCode))
            .toList();
      } else if (_enrolmentFilter == 'NOT_ENROLLED') {
        filteredCourses = filteredCourses
            .where((course) => !_isEnrolled(course.courseCode))
            .toList();
      }
    }

    return Scaffold(
      backgroundColor: const Color(0xFF121212), // Matte Obsidian
      appBar: AppBar(
        title: const Text('Course Enrolment Center'),
        backgroundColor: const Color(0xFF1E1E1E),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _fetchData,
          ),
        ],
      ),
      drawer: const AppDrawer(currentRoute: 'courses'),
      floatingActionButton: !isStudent
          ? FloatingActionButton(
              backgroundColor: const Color(0xFF00BFA5),
              onPressed: _showAddCourseDialog,
              child: const Icon(
                Icons.add_rounded,
                color: Colors.white,
                size: 28,
              ),
            )
          : null,
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFF00BFA5)),
            )
          : Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // ---- Header Profile (Only for Student) ----
                  if (isStudent && _studentProfile != null) ...[
                    StudentEnrolmentHeader(
                      studentProfile: _studentProfile!,
                      fullName: auth.fullName ?? '',
                      activeCoursesCount: _myEnrolments.length,
                    ),
                    const SizedBox(height: 16),
                  ],

                  // ---- Error Alert Box ----
                  if (_error != null) ...[
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.redAccent.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: Colors.redAccent.withOpacity(0.3),
                        ),
                      ),
                      child: Text(
                        _error!,
                        style: const TextStyle(
                          color: Colors.redAccent,
                          fontSize: 13,
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                  ],

                  // ---- Search Bar ----
                  TextField(
                    controller: _searchController,
                    style: const TextStyle(color: Colors.white),
                    onChanged: (val) {
                      setState(() {
                        _searchQuery = val.trim();
                      });
                    },
                    decoration: InputDecoration(
                      hintText: 'Search course code, title, lecturer...',
                      hintStyle: const TextStyle(color: Colors.white38),
                      prefixIcon: const Icon(
                        Icons.search_rounded,
                        color: Color(0xFF00BFA5),
                      ),
                      suffixIcon: _searchQuery.isNotEmpty
                          ? IconButton(
                              icon: const Icon(
                                Icons.clear_rounded,
                                color: Colors.white54,
                              ),
                              onPressed: () {
                                _searchController.clear();
                                setState(() {
                                  _searchQuery = '';
                                });
                              },
                            )
                          : null,
                      filled: true,
                      fillColor: Colors.white.withOpacity(0.03),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 12,
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: const BorderSide(color: Colors.white10),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: const BorderSide(color: Colors.white10),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: const BorderSide(
                          color: Color(0xFF00BFA5),
                          width: 1.0,
                        ),
                      ),
                    ),
                  ),
                  // ---- Enrolment Status Filter (Only for Student) ----
                  if (isStudent) ...[
                    const SizedBox(height: 12),
                    SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: [
                          _buildFilterChip(
                            label: 'All Courses (${_allCourses.length})',
                            isSelected: _enrolmentFilter == 'ALL',
                            onSelected: () =>
                                setState(() => _enrolmentFilter = 'ALL'),
                          ),
                          const SizedBox(width: 8),
                          _buildFilterChip(
                            label: 'Enrolled (${_myEnrolments.length})',
                            isSelected: _enrolmentFilter == 'ENROLLED',
                            onSelected: () =>
                                setState(() => _enrolmentFilter = 'ENROLLED'),
                            activeColor: const Color(0xFF00BFA5),
                          ),
                          const SizedBox(width: 8),
                          _buildFilterChip(
                            label:
                                'Not Enrolled (${_allCourses.length - _myEnrolments.length})',
                            isSelected: _enrolmentFilter == 'NOT_ENROLLED',
                            onSelected: () => setState(
                              () => _enrolmentFilter = 'NOT_ENROLLED',
                            ),
                            activeColor: Colors.amberAccent,
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 16),

                  // ---- Section Title ----
                  Text(
                    isStudent
                        ? 'Available Courses to Enrol'
                        : 'Active Course Catalog',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 12),

                  // ---- Courses List ----
                  Expanded(
                    child: filteredCourses.isEmpty
                        ? Center(
                            child: Text(
                              _searchQuery.isNotEmpty
                                  ? 'No courses matching search query.'
                                  : 'No courses available in the catalog.',
                              style: const TextStyle(
                                color: Colors.white38,
                                fontSize: 14,
                              ),
                            ),
                          )
                        : ListView.builder(
                            itemCount: filteredCourses.length,
                            itemBuilder: (context, index) {
                              final course = filteredCourses[index];
                              final enrolledStatus = _isEnrolled(course.courseCode);

                              return CourseCard(
                                course: course,
                                isStudent: isStudent,
                                enrolledStatus: enrolledStatus,
                                onEnroll: () => _enrollCourse(course.courseCode),
                                onDrop: () => _dropCourse(course.courseCode),
                                onEdit: () => _showEditCourseDialog(course),
                                onDelete: () => _deleteCourse(course.id, course.courseCode),
                              );
                            },
                          ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildFilterChip({
    required String label,
    required bool isSelected,
    required VoidCallback onSelected,
    Color activeColor = const Color(0xFF3F51B5),
  }) {
    return GestureDetector(
      onTap: onSelected,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected
              ? activeColor.withOpacity(0.15)
              : const Color(0xFF1E1E1E),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isSelected ? activeColor : Colors.white10,
            width: isSelected ? 1.2 : 1.0,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? activeColor : Colors.white60,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}
