import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:xml/xml.dart' as xml;
import '../models/book.dart';
import '../models/book_loan.dart';

class SoapService {
  static const String soapUrl = "http://localhost:8085/ws/booking";

  /**
   * Operation A: bookRoom
   * Creates a room booking.
   */
  static Future<String> bookRoom({
    required String studentId,
    required String studentName,
    required String roomName,
    required String slot,
    required String date,
    required String purpose,
  }) async {
    final xmlPayload = '''
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:bookRoom>
         <studentId>$studentId</studentId>
         <studentName>$studentName</studentName>
         <roomName>$roomName</roomName>
         <slot>$slot</slot>
         <date>$date</date>
         <purpose>$purpose</purpose>
      </ser:bookRoom>
   </soapenv:Body>
</soapenv:Envelope>
''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    final document = xml.XmlDocument.parse(response.body);
    final returnNode = document.findAllElements('return').firstOrNull;
    if (returnNode == null) {
      throw Exception("Invalid bookRoom SOAP response.");
    }
    return returnNode.innerText; // Returns reference ID, e.g. BK-XXXXXXXX
  }

  /**
   * Operation B: checkAvailability
   * Checks if a room slot is available.
   */
  static Future<bool> checkAvailability({
    required String roomName,
    required String slot,
    required String date,
  }) async {
    final xmlPayload = '''
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:checkAvailability>
         <roomName>$roomName</roomName>
         <slot>$slot</slot>
         <date>$date</date>
      </ser:checkAvailability>
   </soapenv:Body>
</soapenv:Envelope>
''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    final document = xml.XmlDocument.parse(response.body);
    final returnNode = document.findAllElements('return').firstOrNull;
    return returnNode?.innerText == 'true';
  }

  /**
   * Operation C: cancelBooking
   * Cancels a room booking.
   */
  static Future<bool> cancelBooking(String bookingRef) async {
    final xmlPayload = '''
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:cancelBooking>
         <bookingRef>$bookingRef</bookingRef>
      </ser:cancelBooking>
   </soapenv:Body>
</soapenv:Envelope>
''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    final document = xml.XmlDocument.parse(response.body);
    final returnNode = document.findAllElements('return').firstOrNull;
    return returnNode?.innerText == 'true';
  }

  /**
   * Helper to parse SOAP Fault errors from the XML response.
   */
  static void _checkSoapFault(String responseBody, int statusCode) {
    try {
      final document = xml.XmlDocument.parse(responseBody);
      final faultNode = document.findAllElements('faultstring').firstOrNull;
      if (faultNode != null) {
        final faultMsg = faultNode.innerText.replaceAll("SOAP_FAULT: ", "");
        throw Exception(faultMsg);
      }
    } catch (e) {
      if (e is Exception) rethrow;
    }
    if (statusCode != 200) {
      throw Exception("Request failed with status code: $statusCode");
    }
  }

  /**
   * Operation 1: searchBooks (Open to Students and Admins)
   * Searches books by title, author, category, or ISBN.
   */
  static Future<List<Book>> searchBooks(String query) async {
    final xmlPayload = '''
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
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    final document = xml.XmlDocument.parse(response.body);
    final returnElements = document.findAllElements('return');

    return returnElements.map((element) {
      return Book(
        id: int.parse(element.findElements('id').first.innerText),
        isbn: element.findElements('isbn').first.innerText,
        title: element.findElements('title').first.innerText,
        author: element.findElements('author').firstOrNull?.innerText,
        category: element.findElements('category').firstOrNull?.innerText,
        status: element.findElements('status').first.innerText,
      );
    }).toList();
  }

