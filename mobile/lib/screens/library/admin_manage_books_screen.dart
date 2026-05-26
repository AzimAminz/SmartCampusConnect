import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../models/book.dart';
import '../../models/book_loan.dart';
import '../../services/api_service.dart';
import '../../services/soap_service.dart';
import '../../services/user_service.dart';
import '../../widgets/app_drawer.dart';

class AdminManageBooksScreen extends StatefulWidget {
  const AdminManageBooksScreen({super.key});

  @override
  State<AdminManageBooksScreen> createState() => _AdminManageBooksScreenState();
}

class _AdminManageBooksScreenState extends State<AdminManageBooksScreen> {
  List<BookLoan> _activeLoans = [];
  bool _isLoading = false;
  String? _error;

  String _searchQuery = '';
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _fetchLoans();
  }

  Future<void> _fetchLoans() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final dashboardData = await UserService.getDashboard();
      final loansJson = dashboardData['allBookLoans'] as List;
      final loans = loansJson.map((e) => BookLoan.fromJson(e)).toList();

      // Filter only currently borrowed books
      setState(() {
        _activeLoans = loans
            .where((l) => l.status == 'BORROWED' || l.status == 'OVERDUE')
            .toList();
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _isLoading = false;
      });
    }
  }

  void _showAddBookDialog() {
    final formKey = GlobalKey<FormState>();
    final isbnController = TextEditingController();
    final titleController = TextEditingController();
    final authorController = TextEditingController();
    final categoryController = TextEditingController();
    bool isSaving = false;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text(
            'Add Book to Catalog',
            style: TextStyle(color: Colors.white),
          ),
          content: Form(
            key: formKey,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(
                    controller: isbnController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      labelText: 'ISBN Code',
                      labelStyle: TextStyle(color: Colors.white70),
                    ),
                    validator: (v) =>
                        v == null || v.isEmpty ? 'Required' : null,
                  ),
                  TextFormField(
                    controller: titleController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      labelText: 'Book Title',
                      labelStyle: TextStyle(color: Colors.white70),
                    ),
                    validator: (v) =>
                        v == null || v.isEmpty ? 'Required' : null,
                  ),
                  TextFormField(
                    controller: authorController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      labelText: 'Author Name',
                      labelStyle: TextStyle(color: Colors.white70),
                    ),
                    validator: (v) =>
                        v == null || v.isEmpty ? 'Required' : null,
                  ),
                  TextFormField(
                    controller: categoryController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      labelText: 'Category',
                      labelStyle: TextStyle(color: Colors.white70),
                    ),
                    validator: (v) =>
                        v == null || v.isEmpty ? 'Required' : null,
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: isSaving ? null : () => Navigator.of(ctx).pop(),
              child: const Text(
                'Cancel',
                style: TextStyle(color: Colors.white54),
              ),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF3F51B5),
              ),
              onPressed: isSaving
                  ? null
                  : () async {
                      if (!formKey.currentState!.validate()) return;
                      setDialogState(() => isSaving = true);
                      try {
                        final token = context.read<AuthProvider>().token!;
                        final success = await SoapService.addBook(
                          token: token,
                          isbn: isbnController.text.trim(),
                          title: titleController.text.trim(),
                          author: authorController.text.trim(),
                          category: categoryController.text.trim(),
                        );
                        if (success && mounted) {
                          Navigator.of(ctx).pop();
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text(
                                'Book added to library successfully!',
                              ),
                              backgroundColor: Color(0xFF00BFA5),
                            ),
                          );
                        }
                      } catch (e) {
                        setDialogState(() => isSaving = false);
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              'Error: ${e.toString().replaceAll("Exception: ", "")}',
                            ),
                            backgroundColor: Colors.redAccent,
                          ),
                        );
                      }
                    },
              child: isSaving
                  ? const SizedBox(
                      height: 16,
                      width: 16,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Add Book',
                      style: TextStyle(color: Colors.white),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  void _showIssueBookDialog() {
    final formKey = GlobalKey<FormState>();
    final studentIdController = TextEditingController();
    final studentNameController = TextEditingController();
    final searchController = TextEditingController();
    DateTime? selectedDate = DateTime.now().add(const Duration(days: 14));
    final dateController = TextEditingController(
      text:
          "${selectedDate!.year}-${selectedDate!.month.toString().padLeft(2, '0')}-${selectedDate!.day.toString().padLeft(2, '0')}",
    );

    int currentStep = 1; // 1 = Search & Select Book, 2 = Enter Borrower Details
    Book? selectedBook;
    List<Book> searchResults = [];
    bool isSearching = false;
    String? searchError;
    bool isSaving = false;
    bool initialized = false;

    // Student verification & validation states
    bool isVerified = false;
    bool isVerifying = false;
    String? verificationError;
    String? verifiedProgramme;
    String? verifiedFaculty;
    int activeLoansCount = 0;
    bool hasOverdueLoans = false;
    String? validationWarning;

    Timer? debounceTimer;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) {
          // Pre-load all available books on open
          if (!initialized) {
            initialized = true;
            Future.delayed(Duration.zero, () async {
              setDialogState(() {
                isSearching = true;
                searchError = null;
              });
              try {
                final results = await SoapService.searchBooks("");
                setDialogState(() {
                  searchResults = results;
                  isSearching = false;
                });
              } catch (e) {
                setDialogState(() {
                  searchError = e.toString().replaceAll("Exception: ", "");
                  isSearching = false;
                });
              }
            });
          }

          // Search books function
          Future<void> runSearch(String query) async {
            setDialogState(() {
              isSearching = true;
              searchError = null;
            });
            try {
              final results = await SoapService.searchBooks(query);
              setDialogState(() {
                searchResults = results;
                isSearching = false;
              });
            } catch (e) {
              setDialogState(() {
                searchError = e.toString().replaceAll("Exception: ", "");
                isSearching = false;
              });
            }
          }

          return AlertDialog(
            backgroundColor: const Color(0xFF1E1E1E),
            title: Row(
              children: [
                if (currentStep == 2)
                  IconButton(
                    icon: const Icon(
                      Icons.arrow_back_rounded,
                      color: Colors.white,
                    ),
                    onPressed: () {
                      setDialogState(() {
                        currentStep = 1;
                        selectedBook = null;
                        isVerified = false;
                        isVerifying = false;
                        verificationError = null;
                        validationWarning = null;
                        verifiedProgramme = null;
                        verifiedFaculty = null;
                        activeLoansCount = 0;
                        hasOverdueLoans = false;
                        studentIdController.clear();
                        studentNameController.clear();
                        debounceTimer?.cancel();
                      });
                    },
                  ),
                Text(
                  currentStep == 1
                      ? 'Select Book to Issue'
                      : 'Verify & Enter Details',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            content: SizedBox(
              width: double.maxFinite,
              child: currentStep == 1
                  ? Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        // ---- Search Bar ----
                        TextField(
                          controller: searchController,
                          style: const TextStyle(color: Colors.white),
                          onSubmitted: (val) => runSearch(val.trim()),
                          decoration: InputDecoration(
                            hintText: 'Search by title, author, ISBN...',
                            hintStyle: const TextStyle(color: Colors.white38),
                            prefixIcon: const Icon(
                              Icons.search_rounded,
                              color: Color(0xFF3F51B5),
                            ),
                            suffixIcon: IconButton(
                              icon: const Icon(
                                Icons.send_rounded,
                                color: Color(0xFF00BFA5),
                              ),
                              onPressed: () =>
                                  runSearch(searchController.text.trim()),
                            ),
                            filled: true,
                            fillColor: Colors.black26,
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(10),
                              borderSide: BorderSide.none,
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),

                        // ---- Error Alert Box ----
                        if (searchError != null) ...[
                          Container(
                            padding: const EdgeInsets.all(8),
                            margin: const EdgeInsets.only(bottom: 8),
                            decoration: BoxDecoration(
                              color: Colors.redAccent.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(
                                color: Colors.redAccent.withOpacity(0.3),
                              ),
                            ),
                            child: Text(
                              searchError!,
                              style: const TextStyle(
                                color: Colors.redAccent,
                                fontSize: 12,
                              ),
                            ),
                          ),
                        ],

                        // ---- Scrollable Results Grid ----
                        ConstrainedBox(
                          constraints: const BoxConstraints(maxHeight: 280),
                          child: isSearching
                              ? const Center(
                                  child: Padding(
                                    padding: EdgeInsets.all(20.0),
                                    child: CircularProgressIndicator(
                                      color: Color(0xFF00BFA5),
                                    ),
                                  ),
                                )
                              : searchResults.isEmpty
                              ? const Center(
                                  child: Padding(
                                    padding: EdgeInsets.all(20.0),
                                    child: Text(
                                      'No books found.',
                                      style: TextStyle(color: Colors.white38),
                                    ),
                                  ),
                                )
                              : ListView.builder(
                                  shrinkWrap: true,
                                  itemCount: searchResults.length,
                                  itemBuilder: (context, index) {
                                    final book = searchResults[index];
                                    final isAvail = book.isAvailable;

                                    return Card(
                                      color: Colors.white.withOpacity(0.03),
                                      margin: const EdgeInsets.only(bottom: 8),
                                      shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(10),
                                        side: BorderSide(
                                          color: isAvail
                                              ? Colors.white10
                                              : Colors.redAccent.withOpacity(
                                                  0.2,
                                                ),
                                        ),
                                      ),
                                      child: ListTile(
                                        contentPadding:
                                            const EdgeInsets.symmetric(
                                              horizontal: 12,
                                              vertical: 4,
                                            ),
                                        title: Text(
                                          book.title,
                                          style: TextStyle(
                                            color: isAvail
                                                ? Colors.white
                                                : Colors.white38,
                                            fontWeight: FontWeight.bold,
                                            fontSize: 14,
                                          ),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        subtitle: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              '✍️ ${book.author ?? "Unknown"}',
                                              style: const TextStyle(
                                                color: Colors.white38,
                                                fontSize: 11,
                                              ),
                                            ),
                                            Text(
                                              'ISBN: ${book.isbn}',
                                              style: const TextStyle(
                                                color: Colors.white24,
                                                fontSize: 10,
                                                fontFamily: 'monospace',
                                              ),
                                            ),
                                          ],
                                        ),
                                        trailing: Container(
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 8,
                                            vertical: 3,
                                          ),
                                          decoration: BoxDecoration(
                                            color: isAvail
                                                ? const Color(
                                                    0xFF00BFA5,
                                                  ).withOpacity(0.12)
                                                : Colors.redAccent.withOpacity(
                                                    0.12,
                                                  ),
                                            borderRadius: BorderRadius.circular(
                                              12,
                                            ),
                                          ),
                                          child: Text(
                                            book.status,
                                            style: TextStyle(
                                              color: isAvail
                                                  ? const Color(0xFF00BFA5)
                                                  : Colors.redAccent,
                                              fontWeight: FontWeight.bold,
                                              fontSize: 9,
                                            ),
                                          ),
                                        ),
                                        onTap: isAvail
                                            ? () {
                                                setDialogState(() {
                                                  selectedBook = book;
                                                  currentStep = 2;
                                                });
                                              }
                                            : null, // Disable if already borrowed
                                      ),
                                    );
                                  },
                                ),
                        ),
                      ],
                    )
                  : Form(
                      key: formKey,
                      child: SingleChildScrollView(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            // Selected Book Summary Card
                            if (selectedBook != null) ...[
                              Container(
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  gradient: const LinearGradient(
                                    colors: [
                                      Color(0xFF3F51B5),
                                      Color(0xFF303F9F),
                                    ],
                                  ),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Text(
                                      'SELECTED BOOK',
                                      style: TextStyle(
                                        color: Colors.white60,
                                        fontSize: 10,
                                        fontWeight: FontWeight.bold,
                                        letterSpacing: 0.8,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      selectedBook!.title,
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.bold,
                                        fontSize: 14,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      'ISBN: ${selectedBook!.isbn}',
                                      style: const TextStyle(
                                        color: Colors.white70,
                                        fontSize: 11,
                                        fontFamily: 'monospace',
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(height: 16),
                            ],

                            // Borrower matric with dynamic debounced validation
                            TextFormField(
                              controller: studentIdController,
                              style: const TextStyle(color: Colors.white),
                              decoration: InputDecoration(
                                labelText: 'Student Matric ID',
                                labelStyle: const TextStyle(
                                  color: Colors.white70,
                                ),
                                hintText: 'e.g. B032310001',
                                hintStyle: const TextStyle(
                                  color: Colors.white24,
                                ),
                                suffixIcon: isVerifying
                                    ? const Padding(
                                        padding: EdgeInsets.all(12.0),
                                        child: SizedBox(
                                          width: 16,
                                          height: 16,
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2,
                                            color: Color(0xFF00BFA5),
                                          ),
                                        ),
                                      )
                                    : isVerified
                                    ? const Icon(
                                        Icons.check_circle_rounded,
                                        color: Color(0xFF00BFA5),
                                      )
                                    : null,
                              ),
                              onChanged: (val) {
                                // If they change the matric ID, reset verification state
                                setDialogState(() {
                                  isVerified = false;
                                  studentNameController.clear();
                                  verificationError = null;
                                  validationWarning = null;
                                  verifiedProgramme = null;
                                  verifiedFaculty = null;
                                  activeLoansCount = 0;
                                  hasOverdueLoans = false;
                                });

                                if (debounceTimer?.isActive ?? false)
                                  debounceTimer!.cancel();

                                final matric = val.trim();
                                if (matric.isEmpty) return;

                                debounceTimer = Timer(
                                  const Duration(milliseconds: 600),
                                  () async {
                                    if (!context.mounted) return;
                                    setDialogState(() {
                                      isVerifying = true;
                                    });

                                    try {
                                      // 1. Verify student existence and retrieve profile details from REST
                                      final response = await ApiService.get(
                                        '/reporting/student/$matric',
                                      );
                                      if (response != null &&
                                          response.containsKey('student')) {
                                        final studentData = response['student'];
                                        final name =
                                            studentData['name'] ?? 'Unknown';
                                        final prog =
                                            studentData['programme'] ?? 'N/A';
                                        final fac =
                                            studentData['faculty'] ?? 'N/A';

                                        // 2. Fetch loan history to run validation on current loan counts from SOAP
                                        final token = context
                                            .read<AuthProvider>()
                                            .token!;
                                        final loans =
                                            await SoapService.getStudentLoanHistory(
                                              token: token,
                                              studentId: matric,
                                            );

                                        // Count active loans (status is BORROWED or OVERDUE)
                                        final activeLoans = loans
                                            .where(
                                              (l) =>
                                                  l.status == 'BORROWED' ||
                                                  l.status == 'OVERDUE',
                                            )
                                            .toList();
                                        final count = activeLoans.length;
                                        final hasOverdue = activeLoans.any(
                                          (l) => l.status == 'OVERDUE',
                                        );

                                        String? warning;
                                        if (count >= 3) {
                                          warning =
                                              'Had Pinjam Buku Terlebih! Pelajar telah meminjam $count buku (Had maksima: 3).';
                                        } else if (hasOverdue) {
                                          warning =
                                              'Pelajar mempunyai buku lewat yang belum dipulangkan!';
                                        }

                                        if (!context.mounted) return;
                                        setDialogState(() {
                                          studentNameController.text = name;
                                          verifiedProgramme = prog;
                                          verifiedFaculty = fac;
                                          activeLoansCount = count;
                                          hasOverdueLoans = hasOverdue;
                                          validationWarning = warning;
                                          isVerified = true;
                                          isVerifying = false;
                                        });
                                      } else {
                                        throw Exception(
                                          'Student record not found',
                                        );
                                      }
                                    } catch (e) {
                                      if (!context.mounted) return;
                                      setDialogState(() {
                                        studentNameController.clear();
                                        isVerified = false;
                                        verifiedProgramme = null;
                                        verifiedFaculty = null;
                                        activeLoansCount = 0;
                                        hasOverdueLoans = false;
                                        verificationError =
                                            'No Matrik tiada dalam pangkalan data!';
                                        isVerifying = false;
                                      });
                                    }
                                  },
                                );
                              },
                              validator: (v) => v == null || v.trim().isEmpty
                                  ? 'Matric ID is required'
                                  : null,
                            ),
                            const SizedBox(height: 12),

                            // Verification Error Alert
                            if (verificationError != null) ...[
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 8,
                                ),
                                decoration: BoxDecoration(
                                  color: Colors.redAccent.withOpacity(0.1),
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(
                                    color: Colors.redAccent.withOpacity(0.3),
                                  ),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(
                                      Icons.error_outline_rounded,
                                      color: Colors.redAccent,
                                      size: 16,
                                    ),
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: Text(
                                        verificationError!,
                                        style: const TextStyle(
                                          color: Colors.redAccent,
                                          fontSize: 12,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(height: 12),
                            ],

                            // Verified Student Status Card (Glassmorphic & detailed)
                            if (isVerified) ...[
                              Container(
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: Colors.white.withOpacity(0.02),
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(
                                    color: validationWarning != null
                                        ? Colors.amber.withOpacity(0.3)
                                        : const Color(
                                            0xFF00BFA5,
                                          ).withOpacity(0.2),
                                  ),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      children: [
                                        Icon(
                                          validationWarning != null
                                              ? Icons.warning_amber_rounded
                                              : Icons
                                                    .check_circle_outline_rounded,
                                          color: validationWarning != null
                                              ? Colors.amber
                                              : const Color(0xFF00BFA5),
                                          size: 18,
                                        ),
                                        const SizedBox(width: 6),
                                        Text(
                                          validationWarning != null
                                              ? 'VALIDATION WARNING'
                                              : 'STUDENT VERIFIED',
                                          style: TextStyle(
                                            color: validationWarning != null
                                                ? Colors.amber
                                                : const Color(0xFF00BFA5),
                                            fontSize: 10,
                                            fontWeight: FontWeight.bold,
                                            letterSpacing: 1.0,
                                          ),
                                        ),
                                      ],
                                    ),
                                    const SizedBox(height: 8),
                                    Text(
                                      studentNameController.text,
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.bold,
                                        fontSize: 14,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      '${verifiedProgramme ?? "N/A"} (${verifiedFaculty ?? "N/A"})',
                                      style: const TextStyle(
                                        color: Colors.white54,
                                        fontSize: 11,
                                      ),
                                    ),
                                    const SizedBox(height: 8),
                                    Divider(
                                      color: Colors.white.withOpacity(0.06),
                                      height: 1,
                                    ),
                                    const SizedBox(height: 8),
                                    Row(
                                      mainAxisAlignment:
                                          MainAxisAlignment.spaceBetween,
                                      children: [
                                        const Text(
                                          'Active Library Loans:',
                                          style: TextStyle(
                                            color: Colors.white38,
                                            fontSize: 11,
                                          ),
                                        ),
                                        Text(
                                          '$activeLoansCount / 3 books',
                                          style: TextStyle(
                                            color: activeLoansCount >= 3
                                                ? Colors.redAccent
                                                : Colors.white70,
                                            fontWeight: FontWeight.bold,
                                            fontSize: 11,
                                          ),
                                        ),
                                      ],
                                    ),
                                    if (hasOverdueLoans) ...[
                                      const SizedBox(height: 4),
                                      Row(
                                        mainAxisAlignment:
                                            MainAxisAlignment.spaceBetween,
                                        children: const [
                                          Text(
                                            'Overdue Books Alert:',
                                            style: TextStyle(
                                              color: Colors.redAccent,
                                              fontSize: 11,
                                            ),
                                          ),
                                          Text(
                                            'OVERDUE DETECTED',
                                            style: TextStyle(
                                              color: Colors.redAccent,
                                              fontWeight: FontWeight.bold,
                                              fontSize: 11,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ],
                                    if (validationWarning != null) ...[
                                      const SizedBox(height: 8),
                                      Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 8,
                                          vertical: 6,
                                        ),
                                        decoration: BoxDecoration(
                                          color: Colors.redAccent.withOpacity(
                                            0.1,
                                          ),
                                          borderRadius: BorderRadius.circular(
                                            6,
                                          ),
                                        ),
                                        child: Row(
                                          children: [
                                            const Icon(
                                              Icons.error_outline_rounded,
                                              color: Colors.redAccent,
                                              size: 14,
                                            ),
                                            const SizedBox(width: 6),
                                            Expanded(
                                              child: Text(
                                                validationWarning!,
                                                style: const TextStyle(
                                                  color: Colors.redAccent,
                                                  fontSize: 10,
                                                  fontWeight: FontWeight.w500,
                                                ),
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                    ],
                                  ],
                                ),
                              ),
                              const SizedBox(height: 12),
                            ],

                            // Return Date Calendar Selector
                            TextFormField(
                              controller: dateController,
                              readOnly: true,
                              style: const TextStyle(color: Colors.white),
                              decoration: const InputDecoration(
                                labelText: 'Return Due Date',
                                labelStyle: TextStyle(color: Colors.white70),
                                suffixIcon: Icon(
                                  Icons.calendar_today_rounded,
                                  color: Color(0xFF00BFA5),
                                  size: 20,
                                ),
                              ),
                              validator: (v) =>
                                  v == null || v.isEmpty ? 'Required' : null,
                              onTap: () async {
                                DateTime? picked = await showDatePicker(
                                  context: context,
                                  initialDate:
                                      selectedDate ??
                                      DateTime.now().add(
                                        const Duration(days: 14),
                                      ),
                                  firstDate: DateTime.now(),
                                  lastDate: DateTime.now().add(
                                    const Duration(days: 90),
                                  ),
                                );
                                if (picked != null) {
                                  selectedDate = picked;
                                  setDialogState(() {
                                    dateController.text =
                                        "${picked.year}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}";
                                  });
                                }
                              },
                            ),
                          ],
                        ),
                      ),
                    ),
            ),
            actions: [
              TextButton(
                onPressed: isSaving
                    ? null
                    : () {
                        debounceTimer?.cancel();
                        Navigator.of(ctx).pop();
                      },
                child: const Text(
                  'Cancel',
                  style: TextStyle(color: Colors.white54),
                ),
              ),
              if (currentStep == 2)
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF3F51B5),
                  ),
                  onPressed:
                      (isSaving || !isVerified || validationWarning != null)
                      ? null
                      : () async {
                          if (!formKey.currentState!.validate()) return;
                          setDialogState(() => isSaving = true);
                          try {
                            final token = context.read<AuthProvider>().token!;
                            await SoapService.borrowBook(
                              token: token,
                              studentId: studentIdController.text.trim(),
                              studentName: studentNameController.text.trim(),
                              isbn: selectedBook!.isbn,
                              dueDate: dateController.text,
                            );
                            if (mounted) {
                              debounceTimer?.cancel();
                              Navigator.of(ctx).pop();
                              _fetchLoans(); // Refresh loan log list
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text('Book issued successfully!'),
                                  backgroundColor: Color(0xFF00BFA5),
                                ),
                              );
                            }
                          } catch (e) {
                            setDialogState(() => isSaving = false);
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: Text(
                                  'Error: ${e.toString().replaceAll("Exception: ", "")}',
                                ),
                                backgroundColor: Colors.redAccent,
                              ),
                            );
                          }
                        },
                  child: isSaving
                      ? const SizedBox(
                          height: 16,
                          width: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Text(
                          'Issue Loan',
                          style: TextStyle(color: Colors.white),
                        ),
                ),
            ],
          );
        },
      ),
    );
  }

  void _confirmReturn(BookLoan loan) {
    bool isSaving = false;
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text(
            'Confirm Return',
            style: TextStyle(color: Colors.white),
          ),
          content: Text(
            'Confirm return of "${loan.bookTitle}" (ISBN: ${loan.bookIsbn}) from ${loan.studentName}?',
            style: const TextStyle(color: Colors.white70),
          ),
          actions: [
            TextButton(
              onPressed: isSaving ? null : () => Navigator.of(ctx).pop(),
              child: const Text(
                'Cancel',
                style: TextStyle(color: Colors.white54),
              ),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF00BFA5),
              ),
              onPressed: isSaving
                  ? null
                  : () async {
                      setDialogState(() => isSaving = true);
                      try {
                        final token = context.read<AuthProvider>().token!;
                        final success = await SoapService.returnBook(
                          token: token,
                          loanRef: loan.loanReference,
                        );
                        if (success && mounted) {
                          Navigator.of(ctx).pop();
                          _fetchLoans(); // Refresh
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Book returned successfully!'),
                              backgroundColor: Color(0xFF00BFA5),
                            ),
                          );
                        }
                      } catch (e) {
                        setDialogState(() => isSaving = false);
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              'Error: ${e.toString().replaceAll("Exception: ", "")}',
                            ),
                            backgroundColor: Colors.redAccent,
                          ),
                        );
                      }
                    },
              child: isSaving
                  ? const SizedBox(
                      height: 16,
                      width: 16,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Confirm',
                      style: TextStyle(color: Colors.white),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final filteredLoans = _activeLoans.where((loan) {
      final query = _searchQuery.toLowerCase();
      if (query.isEmpty) return true;
      final title = (loan.bookTitle ?? '').toLowerCase();
      final isbn = loan.bookIsbn.toLowerCase();
      final matric = loan.studentId.toLowerCase();
      final name = (loan.studentName ?? '').toLowerCase();
      final ref = loan.loanReference.toLowerCase();

      return title.contains(query) ||
          isbn.contains(query) ||
          matric.contains(query) ||
          name.contains(query) ||
          ref.contains(query);
    }).toList();

    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        title: const Text('Library Manager Console'),
        backgroundColor: const Color(0xFF1E1E1E),
        foregroundColor: Colors.white,
      ),
      drawer: const AppDrawer(currentRoute: 'library_admin'),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // ---- Quick Action Buttons Row ----
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF1E1E1E),
                      foregroundColor: Colors.white,
                    ),
                    onPressed: _showAddBookDialog,
                    icon: const Icon(
                      Icons.library_add_rounded,
                      color: Color(0xFF00BFA5),
                    ),
                    label: const Text('Add Book'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF1E1E1E),
                      foregroundColor: Colors.white,
                    ),
                    onPressed: _showIssueBookDialog,
                    icon: const Icon(
                      Icons.bookmark_add_rounded,
                      color: Color(0xFF3F51B5),
                    ),
                    label: const Text('Issue Book'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),

            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Active Book Loans Log',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                IconButton(
                  icon: const Icon(
                    Icons.refresh_rounded,
                    color: Colors.white54,
                  ),
                  onPressed: _fetchLoans,
                ),
              ],
            ),
            const SizedBox(height: 12),

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
                hintText: 'Search by student name, matric, book title, ref...',
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
            const SizedBox(height: 12),

            // ---- Error Alert Box ----
            if (_error != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.redAccent.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: Colors.redAccent.withOpacity(0.3)),
                ),
                child: Text(
                  _error!,
                  style: const TextStyle(color: Colors.redAccent, fontSize: 13),
                ),
              ),
              const SizedBox(height: 12),
            ],

            // ---- Loan History List ----
            Expanded(
              child: _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: Color(0xFF00BFA5),
                      ),
                    )
                  : filteredLoans.isEmpty
                  ? Center(
                      child: Text(
                        _searchQuery.isNotEmpty
                            ? 'No active book loans matching query.'
                            : 'No active borrowed books at the moment.',
                        style: const TextStyle(
                          color: Colors.white38,
                          fontSize: 14,
                        ),
                      ),
                    )
                  : ListView.builder(
                      itemCount: filteredLoans.length,
                      itemBuilder: (context, index) {
                        final loan = filteredLoans[index];

                        return Card(
                          color: const Color(0xFF1E1E1E),
                          margin: const EdgeInsets.only(bottom: 12),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                            side: const BorderSide(color: Colors.white10),
                          ),
                          child: ListTile(
                            contentPadding: const EdgeInsets.all(16),
                            title: Text(
                              loan.bookTitle ?? 'Unknown Book',
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 15,
                              ),
                            ),
                            subtitle: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const SizedBox(height: 6),
                                Text(
                                  'Matric: ${loan.studentId} (${loan.studentName ?? "N/A"})',
                                  style: const TextStyle(
                                    color: Colors.white70,
                                    fontSize: 13,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  'ISBN: ${loan.bookIsbn}',
                                  style: const TextStyle(
                                    color: Colors.white54,
                                    fontSize: 13,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  'Borrowed On: ${loan.loanDate}',
                                  style: const TextStyle(
                                    color: Colors.white70,
                                    fontSize: 13,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  'Return Due: ${loan.dueDate}',
                                  style: TextStyle(
                                    color: loan.status == 'OVERDUE'
                                        ? Colors.redAccent
                                        : Colors.white54,
                                    fontSize: 13,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  'Loan Ref: ${loan.loanReference}',
                                  style: const TextStyle(
                                    color: Colors.white24,
                                    fontSize: 11,
                                    fontFamily: 'monospace',
                                  ),
                                ),
                              ],
                            ),
                            trailing: ElevatedButton(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: const Color(
                                  0xFF00BFA5,
                                ).withOpacity(0.15),
                                foregroundColor: const Color(0xFF00BFA5),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(8),
                                  side: const BorderSide(
                                    color: Color(0xFF00BFA5),
                                    width: 0.8,
                                  ),
                                ),
                              ),
                              onPressed: () => _confirmReturn(loan),
                              child: const Text('Return'),
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
}
