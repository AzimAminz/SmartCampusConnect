import 'package:flutter/material.dart';
import '../../../models/course.dart';

class CourseCard extends StatelessWidget {
  final Course course;
  final bool isStudent;
  final bool enrolledStatus;
  final VoidCallback? onEnroll;
  final VoidCallback? onDrop;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  const CourseCard({
    super.key,
    required this.course,
    required this.isStudent,
    required this.enrolledStatus,
    this.onEnroll,
    this.onDrop,
    this.onEdit,
    this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final remainingSeats = course.maxCapacity - course.currentCapacity;

    return Card(
      color: const Color(0xFF1E1E1E),
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: enrolledStatus
              ? const Color(0xFF00BFA5).withOpacity(0.3)
              : Colors.white10,
          width: enrolledStatus ? 1.5 : 1.0,
        ),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: !isStudent ? onEdit : null,
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
                          '${course.courseCode} - ${course.faculty ?? 'FTMK'}',
                          style: const TextStyle(
                            color: Color(0xFF00BFA5),
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1.1,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          course.courseTitle,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        if (isStudent) ...[
                          const SizedBox(height: 6),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 3,
                            ),
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
                                  enrolledStatus
                                      ? Icons.check_circle_rounded
                                      : Icons.radio_button_unchecked_rounded,
                                  color: enrolledStatus
                                      ? const Color(0xFF00BFA5)
                                      : Colors.white30,
                                  size: 13,
                                ),
                                const SizedBox(width: 6),
                                Text(
                                  enrolledStatus
                                      ? 'Enrolled / Sudah Enrol'
                                      : 'Not Enrolled / Belum Enrol',
                                  style: TextStyle(
                                    color: enrolledStatus
                                        ? const Color(0xFF00BFA5)
                                        : Colors.white38,
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
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: remainingSeats > 0
                          ? Colors.white.withOpacity(0.05)
                          : Colors.redAccent.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      remainingSeats > 0
                          ? '$remainingSeats / ${course.maxCapacity} Seats Left'
                          : 'FULL',
                      style: TextStyle(
                        color: remainingSeats > 0
                            ? Colors.white70
                            : Colors.redAccent,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                'Lecturer: ${course.lecturer}',
                style: const TextStyle(
                  color: Colors.white70,
                  fontSize: 13,
                ),
              ),
              Text(
                'Credits: ${course.creditHours} Hours',
                style: const TextStyle(
                  color: Colors.white54,
                  fontSize: 13,
                ),
              ),
              const Divider(
                color: Colors.white10,
                height: 20,
              ),

              // ---- Enroll / Drop Actions ----
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Semester: ${course.semester}',
                    style: const TextStyle(
                      color: Colors.white30,
                      fontSize: 12,
                      fontFamily: 'monospace',
                    ),
                  ),
                  if (isStudent)
                    enrolledStatus
                        ? ElevatedButton(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.redAccent.withOpacity(0.15),
                              foregroundColor: Colors.redAccent,
                              side: const BorderSide(
                                color: Colors.redAccent,
                                width: 0.5,
                              ),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(8),
                              ),
                            ),
                            onPressed: onDrop,
                            child: const Text(
                              'DROP COURSE',
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 12,
                              ),
                            ),
                          )
                        : ElevatedButton(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFF3F51B5),
                              foregroundColor: Colors.white,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(8),
                              ),
                            ),
                            onPressed: remainingSeats > 0 ? onEnroll : null,
                            child: const Text(
                              'ENROL',
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 12,
                              ),
                            ),
                          )
                  else
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          icon: const Icon(
                            Icons.edit_rounded,
                            color: Color(0xFF00BFA5),
                          ),
                          tooltip: 'Edit Course',
                          onPressed: onEdit,
                        ),
                        IconButton(
                          icon: const Icon(
                            Icons.delete_outline_rounded,
                            color: Colors.redAccent,
                          ),
                          tooltip: 'Delete Course',
                          onPressed: onDelete,
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
  }
}
