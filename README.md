# 🎓 Smart Campus Connect (Microservices Architecture)

SmartCampus Connect is a distributed, multi-platform campus management system. It is designed using a **Microservices / Service-Oriented Architecture (SOA)** featuring database isolation per service, inter-service API orchestration/composition, multithreading protections, and multi-protocol clients (REST + SOAP).

The system integrates three clients (**Web**, **Mobile**, and **Desktop**) communicating with a Spring Boot REST API Gateway and a set of dedicated MySQL databases.

---

## 🏗️ System Architecture (Sequence Diagrams)

These detailed sequence diagrams trace the exact runtime execution flows, service boundaries, protocols, and request/response payloads exchanged between the client apps and backend microservices:

### 1. Dashboard API Composition & Aggregation Flow (Port 8080)
```mermaid
sequenceDiagram
    autonumber
    
    actor User as :Student / Admin
    box rgb(30, 41, 59) "CLIENT LAYER"
        participant Client as :Web / Mobile / Desktop Client
    end

    box rgb(22, 78, 99) "GATEWAY LAYER (Port 8080)"
        participant Gateway as :Student Service (REST Gateway)
    end

    box rgb(17, 24, 39) "MICROSERVICES LAYER"
        participant Enrolment as :Enrolment Service (:8081)
        participant Booking as :Booking Service (:8082)
        participant Notification as :Notification Service (:8083)
    end

    box rgb(63, 63, 70) "DATABASE LAYER"
        participant DB as :db_student (MySQL)
    end

    User->>Client: Open Dashboard
    activate Client
    Client->>Gateway: GET /api/dashboard<br/>Headers: { "X-Auth-Token": "token_uuid" }
    activate Gateway
    
    Gateway->>DB: Query user_sessions where token = "token_uuid"
    activate DB
    DB-->>Gateway: Return Session Profile: { role: "STUDENT", userId: "B032310001", fullName: "Amin" }
    deactivate DB

    Note over Gateway: API Composition (Concurrent WebClient REST Requests)
    
    par Enrolments
        Gateway->>Enrolment: GET /api/enrol/student/1
        activate Enrolment
        Enrolment-->>Gateway: Return List<Enrolment> JSON array
        deactivate Enrolment
    and Room Bookings
        Gateway->>Booking: GET /api/bookings/student/B032310001
        activate Booking
        Booking-->>Gateway: Return List<RoomBooking> JSON array
        deactivate Booking
    and Book Loans
        Gateway->>Booking: GET /api/loans/student/B032310001
        activate Booking
        Booking-->>Gateway: Return List<BookLoan> JSON array
        deactivate Booking
    and Notifications
        Gateway->>Notification: GET /api/notifications/recipient/B032310001
        activate Notification
        Notification-->>Gateway: Return List<Notification> JSON array
        deactivate Notification
    end

    Note over Gateway: Aggregates list data into a unified dashboard response map
    Gateway-->>Client: HTTP 200 OK<br/>JSON Response: { "role": "STUDENT", "profile": { "id": 1, "studentId": "B032310001", "name": "Amin", ... }, "enrolments": [...], "roomBookings": [...], "bookLoans": [...], "notifications": [...] }
    deactivate Gateway
    Client-->>User: Render Dashboard UI metrics & tables
    deactivate Client
```

