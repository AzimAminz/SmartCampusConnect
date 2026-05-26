class Book {
  final int id;
  final String isbn;
  final String title;
  final String? author;
  final String? category;
  final String status; // 'AVAILABLE' or 'BORROWED'

  Book({
    required this.id,
    required this.isbn,
    required this.title,
    this.author,
    this.category,
    required this.status,
  });

  bool get isAvailable => status == 'AVAILABLE';

  factory Book.fromJson(Map<String, dynamic> json) {
    return Book(
      id: json['id'] as int,
      isbn: json['isbn'] as String,
      title: json['title'] as String,
      author: json['author'] as String?,
      category: json['category'] as String?,
      status: json['status'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'isbn': isbn,
      'title': title,
      'author': author,
      'category': category,
      'status': status,
    };
  }
}
