# 🖥️ Developer Guide: Java Swing Desktop Admin Client

Welcome to the **SmartCampusConnect Desktop Client** development guide! This document provides a complete blueprint, structural architecture, and step-by-step tasks to implement the Java Swing desktop application.

The desktop client is designed primarily as an **Admin/Staff Management Console** but supports logging in as either a Student or an Admin. It communicates with the Spring Boot backend via both **REST API** (Port `8080`) and **SOAP Web Services** (Port `8085`).

---

## 🏗️ 1. Project Architecture (MVC Pattern)
Keep the project organized under the following package structure inside the `/desktop` folder:

```text
desktop/
├── Main.java                  # Application entry point
├── model/                     # Data models (User, Book, BookLoan, etc.)
├── view/                      # Swing JFrames, JPanels, and Dialogs
├── controller/                # Action listeners and UI-to-API routing
├── service/                   # HTTP Client for REST & SOAP requests
└── util/                      # Configuration, JSON parser, Session Storage
```

---

## 📋 2. Step-by-Step Task Checklist

### Phase 1: Authentication & Session Management
- [ ] Create `util/SessionManager.java` to globally store the logged-in user's `token`, `userId`, `role`, and `fullName`.
- [ ] Create `service/RestService.java` to handle HTTP REST requests.
- [ ] Build `view/LoginView.java` (`JFrame`):
  - A clean, modern login screen requiring **User ID only** (no password).
  - Perform a REST `POST` to `http://localhost:8080/api/auth/login` with raw body: `{"userId": "YOUR_ID"}`.
  - If successful, save the UUID token and user details to `SessionManager`, close the login window, and launch the Dashboard.

### Phase 2: Main Dashboard View (`view/DashboardView.java`)
- [ ] Create a master dashboard `JFrame` using a `JTabbedPane` or sidebar navigation.
- [ ] Implement **Student Dashboard Panel**:
  - Call REST `GET /api/dashboard` with the header `X-Auth-Token`.
  - Display personal academic statistics, active room bookings, book loans, and notification logs in clean `JTable` lists.
- [ ] Implement **Admin Dashboard Panel**:
  - Call REST `GET /api/dashboard` with the header `X-Auth-Token` (as Admin).
  - Display system-wide count summary cards (Total Students, Courses, Bookings, Loans).
  - Create tabs for **System Activity Logs** (Room Bookings list and Book Loans list) inside `JTable` tables.

### Phase 3: SOAP Library Manager (Admin Panel)
All library book transactions use JAX-WS SOAP over HTTP (Port `8085` at `http://localhost:8085/ws/booking`).
- [ ] Create `service/SoapService.java` to send XML SOAP Envelopes to the SOAP endpoint.
- [ ] Build **Book Inventory UI**:
  - **Search Books (`searchBooks`)**: A search input and `JTable` showing Title, Author, Category, ISBN, and Status (`AVAILABLE` or `BORROWED`).
  - **Add Book (`addBook`)**: A form popup allowing the Admin to register a new book with its ISBN, Title, Author, and Category. (Requires `token` verification).
- [ ] Build **Book Transactions UI**:
  - **Borrow Book (`borrowBook`)**: A loan form for the Admin to enter the Student's Matric ID, Name, Book ISBN, and a custom return deadline (`yyyy-MM-dd`). Updates book status to `BORROWED`.
  - **Return Book (`returnBook`)**: A return button that clears the loan, calculates any overdue fine (RM1/day), and updates the book status back to `AVAILABLE`.
- [ ] Build **History & Logs UI**:
  - **Book History (`getBookLoanHistory`)**: Displays a tabular history of everyone who has ever borrowed a specific book.
  - **Student History (`getStudentLoanHistory`)**: Displays the borrowing history of a specific student.

---

## 🛠️ 3. Integration Code Templates

### A. How to Call the REST Login API in Java
Use this code in `service/RestService.java` to authenticate:

```java
package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RestService {
    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient client = HttpClient.newHttpClient();

    public String login(String userId) throws Exception {
        String jsonPayload = "{\"userId\":\"" + userId + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body(); // Returns JSON: token, userId, role, fullName
        } else {
            throw new Exception("Authentication failed: Status " + response.statusCode());
        }
    }
}
```

---

### B. How to Call the SOAP Web Service in Java
Rather than setting up complex JAX-WS dependency libraries, the easiest way to invoke SOAP operations in raw Java is by sending an **HTTP POST** request with an **XML payload** to port `8085`:

```java
package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SoapService {
    private static final String SOAP_URL = "http://localhost:8085/ws/booking";
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Executes the SOAP 'addBook' operation.
     * Requires Admin Session Token.
     */
    public String addBook(String token, String isbn, String title, String author, String category) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:addBook>" +
            "         <token>" + token + "</token>" +
            "         <isbn>" + isbn + "</isbn>" +
            "         <title>" + title + "</title>" +
            "         <author>" + author + "</author>" +
            "         <category>" + category + "</category>" +
            "      </ser:addBook>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SOAP_URL))
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(xmlPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body(); // Returns XML containing <return>true</return>
        } else {
            // Parses the SOAP Fault out of the XML response
            throw new Exception("SOAP Error: " + response.body());
        }
    }
}
```

---

### C. Example: XML Template for `borrowBook`
When calling `borrowBook`, send this XML structure:
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      <ser:borrowBook>
         <token>ADMIN_SESSION_TOKEN</token>
         <studentId>STUDENT_MATRIC_NO</studentId>
         <studentName>STUDENT_NAME</studentName>
         <isbn>BOOK_ISBN</isbn>
         <dueDate>YYYY-MM-DD</dueDate>
      </ser:borrowBook>
   </soapenv:Body>
</soapenv:Envelope>
```

---

## 🎨 4. Beautiful UI Guidelines for Swing
Java Swing defaults to standard layouts which can feel outdated. Encourage your teammate to follow these steps to make the GUI feel premium:

1. **Use FlatLaf (Look and Feel)**:
   Add the **FlatLaf** library (e.g. `com.formdev:flatlaf:3.4.1`) to the dependencies or manually jar load, and initialize it at the beginning of `Main.java` inside `main()`:
   ```java
   try {
       UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
   } catch( Exception ex ) {
       System.err.println( "Failed to initialize FlatLaf theme" );
   }
   ```
2. **Padding & Layouts**:
   - Use `EmptyBorder(10, 10, 10, 10)` for panels to give items breathing space.
   - Use `BorderLayout` and `GridBagLayout` for responsive control rather than absolute positioning.
   - Use `JTable` with a customized header font and background colors.

---

*Good luck with the implementation! Keep the backend running using `docker compose up -d` while building the desktop client.*