### 2. Fair-Locked Course Enrolment Flow (REST Proxy through Gateway)
```mermaid
sequenceDiagram
    autonumber
    
    actor User as :Student
    box rgb(30, 41, 59) "CLIENT LAYER"
        participant Client as :Web / Mobile / Desktop Client
    end

    box rgb(22, 78, 99) "GATEWAY LAYER (Port 8080)"
        participant Gateway as :Student Service (REST Gateway)
    end

    box rgb(17, 24, 39) "MICROSERVICES LAYER"
        participant Enrolment as :Enrolment Service (:8081)
        participant Notification as :Notification Service (:8083)
    end

    box rgb(63, 63, 70) "DATABASE LAYER"
        participant DB_Student as :db_student (MySQL)
        participant DB_Enrolment as :db_enrolment (MySQL)
    end

    User->>Client: Click "ENROL" on Course Card
    activate Client
    Client->>Gateway: POST /api/enrol<br/>JSON Payload: { "studentId": 1, "courseCode": "BITP3123" }
    activate Gateway
    Gateway->>Enrolment: Forward POST /api/enrol<br/>JSON Payload: { "studentId": 1, "courseCode": "BITP3123" }
    activate Enrolment
    
    %% Inter-service call to student-service
    Enrolment->>Gateway: GET /api/students/id/1 (Verify Student)
    activate Gateway
    Gateway->>DB_Student: Query student table where id = 1
    activate DB_Student
    DB_Student-->>Gateway: Return student details
    deactivate DB_Student
    Gateway-->>Enrolment: HTTP 200 OK<br/>JSON Response: { "id": 1, "studentId": "B032310001", "name": "Amin", ... }
    deactivate Gateway

    %% Fair locking
    Note over Enrolment: Enrolment Lock acquired (fair ReentrantLock)
    Enrolment->>DB_Enrolment: Query courses table where code = "BITP3123" (capacity validation)
    activate DB_Enrolment
    DB_Enrolment-->>Enrolment: Capacity OK & no active enrolment duplicate
    deactivate DB_Enrolment

    Enrolment->>DB_Enrolment: Insert into enrolments & Increment course.current_capacity
    activate DB_Enrolment
    DB_Enrolment-->>Enrolment: Success
    deactivate DB_Enrolment
    Note over Enrolment: Enrolment Lock released

    %% Async notification
    Enrolment->>Notification: POST /api/notify<br/>JSON Payload: { "type": "ENROLMENT_SUCCESS", "recipientId": "B032310001", "recipientName": "Amin", "message": "Successfully enrolled in BITP3123...", "relatedEntity": "BITP3123" }
    activate Notification
    Note over Notification: Async Fire-and-Forget (.subscribe())
    Notification-->>Enrolment: HTTP 200 OK (Accepted)
    deactivate Notification
    
    Enrolment-->>Gateway: HTTP 201 Created<br/>JSON Response: { "success": true, "message": "Enrolled successfully", "enrolmentId": 12, "remainingSeats": 27 }
    deactivate Enrolment
    Gateway-->>Client: HTTP 201 Created<br/>JSON Response: { "success": true, "message": "Enrolled successfully", "enrolmentId": 12, "remainingSeats": 27 }
    deactivate Gateway
    Client-->>User: Show SnackBar: "Enrolled successfully!"
    deactivate Client
```

### 3. SOAP-Based Library & Booking Flows (Port 8085 JAX-WS)
```mermaid
sequenceDiagram
    autonumber
    
    actor Admin as :Admin
    box rgb(30, 41, 59) "CLIENT LAYER"
        participant Desktop as :Desktop / Mobile Client
    end

    box rgb(17, 24, 39) "MICROSERVICES LAYER"
        participant Booking as :Booking Service (:8085 SOAP)
        participant Gateway as :Student Service (:8080 REST)
        participant Notification as :Notification Service (:8083)
    end

    box rgb(63, 63, 70) "DATABASE LAYER"
        participant DB_Booking as :db_booking (MySQL)
    end

    Admin->>Desktop: Request borrowBook
    activate Desktop
    Desktop->>Booking: SOAP HTTP POST /ws/booking<br/>XML SOAP Envelope (parameters: token, studentId, studentName, isbn, dueDate)
    activate Booking
    
    %% Token Verification Call
    Booking->>Gateway: GET /api/auth/me<br/>Headers: { "X-Auth-Token": "admin_token_uuid" }
    activate Gateway
    Gateway-->>Booking: HTTP 200 OK<br/>JSON Response: { "userId": "ADMIN", "role": "ADMIN", "fullName": "Library Admin" }
    deactivate Gateway

    %% Transaction Logic
    Note over Booking: Verify role == ADMIN. Check book availability in db.
    Booking->>DB_Booking: Insert book_loans record & set books status = "BORROWED"
    activate DB_Booking
    DB_Booking-->>Booking: Success
    deactivate DB_Booking

    %% Notification Dispatch
    Booking->>Notification: POST /api/notify<br/>JSON Payload: { "type": "BOOK_BORROWED", "recipientId": "studentId", "recipientName": "studentName", "message": "Book borrowed...", "relatedEntity": "reference" }
    activate Notification
    Notification-->>Booking: HTTP 200 OK
    deactivate Notification

    Booking-->>Desktop: SOAP Response XML Envelope: <return>LN-20260619-381</return>
    deactivate Booking
    Desktop-->>Admin: Show Loan Details with Loan Reference ID
    deactivate Desktop
```

