import 'package:flutter/material.dart';
import '../../../models/student.dart';

class StudentCard extends StatelessWidget {
  final Student student;
  final int index;
  final VoidCallback onTap;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  const StudentCard({
    super.key,
    required this.student,
    required this.index,
    required this.onTap,
    required this.onEdit,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    // Avatar color based on index
    final colors = [
      const Color(0xFF3F51B5),
      const Color(0xFF00BFA5),
      Colors.purpleAccent,
      Colors.amberAccent,
      Colors.cyanAccent,
    ];
    final avatarColor = colors[index % colors.length];

    return Card(
      color: const Color(0xFF1E1E1E),
      margin: const EdgeInsets.only(bottom: 10),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: Colors.white10),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14.0),
          child: Row(
            children: [
              // Avatar
              CircleAvatar(
                radius: 24,
                backgroundColor: avatarColor.withOpacity(0.15),
                child: Text(
                  student.name.isNotEmpty ? student.name[0].toUpperCase() : 'S',
                  style: TextStyle(
                    color: avatarColor,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(width: 14),
              // Student Info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      student.name,
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 15,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      student.studentId,
                      style: const TextStyle(
                        color: Color(0xFF00BFA5),
                        fontSize: 12,
                        fontFamily: 'monospace',
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      student.email,
                      style: const TextStyle(
                        color: Colors.white54,
                        fontSize: 11,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        _buildMiniChip(
                          student.faculty ?? 'N/A',
                          Colors.white24,
                          Colors.white70,
                        ),
                        const SizedBox(width: 6),
                        _buildMiniChip(
                          'Sem ${student.semester ?? '1'}',
                          const Color(0xFF3F51B5).withOpacity(0.3),
                          const Color(0xFF3F51B5),
                        ),
                        const SizedBox(width: 6),
                        _buildMiniChip(
                          'GPA: ${student.gpa.toStringAsFixed(2)}',
                          const Color(0xFF00BFA5).withOpacity(0.2),
                          const Color(0xFF00BFA5),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              // Action buttons
              Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: const Icon(
                      Icons.edit_rounded,
                      size: 18,
                      color: Color(0xFF3F51B5),
                    ),
                    tooltip: 'Edit',
                    onPressed: onEdit,
                    padding: const EdgeInsets.all(4),
                    constraints: const BoxConstraints(),
                  ),
                  const SizedBox(height: 6),
                  IconButton(
                    icon: const Icon(
                      Icons.delete_outline_rounded,
                      size: 18,
                      color: Colors.redAccent,
                    ),
                    tooltip: 'Delete',
                    onPressed: onDelete,
                    padding: const EdgeInsets.all(4),
                    constraints: const BoxConstraints(),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMiniChip(String label, Color bgColor, Color textColor) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(5),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: textColor,
          fontSize: 10,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
