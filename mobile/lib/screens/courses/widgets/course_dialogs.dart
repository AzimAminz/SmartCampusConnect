import 'package:flutter/material.dart';
import '../../../models/course.dart';

class AddCourseDialog extends StatefulWidget {
  final Future<void> Function(Map<String, dynamic> courseData) onAdd;

  const AddCourseDialog({super.key, required this.onAdd});

  @override
  State<AddCourseDialog> createState() => _AddCourseDialogState();
}

class _AddCourseDialogState extends State<AddCourseDialog> {
  final _formKey = GlobalKey<FormState>();
  final _codeController = TextEditingController();
  final _titleController = TextEditingController();
  final _lecturerController = TextEditingController();
  final _facultyController = TextEditingController();
  final _creditsController = TextEditingController(text: "3");
  final _capacityController = TextEditingController(text: "30");
  final _semesterController = TextEditingController(text: "1");
  bool _isSubmitting = false;

  @override
  void dispose() {
    _codeController.dispose();
    _titleController.dispose();
    _lecturerController.dispose();
    _facultyController.dispose();
    _creditsController.dispose();
    _capacityController.dispose();
    _semesterController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: const Color(0xFF1E1E1E),
      title: const Text(
        'Register New Course',
        style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
      ),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _codeController,
                decoration: const InputDecoration(
                  labelText: 'Course Code (e.g. BITP3123)',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) => v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              TextFormField(
                controller: _titleController,
                decoration: const InputDecoration(
                  labelText: 'Course Title',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) => v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              TextFormField(
                controller: _lecturerController,
                decoration: const InputDecoration(
                  labelText: 'Lecturer Name',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) => v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              TextFormField(
                controller: _facultyController,
                decoration: const InputDecoration(
                  labelText: 'Faculty (e.g. FTMK)',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
              ),
              TextFormField(
                controller: _creditsController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Credit Hours',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) =>
                    v == null || int.tryParse(v) == null ? 'Must be integer' : null,
              ),
              TextFormField(
                controller: _capacityController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Max Capacity / Seats',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) =>
                    v == null || int.tryParse(v) == null ? 'Must be integer' : null,
              ),
              TextFormField(
                controller: _semesterController,
                decoration: const InputDecoration(
                  labelText: 'Semester',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
          child: const Text(
            'Cancel',
            style: TextStyle(color: Colors.white54),
          ),
        ),
        ElevatedButton(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF3F51B5),
          ),
          onPressed: _isSubmitting
              ? null
              : () async {
                  if (!_formKey.currentState!.validate()) return;
                  setState(() => _isSubmitting = true);
                  final courseData = {
                    'courseCode': _codeController.text.trim(),
                    'courseTitle': _titleController.text.trim(),
                    'lecturer': _lecturerController.text.trim(),
                    'faculty': _facultyController.text.trim(),
                    'creditHours': int.parse(_creditsController.text),
                    'maxCapacity': int.parse(_capacityController.text),
                    'semester': _semesterController.text.trim(),
                    'currentCapacity': 0,
                    'enrolledCount': 0,
                  };
                  try {
                    await widget.onAdd(courseData);
                    if (mounted) Navigator.of(context).pop();
                  } catch (e) {
                    setState(() => _isSubmitting = false);
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(e.toString().replaceAll("Exception: ", "")),
                          backgroundColor: Colors.redAccent,
                        ),
                      );
                    }
                  }
                },
          child: _isSubmitting
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                )
              : const Text(
                  'Add Course',
                  style: TextStyle(color: Colors.white),
                ),
        ),
      ],
    );
  }
}

class EditCourseDialog extends StatefulWidget {
  final Course course;
  final Future<void> Function(Map<String, dynamic> courseData) onSave;

  const EditCourseDialog({super.key, required this.course, required this.onSave});

  @override
  State<EditCourseDialog> createState() => _EditCourseDialogState();
}

class _EditCourseDialogState extends State<EditCourseDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _codeController;
  late final TextEditingController _titleController;
  late final TextEditingController _lecturerController;
  late final TextEditingController _facultyController;
  late final TextEditingController _creditsController;
  late final TextEditingController _capacityController;
  late final TextEditingController _semesterController;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _codeController = TextEditingController(text: widget.course.courseCode);
    _titleController = TextEditingController(text: widget.course.courseTitle);
    _lecturerController = TextEditingController(text: widget.course.lecturer);
    _facultyController = TextEditingController(text: widget.course.faculty ?? '');
    _creditsController = TextEditingController(text: widget.course.creditHours.toString());
    _capacityController = TextEditingController(text: widget.course.maxCapacity.toString());
    _semesterController = TextEditingController(text: widget.course.semester);
  }

  @override
  void dispose() {
    _codeController.dispose();
    _titleController.dispose();
    _lecturerController.dispose();
    _facultyController.dispose();
    _creditsController.dispose();
    _capacityController.dispose();
    _semesterController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: const Color(0xFF1E1E1E),
      title: Text(
        'Edit Course: ${widget.course.courseCode}',
        style: const TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.bold,
        ),
      ),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _codeController,
                readOnly: true,
                decoration: const InputDecoration(
                  labelText: 'Course Code (Read-Only)',
                  labelStyle: TextStyle(color: Colors.white38),
                  enabledBorder: UnderlineInputBorder(
                    borderSide: BorderSide(color: Colors.white10),
                  ),
                ),
                style: const TextStyle(color: Colors.white38),
              ),
              TextFormField(
                controller: _titleController,
                decoration: const InputDecoration(
                  labelText: 'Course Title',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) => v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              TextFormField(
                controller: _lecturerController,
                decoration: const InputDecoration(
                  labelText: 'Lecturer Name',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) => v == null || v.trim().isEmpty ? 'Required' : null,
              ),
              TextFormField(
                controller: _facultyController,
                decoration: const InputDecoration(
                  labelText: 'Faculty (e.g. FTMK)',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
              ),
              TextFormField(
                controller: _creditsController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Credit Hours',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) =>
                    v == null || int.tryParse(v) == null ? 'Must be integer' : null,
              ),
              TextFormField(
                controller: _capacityController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Max Capacity / Seats',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
                validator: (v) =>
                    v == null || int.tryParse(v) == null ? 'Must be integer' : null,
              ),
              TextFormField(
                controller: _semesterController,
                decoration: const InputDecoration(
                  labelText: 'Semester',
                  labelStyle: TextStyle(color: Colors.white70),
                ),
                style: const TextStyle(color: Colors.white),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
          child: const Text(
            'Cancel',
            style: TextStyle(color: Colors.white54),
          ),
        ),
        ElevatedButton(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF00BFA5),
          ),
          onPressed: _isSubmitting
              ? null
              : () async {
                  if (!_formKey.currentState!.validate()) return;
                  setState(() => _isSubmitting = true);
                  final courseData = {
                    'courseTitle': _titleController.text.trim(),
                    'lecturer': _lecturerController.text.trim(),
                    'faculty': _facultyController.text.trim(),
                    'creditHours': int.parse(_creditsController.text),
                    'maxCapacity': int.parse(_capacityController.text),
                    'semester': _semesterController.text.trim(),
                  };
                  try {
                    await widget.onSave(courseData);
                    if (mounted) Navigator.of(context).pop();
                  } catch (e) {
                    setState(() => _isSubmitting = false);
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(e.toString().replaceAll("Exception: ", "")),
                          backgroundColor: Colors.redAccent,
                        ),
                      );
                    }
                  }
                },
          child: _isSubmitting
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                )
              : const Text(
                  'Save Changes',
                  style: TextStyle(color: Colors.white),
                ),
        ),
      ],
    );
  }
}