---


## 🗄️ Database Relationships (ER Diagram)

Each microservice manages its own isolated MySQL database. Relationships are resolved logically via code/API calls rather than physical foreign key constraints.

```mermaid
erDiagram
    %% db_student Schema
    USERS {
        bigint id PK
        varchar user_id UK "Matric / ID"
        enum role "STUDENT / ADMIN"
        varchar full_name
        datetime created_at
    }
    USER_SESSIONS {
        bigint id PK
        varchar token UK "UUID Token"
        varchar user_id "Logical FK to USERS"
        enum role
        varchar full_name
        datetime expires_at
    }
    STUDENTS {
        bigint id PK
        varchar student_id UK "Matric no"
        varchar name
        varchar email UK
        varchar programme
        varchar faculty
        varchar semester
        decimal gpa
        datetime created_at
    }

    %% db_enrolment Schema
    COURSES {
        bigint id PK
        varchar course_code UK
        varchar course_title
        varchar lecturer
        varchar faculty
        int credit_hours
        int current_capacity
        int max_capacity
    }
    ENROLMENTS {
        bigint id PK
        bigint student_id "Logical FK to STUDENTS"
        varchar course_code "Logical FK to COURSES"
        varchar student_name
        varchar course_title
        enum status "ACTIVE / DROPPED / COMPLETED"
    }

    %% db_booking Schema
    BOOKS {
        bigint id PK
        varchar isbn UK
        varchar title
        varchar author
        varchar category
        enum status "AVAILABLE / BORROWED"
    }
    BOOK_LOANS {
        bigint id PK
        varchar loan_reference UK
        varchar student_id "Matric no"
        varchar student_name
        varchar book_isbn
        date loan_date
        date due_date
        date return_date
        enum status "BORROWED / RETURNED / OVERDUE"
        decimal fine_amount
    }
    ROOM_BOOKINGS {
        bigint id PK
        varchar booking_reference UK
        varchar student_id "Matric no"
        varchar room_name
        varchar slot
        date booking_date
        enum status "CONFIRMED / CANCELLED"
    }

    %% db_notification Schema
    NOTIFICATIONS {
        bigint id PK
        varchar type
        varchar recipient_id "Matric / ID"
        varchar recipient_name
        varchar message
        varchar delivery_status "SENT / FAILED"
        varchar channel "HTTP / TCP_SOCKET"
    }

    %% Logical Relationships
    USERS ||--o{ USER_SESSIONS : "generates"
    STUDENTS ||--o{ ENROLMENTS : "enrolls (logical)"
    COURSES ||--o{ ENROLMENTS : "contains (logical)"
    STUDENTS ||--o{ ROOM_BOOKINGS : "books (logical)"
    STUDENTS ||--o{ BOOK_LOANS : "borrows (logical)"
    STUDENTS ||--o{ NOTIFICATIONS : "receives (logical)"
```

---

## 🎓 Coursework Compliance Matrix (R1 - R10)

| Requirement | Concept (Week) | Implementation Details & Mapping |
| :--- | :--- | :--- |
| **R1: System Characterisation** | Week 1 | • Decoupled Microservices with location/access transparency via Gateway Proxies.<br>• Graceful degradation using WebClient fallback logic if a service becomes unavailable. |
| **R2: Architectural Pattern** | Week 2 | • **Multi-tier Microservices**: Separates Presentation layer, business processes, and database persistence. |
| **R3: SOA Principles** | Week 3 | • Services separated into: Student, Enrolment, Booking, and Notification.<br>• **Database-per-Service**: Decoupled databases (`db_student`, `db_enrolment`, `db_booking`, `db_notification`). |
| **R4: Service Composition** | Week 3 | • Student Service (Dashboard) orchestrates REST aggregation across enrolment, booking, and notification services via WebClient.<br>• Internal notifications sent using decoupled HTTP post requests. |
| **R5: Multithreaded Server** | Week 4 | • enrolment-service utilizes a fairness-mode `ReentrantLock` to protect course capacity concurrency states during registration load tests. |
| **R6: Distributed Messaging** | Week 5 | • Implements a custom non-blocking notification server listening on TCP Socket port `9090` (Producer-Consumer pattern). |
| **R7: REST API** | Week 6 | • REST APIs exposed via API Gateway controllers at port `8080` (handles proxy routing to `/api/enrol`, `/api/courses`, and `/api/notifications`). |
| **R8: SOAP Service** | Week 7 | • SOAP/WSDL endpoints exposed in `booking-service` at port `8085` using JAX-WS. |
| **R9: Failure Handling** | Weeks 1, 4 | • Isolated microservices prevent cascade failures; failing to query one service fallbacks to empty list data without crashing the dashboard. |
| **R10: Version Control & Build** | Engineering Practice | • Decoupled build containers configured in `docker-compose.yml` allowing single-command startup. |

