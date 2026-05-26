import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../services/api_service.dart';
import '../../services/user_service.dart';
import '../../widgets/app_drawer.dart';

class CourseEnrolmentScreen extends StatefulWidget {
  const CourseEnrolmentScreen({super.key});

  @override
  State<CourseEnrolmentScreen> createState() => _CourseEnrolmentScreenState();
}

class _CourseEnrolmentScreenState extends State<CourseEnrolmentScreen> {
  List<dynamic> _allCourses = [];
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
      
      // 1. Fetch all courses in the system
      final coursesResponse = await ApiService.get('/courses') as List;
      
      // 2. Fetch student dashboard data if student
      List<dynamic> myEnrolments = [];
      Map<String, dynamic>? studentProfile;
      int? studentDbId;

      if (auth.role == 'STUDENT') {
        final dash = await UserService.getDashboard();
        studentProfile = dash['profile'];
        studentDbId = studentProfile?['id'];
        final rawEnrolments = dash['enrolments'] as List? ?? [];
        myEnrolments = rawEnrolments.where((e) => e['status'] == 'ACTIVE').toList();
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
        const SnackBar(content: Text('Error: Student Profile database ID not loaded.'), backgroundColor: Colors.redAccent),
      );
      return;
    }

    // Confirm Box
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
            Text('Confirm Enrolment', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ],
        ),
        content: Text('Are you sure you want to enrol in course $courseCode?', style: const TextStyle(color: Colors.white70)),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel', style: TextStyle(color: Colors.white54)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF3F51B5),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Confirm', style: TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    if (!mounted) return;
    setState(() => _isLoading = true);
    try {
      final body = {
        'studentId': _studentDbId,
        'courseCode': courseCode,
      };

      final response = await ApiService.post('/enrol', body);
      if (!mounted) return;
      if (response is Map && response['success'] == true) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(response['message'] ?? 'Successfully enrolled!'), backgroundColor: const Color(0xFF00BFA5)),
        );
      } else if (response is Map && response.containsKey('error')) {
        throw Exception(response['error']);
      } else {
        throw Exception('Enrolment failed. Course might be full or credits exceeded.');
      }
      _fetchData(); // Refresh list and active enrollments
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text('Enrolment Failed', style: TextStyle(color: Colors.white)),
          content: Text(e.toString().replaceAll("Exception: ", ""), style: const TextStyle(color: Colors.white70)),
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
        content: Text('Are you sure you want to drop course $courseCode?', style: const TextStyle(color: Colors.white70)),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel', style: TextStyle(color: Colors.white54)),
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
      final response = await ApiService.delete('/enrol/$_studentDbId/$courseCode');
      if (!mounted) return;
      if (response is Map && response['success'] == true) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(response['message'] ?? 'Successfully dropped course!'), backgroundColor: const Color(0xFF00BFA5)),
        );
      } else if (response is Map && response.containsKey('error')) {
        throw Exception(response['error']);
      }
      _fetchData();
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'), backgroundColor: Colors.redAccent),
      );
    }
  }

  // Handle Admin Add New Course
  void _showAddCourseDialog() {
    final codeController = TextEditingController();
    final titleController = TextEditingController();
    final lecturerController = TextEditingController();
    final facultyController = TextEditingController();
    final creditsController = TextEditingController();
    final capacityController = TextEditingController();
    final semesterController = TextEditingController(text: "1");

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setStateDialog) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text('Register New Course', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: codeController,
                  decoration: const InputDecoration(labelText: 'Course Code (e.g. BITP3123)', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: titleController,
                  decoration: const InputDecoration(labelText: 'Course Title', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: lecturerController,
                  decoration: const InputDecoration(labelText: 'Lecturer Name', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: facultyController,
                  decoration: const InputDecoration(labelText: 'Faculty (e.g. FTMK)', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: creditsController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Credit Hours', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: capacityController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Max Capacity / Seats', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: semesterController,
                  decoration: const InputDecoration(labelText: 'Semester', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancel', style: TextStyle(color: Colors.white54)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF3F51B5)),
              onPressed: () async {
                final code = codeController.text.trim();
                final title = titleController.text.trim();
                final lecturer = lecturerController.text.trim();
                final faculty = facultyController.text.trim();
                final credits = int.tryParse(creditsController.text) ?? 3;
                final capacity = int.tryParse(capacityController.text) ?? 30;
                final semester = semesterController.text.trim();

                if (code.isEmpty || title.isEmpty || lecturer.isEmpty) return;

                final courseData = {
                  'courseCode': code,
                  'courseTitle': title,
                  'lecturer': lecturer,
                  'faculty': faculty,
                  'creditHours': credits,
                  'maxCapacity': capacity,
                  'semester': semester,
                  'currentCapacity': 0,
                  'enrolledCount': 0
                };

                try {
                  final response = await ApiService.post('/courses', courseData);
                  if (response is Map && response.containsKey('error')) {
                    throw Exception(response['error']);
                  }
                  if (ctx.mounted) Navigator.of(ctx).pop();
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('New course added successfully!'), backgroundColor: Color(0xFF00BFA5)),
                    );
                    _fetchData();
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'), backgroundColor: Colors.redAccent),
                    );
                  }
                }
              },
              child: const Text('Add Course', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

  // Handle Admin Edit Course Details
  void _showEditCourseDialog(dynamic course) {
    final id = course['id'];
    final codeController = TextEditingController(text: course['courseCode']);
    final titleController = TextEditingController(text: course['courseTitle']);
    final lecturerController = TextEditingController(text: course['lecturer']);
    final facultyController = TextEditingController(text: course['faculty'] ?? '');
    final creditsController = TextEditingController(text: (course['creditHours'] ?? 3).toString());
    final capacityController = TextEditingController(text: (course['maxCapacity'] ?? 30).toString());
    final semesterController = TextEditingController(text: course['semester'] ?? '1');

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setStateDialog) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: Text('Edit Course: ${course['courseCode']}', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: codeController,
                  readOnly: true, // Course Code is immutable
                  decoration: const InputDecoration(
                    labelText: 'Course Code (Read-Only)', 
                    labelStyle: TextStyle(color: Colors.white38),
                    enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  ),
                  style: const TextStyle(color: Colors.white38),
                ),
                TextField(
                  controller: titleController,
                  decoration: const InputDecoration(labelText: 'Course Title', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: lecturerController,
                  decoration: const InputDecoration(labelText: 'Lecturer Name', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: facultyController,
                  decoration: const InputDecoration(labelText: 'Faculty (e.g. FTMK)', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: creditsController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Credit Hours', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: capacityController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Max Capacity / Seats', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
                TextField(
                  controller: semesterController,
                  decoration: const InputDecoration(labelText: 'Semester', labelStyle: TextStyle(color: Colors.white70)),
                  style: const TextStyle(color: Colors.white),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancel', style: TextStyle(color: Colors.white54)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF00BFA5)),
              onPressed: () async {
                final title = titleController.text.trim();
                final lecturer = lecturerController.text.trim();
                final faculty = facultyController.text.trim();
                final credits = int.tryParse(creditsController.text) ?? 3;
                final capacity = int.tryParse(capacityController.text) ?? 30;
                final semester = semesterController.text.trim();

                if (title.isEmpty || lecturer.isEmpty) return;

                final courseData = {
                  'courseTitle': title,
                  'lecturer': lecturer,
                  'faculty': faculty,
                  'creditHours': credits,
                  'maxCapacity': capacity,
                  'semester': semester,
                };

                try {
                  final response = await ApiService.put('/courses/$id', courseData);
                  if (response is Map && response.containsKey('error')) {
                    throw Exception(response['error']);
                  }
                  if (ctx.mounted) Navigator.of(ctx).pop();
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Course updated successfully!'), backgroundColor: Color(0xFF00BFA5)),
                    );
                    _fetchData();
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'), backgroundColor: Colors.redAccent),
                    );
                  }
                }
              },
              child: const Text('Save Changes', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

  // Handle Admin Delete Course
  Future<void> _deleteCourse(int id, String courseCode) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        title: const Text('Delete Course', style: TextStyle(color: Colors.white)),
        content: Text('Are you sure you want to permanently delete course $courseCode?', style: const TextStyle(color: Colors.white70)),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel', style: TextStyle(color: Colors.white54)),
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
      final response = await ApiService.delete('/courses/$id');
      if (response is Map && response.containsKey('error')) {
        throw Exception(response['error']);
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Course deleted successfully!'), backgroundColor: Color(0xFF00BFA5)),
      );
      _fetchData();
    } catch (e) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'), backgroundColor: Colors.redAccent),
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
      final code = (course['courseCode'] ?? '').toString().toLowerCase();
      final title = (course['courseTitle'] ?? '').toString().toLowerCase();
      final lecturer = (course['lecturer'] ?? '').toString().toLowerCase();
      final faculty = (course['faculty'] ?? '').toString().toLowerCase();

      return code.contains(query) ||
             title.contains(query) ||
             lecturer.contains(query) ||
             faculty.contains(query);
    }).toList();

    if (isStudent) {
      if (_enrolmentFilter == 'ENROLLED') {
        filteredCourses = filteredCourses.where((course) => _isEnrolled(course['courseCode'])).toList();
      } else if (_enrolmentFilter == 'NOT_ENROLLED') {
        filteredCourses = filteredCourses.where((course) => !_isEnrolled(course['courseCode'])).toList();
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
              child: const Icon(Icons.add_rounded, color: Colors.white, size: 28),
            )
          : null,
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF00BFA5)))
          : Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // ---- Header Profile (Only for Student) ----
                  if (isStudent && _studentProfile != null) ...[
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFF1E1E1E), Color(0xFF2C2C2C)],
                        ),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: Colors.white10),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                _studentProfile?['programme'] ?? 'Enrolled Student',
                                style: const TextStyle(color: Color(0xFF00BFA5), fontSize: 13, fontWeight: FontWeight.bold),
                              ),
                              Text(
                                'Sem: ${_studentProfile?['semester'] ?? '1'}',
                                style: const TextStyle(color: Colors.white70, fontSize: 12, fontWeight: FontWeight.bold),
                              ),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text(
                            auth.fullName ?? '',
                            style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 8),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                'My Active Courses: ${_myEnrolments.length}',
                                style: const TextStyle(color: Colors.white70, fontSize: 13),
                              ),
                              Text(
                                'GPA: ${_studentProfile?['gpa']?.toString() ?? 'N/A'}',
                                style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600),
                              ),
                            ],
                          ),
                        ],
                      ),
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
                        border: Border.all(color: Colors.redAccent.withOpacity(0.3)),
                      ),
                      child: Text(_error!, style: const TextStyle(color: Colors.redAccent, fontSize: 13)),
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
                      prefixIcon: const Icon(Icons.search_rounded, color: Color(0xFF00BFA5)),
                      suffixIcon: _searchQuery.isNotEmpty
                          ? IconButton(
                              icon: const Icon(Icons.clear_rounded, color: Colors.white54),
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
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
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
                        borderSide: const BorderSide(color: Color(0xFF00BFA5), width: 1.0),
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
                            onSelected: () => setState(() => _enrolmentFilter = 'ALL'),
                          ),
                          const SizedBox(width: 8),
                          _buildFilterChip(
                            label: 'Enrolled (${_myEnrolments.length})',
                            isSelected: _enrolmentFilter == 'ENROLLED',
                            onSelected: () => setState(() => _enrolmentFilter = 'ENROLLED'),
                            activeColor: const Color(0xFF00BFA5),
                          ),
                          const SizedBox(width: 8),
                          _buildFilterChip(
                            label: 'Not Enrolled (${_allCourses.length - _myEnrolments.length})',
                            isSelected: _enrolmentFilter == 'NOT_ENROLLED',
                            onSelected: () => setState(() => _enrolmentFilter = 'NOT_ENROLLED'),
                            activeColor: Colors.amberAccent,
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 16),

                  // ---- Section Title ----
                  Text(
                    isStudent ? 'Available Courses to Enrol' : 'Active Course Catalog',
                    style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
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
                              style: const TextStyle(color: Colors.white38, fontSize: 14),
                            ),
                          )
                        : ListView.builder(
                            itemCount: filteredCourses.length,
                            itemBuilder: (context, index) {
                              final course = filteredCourses[index];
                              final String code = course['courseCode'];
                              final String title = course['courseTitle'];
                              final String lecturer = course['lecturer'];
                              final String faculty = course['faculty'] ?? 'FTMK';
                              final int credits = course['creditHours'] ?? 3;
                              final int capacity = course['maxCapacity'] ?? 30;
                              final int enrolled = course['currentCapacity'] ?? course['enrolledCount'] ?? 0;
                              final int remainingSeats = capacity - enrolled;
                              final bool enrolledStatus = _isEnrolled(code);

                              return Card(
                                color: const Color(0xFF1E1E1E),
                                margin: const EdgeInsets.only(bottom: 12),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  side: BorderSide(
                                    color: enrolledStatus ? const Color(0xFF00BFA5).withOpacity(0.3) : Colors.white10,
                                    width: enrolledStatus ? 1.5 : 1.0,
                                  ),
                                ),
                                child: InkWell(
                                  borderRadius: BorderRadius.circular(12),
                                  onTap: !isStudent
                                      ? () => _showEditCourseDialog(course)
                                      : null,
                                  child: Padding(
                                    padding: const EdgeInsets.all(16.0),
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Row(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Expanded(
                                              child: Column(
                                                crossAxisAlignment: CrossAxisAlignment.start,
                                                children: [
                                                  Text(
                                                    '$code - $faculty',
                                                    style: const TextStyle(color: Color(0xFF00BFA5), fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.1),
                                                  ),
                                                  const SizedBox(height: 4),
                                                  Text(
                                                    title,
                                                    style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                                                  ),
                                                  if (isStudent) ...[
                                                    const SizedBox(height: 6),
                                                    Container(
                                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                                      decoration: BoxDecoration(
                                                        color: enrolledStatus
                                                            ? const Color(0xFF00BFA5).withOpacity(0.15)
                                                            : Colors.white.withOpacity(0.05),
                                                        borderRadius: BorderRadius.circular(6),
                                                        border: Border.all(
                                                          color: enrolledStatus
                                                              ? const Color(0xFF00BFA5).withOpacity(0.3)
                                                              : Colors.white10,
                                                          width: 0.8,
                                                        ),
                                                      ),
                                                      child: Row(
                                                        mainAxisSize: MainAxisSize.min,
                                                        children: [
                                                          Icon(
                                                            enrolledStatus ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
                                                            color: enrolledStatus ? const Color(0xFF00BFA5) : Colors.white30,
                                                            size: 13,
                                                          ),
                                                          const SizedBox(width: 6),
                                                          Text(
                                                            enrolledStatus ? 'Enrolled / Sudah Enrol' : 'Not Enrolled / Belum Enrol',
                                                            style: TextStyle(
                                                              color: enrolledStatus ? const Color(0xFF00BFA5) : Colors.white38,
                                                              fontSize: 11,
                                                              fontWeight: FontWeight.bold,
                                                            ),
                                                          ),
                                                        ],
                                                      ),
                                                    ),
                                                  ],
                                                ],
                                              ),
                                            ),
                                            // ---- Capacity Seats Badge ----
                                            Container(
                                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                              decoration: BoxDecoration(
                                                color: remainingSeats > 0 ? Colors.white.withOpacity(0.05) : Colors.redAccent.withOpacity(0.15),
                                                borderRadius: BorderRadius.circular(8),
                                              ),
                                              child: Text(
                                                remainingSeats > 0 ? '$remainingSeats / $capacity Seats Left' : 'FULL',
                                                style: TextStyle(
                                                  color: remainingSeats > 0 ? Colors.white70 : Colors.redAccent,
                                                  fontSize: 10,
                                                  fontWeight: FontWeight.bold,
                                                ),
                                              ),
                                            ),
                                          ],
                                        ),
                                        const SizedBox(height: 10),
                                        Text('Lecturer: $lecturer', style: const TextStyle(color: Colors.white70, fontSize: 13)),
                                        Text('Credits: $credits Hours', style: const TextStyle(color: Colors.white54, fontSize: 13)),
                                        const Divider(color: Colors.white10, height: 20),
                                        
                                        // ---- Enroll / Drop Actions ----
                                        Row(
                                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                          children: [
                                            Text(
                                              'Semester: ${course['semester'] ?? '1'}',
                                              style: const TextStyle(color: Colors.white30, fontSize: 12, fontFamily: 'monospace'),
                                            ),
                                            if (isStudent)
                                              enrolledStatus
                                                  ? ElevatedButton(
                                                      style: ElevatedButton.styleFrom(
                                                        backgroundColor: Colors.redAccent.withOpacity(0.15),
                                                        foregroundColor: Colors.redAccent,
                                                        side: const BorderSide(color: Colors.redAccent, width: 0.5),
                                                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                                      ),
                                                      onPressed: () => _dropCourse(code),
                                                      child: const Text('DROP COURSE', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                                                    )
                                                  : ElevatedButton(
                                                      style: ElevatedButton.styleFrom(
                                                        backgroundColor: const Color(0xFF3F51B5),
                                                        foregroundColor: Colors.white,
                                                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                                      ),
                                                      onPressed: remainingSeats > 0 ? () => _enrollCourse(code) : null,
                                                      child: const Text('ENROL', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                                                    )
                                            else
                                              Row(
                                                mainAxisSize: MainAxisSize.min,
                                                children: [
                                                  IconButton(
                                                    icon: const Icon(Icons.edit_rounded, color: Color(0xFF00BFA5)),
                                                    tooltip: 'Edit Course',
                                                    onPressed: () => _showEditCourseDialog(course),
                                                  ),
                                                  IconButton(
                                                    icon: const Icon(Icons.delete_outline_rounded, color: Colors.redAccent),
                                                    tooltip: 'Delete Course',
                                                    onPressed: () => _deleteCourse(course['id'], code),
                                                  ),
                                                ],
                                              ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
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
          color: isSelected ? activeColor.withOpacity(0.15) : const Color(0xFF1E1E1E),
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
