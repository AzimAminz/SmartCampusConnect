# 📱 Developer Guide: Flutter Mobile Client (REST + SOAP)

Welcome to the **SmartCampusConnect Mobile Client** developer guide! This document provides a complete checklist, architectural instructions, and coding templates for the Flutter mobile application to integrate the new Library Master Book Catalog and Secure Transaction workflows.

The mobile app communicates with:
- **REST API** (Port `8080`) for user authentication, student dashboard, and course enrolments.
- **SOAP API** (Port `8085` at `http://localhost:8085/ws/booking`) for room bookings and book loans.

---

## 🏗️ 1. Mobile Project File Structure
Keep the mobile application modular inside the `/mobile/lib` directory:

```text
mobile/lib/
├── main.dart                  # App initialization (providers, themes, routes)
├── models/
│   ├── book.dart              # [NEW] Book catalog model class
│   └── book_loan.dart         # [NEW] Book loan history model class
├── providers/                 # Session / Auth state management (auth_provider.dart)
├── services/
│   ├── auth_service.dart      # REST login caller
│   └── soap_service.dart      # [NEW] SOAP helper (sends XML request bodies)
└── screens/
    ├── login/                 # Login screen (single Matric ID input)
    ├── dashboard/             # Core dashboards (Student vs Admin)
    └── library/               # [NEW] Library screens (Search, Manage, History)
```

---

## 📋 2. Step-by-Step Task Checklist

### Phase 1: Implement the SOAP Service (`lib/services/soap_service.dart`)
- [ ] Create `soap_service.dart` to post XML SOAP requests to the legacy SOAP server at `http://localhost:8085/ws/booking`.
- [ ] Implement the following asynchronous operations in the service:
  - `searchBooks(String query)`: Returns `Future<List<Book>>` (open lookup).
  - `addBook(String token, String isbn, String title, String author, String category)`: Returns `Future<bool>` (Admin only).
  - `borrowBook(String token, String studentId, String studentName, String isbn, String dueDate)`: Returns `Future<String>` loanReference (Admin only).
  - `returnBook(String token, String loanRef)`: Returns `Future<bool>` (Admin only).
  - `getBookLoanHistory(String token, String isbn)`: Returns `Future<List<BookLoan>>` (Admin only).
  - `getStudentLoanHistory(String token, String studentId)`: Returns `Future<List<BookLoan>>`.

### Phase 2: Build the Library UI Screens (`lib/screens/library/`)
- [ ] Create **`screens/library/search_books_screen.dart`**:
  - A clean, modern page with a search textfield.
  - Lists the books returned from `SoapService.searchBooks(...)`.
  - Uses cards with color-coded status badges: Green for **Available** and Red for **Borrowed**.
- [ ] Create **`screens/library/admin_manage_books_screen.dart`**:
  - Accessible only to users with the `ADMIN` role.
  - Floating Action Button to launch the **"Add Book"** dialog form.
  - Floating Action Button to launch the **"Issue/Borrow Book"** form. Requires entering the student matric number, student name, book ISBN, and picking a custom due date using Flutter's `showDatePicker()`.
  - A list of active book loans with a trailing `ElevatedButton` to trigger **"Return Book"** (which updates the loan and catalog instantly).
- [ ] Create **`screens/library/loan_history_screen.dart`**:
  - Displays a clean history log of current and past book loans.
  - Highlights late returns or pending fines in red.

---

## 🛠️ 3. Integration Dart SOAP Template

To invoke SOAP operations in Flutter without massive third-party XML packages, send an **HTTP POST** request with the JAX-WS XML body using Flutter's standard `http` library:

```dart
// lib/services/soap_service.dart

import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:xml/xml.dart' as xml; // Add 'xml: ^6.3.0' to pubspec.yaml
import '../models/book.dart';

class SoapService {
  static const String soapUrl = "http://localhost:8085/ws/booking";

  /**
   * Search books from the catalog.
   */
  static Future<List<Book>> searchBooks(String query) async {
    final String xmlPayload = '''
      <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
         <soapenv:Header/>
         <soapenv:Body>
            <ser:searchBooks>
               <query>$query</query>
            </ser:searchBooks>
         </soapenv:Body>
      </soapenv:Envelope>
    ''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {
        "Content-Type": "text/xml; charset=utf-8",
      },
      body: utf8.encode(xmlPayload),
    );

    if (response.statusCode == 200) {
      return _parseBooksXml(response.body);
    } else {
      throw Exception("Failed to fetch books: Status ${response.statusCode}");
    }
  }

  /**
   * Parses the SOAP XML response into a clean list of Book models
   */
  static List<Book> _parseBooksXml(String xmlBody) {
    final document = xml.XmlDocument.parse(xmlBody);
    final returnElements = document.findAllElements('return');
    
    return returnElements.map((element) {
      return Book(
        id: int.parse(element.findElements('id').first.innerText),
        isbn: element.findElements('isbn').first.innerText,
        title: element.findElements('title').first.innerText,
        author: element.findElements('author').first.innerText,
        category: element.findElements('category').first.innerText,
        status: element.findElements('status').first.innerText,
      );
    }).toList();
  }
}
```

---

## 🎨 4. Premium Mobile UI Guidelines for Flutter
To ensure the app looks gorgeous and satisfies premium coursework standards:

1. **Clean Card-based Layouts**: Use rounded containers (`BorderRadius.circular(16)`) and subtle box shadows.
2. **Modern Colors**: Use sleek, harmonious color schemes. Use HSL/Curated primary colors:
   - Primary: Deep Indigo (`Color(0xFF3F51B5)`)
   - Accent: Mint Green (`Color(0xFF00BFA5)`)
   - Dark Theme background: Matte Obsidian (`Color(0xFF121212)`)
3. **Overdue / Fine alerts**: Use warning colors like soft Crimson red (`Colors.redAccent`) to call attention to late returns.
4. **Idempotent Input Pickers**: Avoid manual typing of dates; always use Flutter's built-in date picker to avoid format conflicts:
   ```dart
   DateTime? pickedDate = await showDatePicker(
       context: context,
       initialDate: DateTime.now().add(Duration(days: 14)),
       firstDate: DateTime.now(),
       lastDate: DateTime.now().add(Duration(days: 90)),
   );
   ```