---

## 🌐 API Reference (REST & SOAP)

### 1. REST API Endpoints (Gateway Port 8080)

All HTTP REST endpoints listed below are routed via the Gateway (`student-service` on port `8080`) to ensure backend location transparency.

| Category | Method | Endpoint / URL | Request Payload (JSON) | Description |
| :--- | :---: | :--- | :--- | :--- |
| **🔐 Auth** | `POST` | `/api/auth/login` | `{ "userId": "B032310001" }` | Logs in a user by ID only (matric number or `ADMIN`). |
| | `GET` | `/api/auth/me` | *None* | Retrieves active session context (requires `X-Auth-Token` header). |
| | `POST` | `/api/auth/logout` | *None* | Invalidates and destroys the active session. |
| **👤 Student** | `GET` | `/api/students` | *None* | Lists all student profiles in the database. |
| | `GET` | `/api/students/{matricNo}` | *None* | Retrieves student profile by matriculation number. |
| | `GET` | `/api/students/id/{id}` | *None* | Retrieves student profile by database ID. |
| | `POST` | `/api/students` | `{ "name": "Amin", "email": "amin@...", "programme": "...", "faculty": "FTMK", "semester": "1", "gpa": 3.60, "phoneNumber": "..." }` | Registers a new student and generates login credentials. |
| | `PUT` | `/api/students/{id}` | `{ "name": "Amin New", "gpa": 3.70 }` | Updates profile info by database ID. |
| | `DELETE` | `/api/students/{id}` | *None* | Deletes student profile and login credentials. |
| **📝 Enrolment**| `GET` | `/api/courses` | *None* | Returns a list of all courses. |
| | `POST` | `/api/courses` | `{ "courseCode": "BITP3123", "courseTitle": "Distributed Apps", "lecturer": "Dr. R", "faculty": "FTMK", "creditHours": 3, "maxCapacity": 30, "semester": "2024/2025 SEM 1" }` | Creates a new course offering. |
| | `POST` | `/api/enrol` | `{ "studentId": 1, "courseCode": "BITP3123" }` | Enrolls a student into a course. Protected by `ReentrantLock`. |
| | `DELETE` | `/api/enrol/{studentId}/{courseCode}`| *None* | Drops a course registration. |
| | `GET` | `/api/enrol/student/{studentId}`| *None* | Gets all course registrations for a student. |
| | `POST` | `/api/enrol/load-test/{courseCode}`| *None* | Triggers load test (10 concurrent threads booking remaining 3 seats). |
| **🔔 Alerts** | `GET` | `/api/notifications` | *None* | Lists all logged system alerts. |
| | `GET` | `/api/notifications/recipient/{id}`| *None* | Retrieves logs for a specific recipient (matric number). |

### 2. SOAP Web Services (Port 8085)

The SOAP service is published directly by `booking-service` at: `http://localhost:8085/ws/booking`.

| Operation Name | Input Parameters | Output / Return | Description |
| :--- | :--- | :--- | :--- |
| `bookRoom` | `studentId`, `studentName`, `roomName`, `slot`, `date`, `purpose` | `String` (Booking Ref) | Confirms a room booking. SOAP Fault if slot is already booked. |
| `checkAvailability`| `roomName`, `slot`, `date` | `boolean` | Checks if a room slot is available. |
| `cancelBooking` | `bookingRef` | `boolean` | Cancels a room booking. |
| `borrowBook` | `token`, `studentId`, `studentName`, `isbn`, `dueDate` | `String` (Loan Ref) | Admin lends a book. |
| `returnBook` | `token`, `loanRef` | `boolean` | Admin records a book return (calculates fine). |
| `addBook` | `token`, `isbn`, `title`, `author`, `category` | `boolean` | Admin adds a book. |
| `searchBooks` | `query` | `List<Book>` | Searches book catalog. |

