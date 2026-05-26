import 'package:flutter/material.dart';
import '../../models/book.dart';
import '../../services/soap_service.dart';
import '../../widgets/app_drawer.dart';

class SearchBooksScreen extends StatefulWidget {
  const SearchBooksScreen({super.key});

  @override
  State<SearchBooksScreen> createState() => _SearchBooksScreenState();
}

class _SearchBooksScreenState extends State<SearchBooksScreen> {
  final _searchController = TextEditingController();
  List<Book> _books = [];
  bool _isLoading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _performSearch(""); // Load all books on startup
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _performSearch(String query) async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final results = await SoapService.searchBooks(query);
      setState(() {
        _books = results;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: const Color(0xFF121212), // Matte Obsidian
      appBar: AppBar(
        title: const Text('Search Library Catalog'),
        backgroundColor: const Color(0xFF1E1E1E),
        foregroundColor: Colors.white,
      ),
      drawer: const AppDrawer(currentRoute: 'search_books'),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // ---- Search Input Area ----
            TextField(
              controller: _searchController,
              style: const TextStyle(color: Colors.white),
              onChanged: (val) => _performSearch(val),
              decoration: InputDecoration(
                hintText: 'Search by title, author, category, or ISBN...',
                hintStyle: const TextStyle(color: Colors.white38),
                prefixIcon: const Icon(Icons.search_rounded, color: Color(0xFF3F51B5)),
                suffixIcon: IconButton(
                  icon: const Icon(Icons.clear_rounded, color: Colors.white54),
                  onPressed: () {
                    _searchController.clear();
                    _performSearch("");
                  },
                ),
                filled: true,
                fillColor: const Color(0xFF1E1E1E),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
            const SizedBox(height: 16),

            // ---- Error Alert Box ----
            if (_error != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.redAccent.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: Colors.redAccent.withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.error_outline, color: Colors.redAccent, size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        _error!,
                        style: const TextStyle(color: Colors.redAccent, fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ],

            // ---- Search Results Grid/List ----
            Expanded(
              child: _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(color: Color(0xFF00BFA5)),
                    )
                  : _books.isEmpty
                      ? const Center(
                          child: Text(
                            'No books found matching your query.',
                            style: TextStyle(color: Colors.white38, fontSize: 14),
                          ),
                        )
                      : ListView.builder(
                          itemCount: _books.length,
                          itemBuilder: (context, index) {
                            final book = _books[index];
                            final isAvail = book.isAvailable;

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
                                            book.title,
                                            style: const TextStyle(
                                              color: Colors.white,
                                              fontSize: 16,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ),
                                        const SizedBox(width: 8),
                                        // ---- Availability Status Badge ----
                                        Container(
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 10,
                                            vertical: 4,
                                          ),
                                          decoration: BoxDecoration(
                                            color: isAvail
                                                ? const Color(0xFF00BFA5).withOpacity(0.15)
                                                : Colors.redAccent.withOpacity(0.15),
                                            borderRadius: BorderRadius.circular(20),
                                            border: Border.all(
                                              color: isAvail
                                                  ? const Color(0xFF00BFA5).withOpacity(0.4)
                                                  : Colors.redAccent.withOpacity(0.4),
                                            ),
                                          ),
                                          child: Text(
                                            book.status,
                                            style: TextStyle(
                                              color: isAvail
                                                  ? const Color(0xFF00BFA5)
                                                  : Colors.redAccent,
                                              fontSize: 11,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                    const SizedBox(height: 8),
                                    Text(
                                      'Author: ${book.author ?? "Unknown"}',
                                      style: const TextStyle(color: Colors.white70, fontSize: 13),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      'Category: ${book.category ?? "General"}',
                                      style: const TextStyle(color: Colors.white54, fontSize: 13),
                                    ),
                                    const Divider(color: Colors.white10, height: 20),
                                    Text(
                                      'ISBN: ${book.isbn}',
                                      style: const TextStyle(
                                        color: Colors.white38,
                                        fontSize: 11,
                                        fontFamily: 'monospace',
                                      ),
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
