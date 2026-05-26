import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../models/book_loan.dart';
import '../../services/soap_service.dart';
import '../../services/user_service.dart';
import '../../widgets/app_drawer.dart';

class LoanHistoryScreen extends StatefulWidget {
  const LoanHistoryScreen({super.key});

  @override
  State<LoanHistoryScreen> createState() => _LoanHistoryScreenState();
}

class _LoanHistoryScreenState extends State<LoanHistoryScreen> {
  final _searchController = TextEditingController();
  List<BookLoan> _loans = [];
  bool _isLoading = false;
  String? _error;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    final auth = context.read<AuthProvider>();
    if (auth.role == 'STUDENT') {
      _fetchHistory(auth.userId!);
    } else {
      _fetchGlobalHistory();
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _fetchHistory(String studentId) async {
    if (studentId.trim().isEmpty) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final token = context.read<AuthProvider>().token!;
      final results = await SoapService.getStudentLoanHistory(token: token, studentId: studentId.trim());
      setState(() {
        _loans = results;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _loans = [];
        _isLoading = false;
      });
    }
  }

  Future<void> _fetchGlobalHistory() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final dashboardData = await UserService.getDashboard();
      final loansJson = dashboardData['allBookLoans'] as List? ?? [];
      final results = loansJson.map((e) => BookLoan.fromJson(e)).toList();
      setState(() {
        _loans = results;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _loans = [];
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final isStudent = auth.role == 'STUDENT';

    final filteredLoans = _loans.where((loan) {
      final query = _searchQuery.toLowerCase();
      if (query.isEmpty) return true;
      final title = (loan.bookTitle ?? '').toLowerCase();
      final isbn = loan.bookIsbn.toLowerCase();
      final matric = loan.studentId.toLowerCase();
      final name = (loan.studentName ?? '').toLowerCase();
      final ref = loan.loanReference.toLowerCase();
      final status = loan.status.toLowerCase();

      return title.contains(query) ||
             isbn.contains(query) ||
             matric.contains(query) ||
             name.contains(query) ||
             ref.contains(query) ||
             status.contains(query);
    }).toList();

    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        title: const Text('Book Borrowing History'),
        backgroundColor: const Color(0xFF1E1E1E),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: isStudent
                ? () => _fetchHistory(auth.userId!)
                : _fetchGlobalHistory,
          ),
        ],
      ),
      drawer: const AppDrawer(currentRoute: 'loan_history'),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
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
                hintText: isStudent
                    ? 'Search my loans by title, ISBN, status...'
                    : 'Search all loans by name, matric, title, ISBN...',
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
                fillColor: const Color(0xFF1E1E1E),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
            const SizedBox(height: 16),

            // ---- Student Info Label ----
            if (isStudent) ...[
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFF1E1E1E),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.white10),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.info_outline, color: Color(0xFF00BFA5)),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'Showing loan logs for personal Matric: ${auth.userId}',
                        style: const TextStyle(color: Colors.white70, fontSize: 14, fontWeight: FontWeight.w600),
                      ),
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
                  color: Colors.redAccent.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: Colors.redAccent.withValues(alpha: 0.3)),
                ),
                child: Text(_error!, style: const TextStyle(color: Colors.redAccent, fontSize: 13)),
              ),
              const SizedBox(height: 12),
            ],

            // ---- Loans Timeline List ----
            Expanded(
              child: _isLoading
                  ? const Center(child: CircularProgressIndicator(color: Color(0xFF00BFA5)))
                  : filteredLoans.isEmpty
                      ? Center(
                          child: Text(
                            isStudent
                                ? (_searchQuery.isNotEmpty
                                    ? 'No loans match your search.'
                                    : 'You have not borrowed any books yet.')
                                : (_searchQuery.isNotEmpty
                                    ? 'No loans match your search.'
                                    : 'No borrowing history found in the campus.'),
                            style: const TextStyle(color: Colors.white38, fontSize: 14),
                            textAlign: TextAlign.center,
                          ),
                        )
                      : ListView.builder(
                          itemCount: filteredLoans.length,
                          itemBuilder: (context, index) {
                            final loan = filteredLoans[index];
                            final returned = loan.isReturned;

                            return Card(
                              color: const Color(0xFF1E1E1E),
                              margin: const EdgeInsets.only(bottom: 12),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                                side: const BorderSide(color: Colors.white10),
                              ),
                              child: Padding(
                                padding: const EdgeInsets.all(16.0),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Expanded(
                                          child: Text(
                                            loan.bookTitle ?? 'Unknown Book',
                                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                                          ),
                                        ),
                                        const SizedBox(width: 8),
                                        // ---- Return Status Badge ----
                                        Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                          decoration: BoxDecoration(
                                            color: returned
                                                ? const Color(0xFF00BFA5).withValues(alpha: 0.15)
                                                : Colors.redAccent.withValues(alpha: 0.15),
                                            borderRadius: BorderRadius.circular(20),
                                            border: Border.all(
                                              color: returned
                                                  ? const Color(0xFF00BFA5).withValues(alpha: 0.4)
                                                  : Colors.redAccent.withValues(alpha: 0.4),
                                            ),
                                          ),
                                          child: Text(
                                            loan.status,
                                            style: TextStyle(
                                              color: returned ? const Color(0xFF00BFA5) : Colors.redAccent,
                                              fontSize: 10,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                    const SizedBox(height: 8),
                                    if (!isStudent && loan.studentName != null) ...[
                                      Text('Borrower: ${loan.studentName} (${loan.studentId})', style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.bold)),
                                      const SizedBox(height: 4),
                                    ],
                                    Text('ISBN: ${loan.bookIsbn}', style: const TextStyle(color: Colors.white54, fontSize: 13)),
                                    const SizedBox(height: 6),
                                    Text('Borrowed On: ${loan.loanDate}', style: const TextStyle(color: Colors.white70, fontSize: 13)),
                                    Text('Return Due: ${loan.dueDate}', style: const TextStyle(color: Colors.white70, fontSize: 13)),
                                    if (loan.returnDate != null && loan.returnDate != 'null' && loan.returnDate!.trim().isNotEmpty)
                                      Text('Returned On: ${loan.returnDate}', style: const TextStyle(color: Color(0xFF00BFA5), fontSize: 13, fontWeight: FontWeight.w600)),
                                    if (loan.fineAmount > 0) ...[
                                      const SizedBox(height: 6),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                        decoration: BoxDecoration(
                                          color: Colors.redAccent.withValues(alpha: 0.1),
                                          borderRadius: BorderRadius.circular(6),
                                        ),
                                        child: Text(
                                          'Late Fine: RM${loan.fineAmount.toStringAsFixed(2)}',
                                          style: const TextStyle(color: Colors.redAccent, fontSize: 13, fontWeight: FontWeight.bold),
                                        ),
                                      ),
                                    ],

                                    const Divider(color: Colors.white10, height: 24),
                                    Text(
                                      'Loan Ref: ${loan.loanReference}',
                                      style: const TextStyle(color: Colors.white24, fontSize: 11, fontFamily: 'monospace'),
                                    ),
                                  ],
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