  /**
   * Operation 2: addBook (ADMIN only)
   * Registers a new book into the library catalog.
   */
  static Future<bool> addBook({
    required String token,
    required String isbn,
    required String title,
    required String author,
    required String category,
  }) async {
    final xmlPayload = '''
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:addBook>
         <token>$token</token>
         <isbn>$isbn</isbn>
         <title>$title</title>
         <author>$author</author>
         <category>$category</category>
      </ser:addBook>
   </soapenv:Body>
</soapenv:Envelope>
''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    final document = xml.XmlDocument.parse(response.body);
    final returnNode = document.findAllElements('return').firstOrNull;
    return returnNode?.innerText == 'true';
  }

  /**
   * Operation 3: borrowBook (ADMIN only)
   * Creates a book loan transaction with a custom due date deadline.
   */
  static Future<String> borrowBook({
    required String token,
    required String studentId,
    required String studentName,
    required String isbn,
    required String dueDate, // yyyy-MM-dd
  }) async {
    final xmlPayload = '''
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:borrowBook>
         <token>$token</token>
         <studentId>$studentId</studentId>
         <studentName>$studentName</studentName>
         <isbn>$isbn</isbn>
         <dueDate>$dueDate</dueDate>
      </ser:borrowBook>
   </soapenv:Body>
</soapenv:Envelope>
''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    final document = xml.XmlDocument.parse(response.body);
    final returnNode = document.findAllElements('return').firstOrNull;
    if (returnNode == null) {
      throw Exception("Invalid borrowBook SOAP response.");
    }
    return returnNode.innerText; // Returns reference ID, e.g. LN-XXXXXXXX
  }

  /**
   * Operation 4: returnBook (ADMIN only)
   * Confirms return of a borrowed book.
   */
  static Future<bool> returnBook({
    required String token,
    required String loanRef,
  }) async {
    final xmlPayload = '''
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:returnBook>
         <token>$token</token>
         <loanRef>$loanRef</loanRef>
      </ser:returnBook>
   </soapenv:Body>
</soapenv:Envelope>
''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    final document = xml.XmlDocument.parse(response.body);
    final returnNode = document.findAllElements('return').firstOrNull;
    return returnNode?.innerText == 'true';
  }

  /**
   * Operation 5: getBookLoanHistory (ADMIN only)
   * Retrieves the historical list of who has ever borrowed a book.
   */
  static Future<List<BookLoan>> getBookLoanHistory({
    required String token,
    required String isbn,
  }) async {
    final xmlPayload = '''
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:getBookLoanHistory>
         <token>$token</token>
         <isbn>$isbn</isbn>
      </ser:getBookLoanHistory>
   </soapenv:Body>
</soapenv:Envelope>
''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    return _parseBookLoansXml(response.body);
  }

  /**
   * Operation 6: getStudentLoanHistory (STUDENT or ADMIN)
   * Retrieves the historical list of books a student has borrowed.
   */
  static Future<List<BookLoan>> getStudentLoanHistory({
    required String token,
    required String studentId,
  }) async {
    final xmlPayload = '''
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:getStudentLoanHistory>
         <token>$token</token>
         <studentId>$studentId</studentId>
      </ser:getStudentLoanHistory>
   </soapenv:Body>
</soapenv:Envelope>
''';

    final response = await http.post(
      Uri.parse(soapUrl),
      headers: {"Content-Type": "text/xml; charset=utf-8"},
      body: utf8.encode(xmlPayload),
    );

    _checkSoapFault(response.body, response.statusCode);

    return _parseBookLoansXml(response.body);
  }

  /**
   * Private helper to parse XML lists of BookLoan.
   */
  static List<BookLoan> _parseBookLoansXml(String xmlBody) {
    final document = xml.XmlDocument.parse(xmlBody);
    final returnElements = document.findAllElements('return');

    return returnElements.map((element) {
      final retDateVal = element.findElements('returnDate').firstOrNull?.innerText;
      final retDate = (retDateVal == null || retDateVal.trim().isEmpty || retDateVal == 'null') ? null : retDateVal;

      return BookLoan(
        id: int.parse(element.findElements('id').first.innerText),
        loanReference: element.findElements('loanReference').first.innerText,
        studentId: element.findElements('studentId').first.innerText,
        studentName: element.findElements('studentName').firstOrNull?.innerText,
        bookIsbn: element.findElements('bookIsbn').first.innerText,
        bookTitle: element.findElements('bookTitle').firstOrNull?.innerText,
        loanDate: element.findElements('loanDate').first.innerText,
        dueDate: element.findElements('dueDate').first.innerText,
        returnDate: retDate,
        status: element.findElements('status').first.innerText,
        fineAmount: double.parse(element.findElements('fineAmount').first.innerText),
      );
    }).toList();
  }
}
