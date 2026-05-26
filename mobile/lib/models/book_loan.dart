class BookLoan {
  final int id;
  final String loanReference;
  final String studentId;
  final String? studentName;
  final String bookIsbn;
  final String? bookTitle;
  final String loanDate; // yyyy-MM-dd
  final String dueDate; // yyyy-MM-dd
  final String? returnDate; // yyyy-MM-dd (null if not returned)
  final String status; // 'BORROWED', 'RETURNED', 'OVERDUE', 'LOST'
  final double fineAmount;

  BookLoan({
    required this.id,
    required this.loanReference,
    required this.studentId,
    this.studentName,
    required this.bookIsbn,
    this.bookTitle,
    required this.loanDate,
    required this.dueDate,
    this.returnDate,
    required this.status,
    required this.fineAmount,
  });

  bool get isReturned => status == 'RETURNED';
  bool get isOverdue => status == 'OVERDUE';

  factory BookLoan.fromJson(Map<String, dynamic> json) {
    final retDateVal = json['returnDate'] as String?;
    final retDate =
        (retDateVal == null ||
            retDateVal.trim().isEmpty ||
            retDateVal == 'null')
        ? null
        : retDateVal;

    return BookLoan(
      id: json['id'] as int,
      loanReference: json['loanReference'] as String,
      studentId: json['studentId'] as String,
      studentName: json['studentName'] as String?,
      bookIsbn: json['bookIsbn'] as String,
      bookTitle: json['bookTitle'] as String?,
      loanDate: json['loanDate'] as String,
      dueDate: json['dueDate'] as String,
      returnDate: retDate,
      status: json['status'] as String,
      fineAmount: (json['fineAmount'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'loanReference': loanReference,
      'studentId': studentId,
      'studentName': studentName,
      'bookIsbn': bookIsbn,
      'bookTitle': bookTitle,
      'loanDate': loanDate,
      'dueDate': dueDate,
      'returnDate': returnDate,
      'status': status,
      'fineAmount': fineAmount,
    };
  }
}