---

## 🗄️ Data Dictionary

### 1. Database: `db_student` (student-service)

#### Table: `users`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Internal surrogate identifier. |
| `user_id` | VARCHAR(20) | Unique, Indexed | Student matric number or `ADMIN`. |
| `role` | ENUM | `STUDENT` / `ADMIN` | User access level. |
| `full_name` | VARCHAR(100) | — | Display name of the user. |
| `created_at` | DATETIME | — | Record creation timestamp. |

#### Table: `user_sessions`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Session primary identifier. |
| `token` | VARCHAR(100) | Unique, Indexed | Session UUID token. |
| `user_id` | VARCHAR(20) | Indexed | Target user ID. |
| `role` | ENUM | `STUDENT` / `ADMIN` | Session role. |
| `full_name` | VARCHAR(100) | — | Display name of session owner. |
| `expires_at` | DATETIME | — | Expiration timestamp (default 24 hours). |

#### Table: `students`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Student record primary key. |
| `student_id`| VARCHAR(20) | Unique | Matric number (e.g. `B032310001`). |
| `name` | VARCHAR(100) | — | Full name. |
| `email` | VARCHAR(150) | Unique | Student email. |
| `programme` | VARCHAR(100) | — | Major field of study. |
| `faculty` | VARCHAR(50) | — | Faculty identifier (e.g. `FTMK`). |
| `semester` | VARCHAR(10) | — | Current semester. |
| `gpa` | DECIMAL(4,2) | — | Cumulative GPA. |
| `phone_number`| VARCHAR(15) | — | Contact number. |

---

### 2. Database: `db_enrolment` (enrolment-service)

#### Table: `courses`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Course identifier. |
| `course_code`| VARCHAR(20) | Unique | Course code (e.g. `BITP3123`). |
| `course_title`| VARCHAR(150)| — | Course name. |
| `lecturer` | VARCHAR(100) | — | Lecturer name. |
| `faculty` | VARCHAR(50) | — | Hosting faculty. |
| `credit_hours`| INT | Default 3 | Credit weight. |
| `current_capacity`| INT | Default 0 | Current enrolled seats. |
| `max_capacity`| INT | Default 30 | Maximum capacity limit. |

#### Table: `enrolments`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Enrolment identifier. |
| `student_id`| BIGINT | Composite Unique (with course_code) | Logical reference to Student ID on `db_student`. |
| `course_code`| VARCHAR(20) | Composite Unique (with student_id) | Target course code. |
| `student_name`| VARCHAR(100)| — | Denormalized student name. |
| `course_title`| VARCHAR(150)| — | Denormalized course title. |
| `status` | ENUM | `ACTIVE` / `DROPPED` / `COMPLETED` | Registration status. |

---

### 3. Database: `db_booking` (booking-service)

#### Table: `books`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Book ID. |
| `isbn` | VARCHAR(20) | Unique | Book ISBN-13 code. |
| `title` | VARCHAR(200) | — | Book title. |
| `author` | VARCHAR(150) | — | Author name. |
| `category` | VARCHAR(100) | — | Genre/Category. |
| `status` | ENUM | `AVAILABLE` / `BORROWED` | Book status. |

#### Table: `book_loans`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Loan identifier. |
| `loan_reference`| VARCHAR(30)| Unique | Unique loan reference (e.g. `LN-XXXXXXXX`). |
| `student_id`| VARCHAR(20) | — | Student matric number. |
| `student_name`| VARCHAR(100)| — | Student name. |
| `book_isbn` | VARCHAR(20) | — | ISBN of the borrowed book. |
| `loan_date` | DATE | — | Loan date. |
| `due_date` | DATE | — | Due deadline date. |
| `return_date`| DATE | Nullable | Actual returned date. |
| `status` | ENUM | `BORROWED` / `RETURNED` / `OVERDUE` | Loan status. |
| `fine_amount`| DECIMAL(8,2)| Default 0.00 | Fine amount (RM1/day overdue). |

