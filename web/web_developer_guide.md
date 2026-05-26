# 🌐 Developer Guide: Web SPA Client (REST + SOAP)

Welcome to the **SmartCampusConnect Web Client** developer guide! This document outlines the files, architecture, and step-by-step tasks to implement the new Library Master Book Catalog and Secure Transactions inside the **Vanilla HTML/CSS/JS Single Page Application (SPA)**.

The web client communicates with:
- **REST API** (Port `8080`) for Auth, Dashboard Stats, Profile, and Enrolment.
- **SOAP API** (Port `8085` at `http://localhost:8085/ws/booking`) for Room Bookings and Book Loans.

---

## 🏗️ 1. Web Project File Structure
All Javascript files should remain modular under the `/web/js` directory:

```text
web/
├── view/
│   └── index.html             # Main entry point (served by Nginx)
├── css/
│   └── styles.css             # Harmonious dark/light CSS styles
└── js/
    ├── app.js                 # Central SPA Router and Session initialization
    ├── services/
    │   ├── authService.js     # REST Auth caller
    │   └── libraryService.js  # [NEW] SOAP Library Client (search, borrow, return, history)
    └── views/
        ├── login.js           # Matric/ADMIN Login panel
        └── dashboard.js       # Dynamic Student & Admin view renderer
```

---

## 📋 2. Step-by-Step Task Checklist

### Phase 1: Create the SOAP Client Service
- [ ] Create `js/services/libraryService.js` to dispatch AJAX `fetch` calls containing SOAP XML payloads to port `8085`.
- [ ] Implement the following functions inside `libraryService.js`:
  - `searchBooks(query)`: Open to all. Returns a list of books matching the query.
  - `addBook(isbn, title, author, category)`: Admin only (passes session token).
  - `borrowBook(studentId, studentName, isbn, dueDate)`: Admin only (passes session token and custom due date).
  - `returnBook(loanRef)`: Admin only (passes session token).
  - `getBookLoanHistory(isbn)`: Admin only (passes session token).
  - `getStudentLoanHistory(studentId)`: Admin & student (passes session token).

### Phase 2: Implement Student Library UI
- [ ] Open `js/views/dashboard.js` and locate the Student dashboard rendering sections.
- [ ] Add a **Library Tab** containing:
  - **Search Bar**: A search input that fires `libraryService.searchBooks(query)` on keyup or search click.
  - **Books Grid/List**: Displays book card components (Title, Author, Category, ISBN) with a colored badge showing status:
    - Green badge: `AVAILABLE`
    - Red badge: `BORROWED` (indicating it is currently out)
  - **Personal History**: Renders a list of the student's active loans and past borrowing history (returned books, overdue fines paid) using `libraryService.getStudentLoanHistory(studentId)`.

### Phase 3: Implement Admin Library Manager UI
- [ ] Open `js/views/dashboard.js` and locate the Admin dashboard rendering sections.
- [ ] Add an **Admin Library Control Center Tab** containing:
  - **Search & Stats**: Search all books and view active loans.
  - **Register Book Modal**: A button to open an "Add Book" modal. It takes ISBN, Title, Author, Category and calls `libraryService.addBook(...)`.
  - **Issue Book Modal**: A modal allowing the Admin to input the student's Matric No, Name, ISBN, and select a **custom return date** (via `<input type="date">`). Calls `libraryService.borrowBook(...)`.
  - **Return Book Action**: Add a "Confirm Return" button next to active loans. On click, it calls `libraryService.returnBook(...)` which triggers late fine calculation.
  - **Book History Search**: Allows the Admin to search by book ISBN and see a timeline of who has ever borrowed that copy.

---

## 🛠️ 3. Integration JavaScript SOAP Template
Since the Web Client is a pure Vanilla JS SPA, you can use the built-in browser `fetch` API to make direct asynchronous SOAP XML requests:

```javascript
// js/services/libraryService.js

const SOAP_URL = "http://localhost:8085/ws/booking";

export const libraryService = {
    /**
     * Search the master books catalog.
     * Open to all users (does not require auth token).
     */
    async searchBooks(query = "") {
        const xmlPayload = `
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
               <soapenv:Header/>
               <soapenv:Body>
                  <ser:searchBooks>
                     <query>${query}</query>
                  </ser:searchBooks>
               </soapenv:Body>
            </soapenv:Envelope>
        `;

        const response = await fetch(SOAP_URL, {
            method: "POST",
            headers: {
                "Content-Type": "text/xml"
            },
            body: xmlPayload
        });

        const xmlText = await response.text();
        return parseBooksXml(xmlText); // Parses the XML into a clean JSON array
    }
};

/**
 * Quick Helper to parse searchBooks SOAP XML responses into JSON
 */
function parseBooksXml(xmlString) {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlString, "text/xml");
    const bookNodes = xmlDoc.getElementsByTagName("return");
    const books = [];

    for (let node of bookNodes) {
        books.push({
            id: node.getElementsByTagName("id")[0]?.textContent,
            isbn: node.getElementsByTagName("isbn")[0]?.textContent,
            title: node.getElementsByTagName("title")[0]?.textContent,
            author: node.getElementsByTagName("author")[0]?.textContent,
            category: node.getElementsByTagName("category")[0]?.textContent,
            status: node.getElementsByTagName("status")[0]?.textContent
        });
    }
    return books;
}
```

---

## 🎨 4. Premium SPA Design Aesthetics
To keep the web frontend premium and responsive:
1. **Glassmorphism**: Use translucent backdrops for panels and modals:
   ```css
   .glass-card {
       background: rgba(255, 255, 255, 0.05);
       backdrop-filter: blur(10px);
       border: 1px solid rgba(255, 255, 255, 0.1);
       border-radius: 16px;
   }
   ```
2. **Micro-Animations**: Add smooth transition hover states:
   ```css
   .book-card {
       transition: transform 0.2s ease, box-shadow 0.2s ease;
   }
   .book-card:hover {
       transform: translateY(-4px);
       box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
   }
   ```