#### Table: `room_bookings`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Booking identifier. |
| `booking_reference`| VARCHAR(30)| Unique | Unique booking reference (e.g. `BK-XXXXXXXX`). |
| `student_id`| VARCHAR(20) | — | Student matric number. |
| `room_name` | VARCHAR(50) | Composite Unique (with slot & date) | Reserved room name. |
| `slot` | VARCHAR(50) | Composite Unique | Reserved slot. |
| `booking_date`| DATE | Composite Unique | Reserved date. |
| `status` | ENUM | `CONFIRMED` / `CANCELLED` | Booking status. |

---

### 4. Database: `db_notification` (notification-service)

#### Table: `notifications`
| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto-Increment | Notification identifier. |
| `type` | VARCHAR(30) | — | Notification event type. |
| `recipient_id`| VARCHAR(20) | — | Target recipient matric number or `ADMIN`. |
| `recipient_name`| VARCHAR(100)| — | Target recipient name. |
| `message` | VARCHAR(500)| — | Alert content message. |
| `delivery_status`| VARCHAR(10)| — | Status (`SENT` / `FAILED`). |
| `channel` | VARCHAR(20) | — | Transport channel (`HTTP` / `TCP_SOCKET`). |

---

## 🚀 Instruction Guide

Follow these steps to run the various components of the project:

### Step 1: Running the Microservices backend (Docker)

Make sure **Docker Desktop** is open and running in the background.

1. Open your terminal in the root folder of the project (`SmartCampusConnect`).
2. Run docker compose to compile and launch all microservices and databases:
   ```bash
   docker compose up --build -d
   ```
3. Check execution status:
   ```bash
   docker compose ps
   ```

---

### Step 2: Running the Web Client (Vanilla JS)

*   **Option A (Using Docker)**: The Web Client is automatically served and exposed at: [http://localhost:3000](http://localhost:3000).
*   **Option B (Python Local Web Server)**:
     1. Open your terminal in the `web/` folder:
         ```bash
         cd web
         ```
     2. Start the HTTP server:
         ```bash
         python3 -m http.server 3000
         ```
     3. Visit: [http://localhost:3000/index.html](http://localhost:3000/index.html).

**Note:** The Web Client looks for a project logo at `web/omgosh-logo.png`. To display your logo in the dashboard, place a PNG named `omgosh-logo.png` in the `web/` folder or update the path in `web/js/views/dashboard.js`.

### Web changelog (recent)

- Updated web UI: mobile-responsive layout for dashboard, library, login, and profile.
- Restored visible circular spinner and improved loader behavior.
- Added support for `omgosh-logo.png` in the dashboard header (place the file in `web/`).
- Corrected web run instructions and added notes for local serving.

---

### Step 3: Running the Desktop Admin Client (Java Swing)

1. Open your terminal in the `desktop/` folder:
   ```bash
   cd desktop
   ```
2. Compile the Java files:
   * **macOS/Linux:**
     ```bash
     ./compile.sh
     ```
   * **Windows (Command Prompt / PowerShell):**
     ```cmd
     compile.bat
     ```
3. Run the desktop application:
   * **macOS/Linux:**
     ```bash
     ./run.sh
     ```
   * **Windows (Command Prompt / PowerShell):**
     ```cmd
     run.bat
     ```

---

### Step 4: Running the Mobile Client (Flutter)

1. Open your terminal in the `mobile/` folder:
   ```bash
   cd mobile
   ```
2. Get Flutter packages:
   ```bash
   flutter pub get
   ```
3. Connect your device/emulator and run the application:
   ```bash
   flutter run
   ```

---

## 📁 Project Directory Structure

```text
SmartCampusConnect/
├── .env                       # Environment variables config
├── docker-compose.yml         # Docker Compose orchestration
├── backend/                   # ☕ student-service & REST Gateway
├── enrolment-service/         # ☕ enrolment-service (Port 8081)
├── booking-service/           # ☕ booking-service (Port 8082/8085)
├── notification-service/      # ☕ notification-service (Port 8083/9090)
├── web/                       # 🌐 Web Frontend Client
├── mobile/                    # 📱 Mobile Client (Flutter App)
└── desktop/                   # 🖥️ Desktop Client (Java Swing Admin Console)
```