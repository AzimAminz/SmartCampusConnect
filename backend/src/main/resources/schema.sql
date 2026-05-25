-- =========================================================================
-- SMART CAMPUS CONNECT - DATABASE SCHEMA
-- =========================================================================
-- Database  : smartcampus
-- Engine    : MySQL 8.0
-- Charset   : utf8mb4
-- Collation : utf8mb4_unicode_ci
--
-- This schema is auto-generated as reference documentation.
-- The actual tables are created automatically by Hibernate (JPA)
-- when the Spring Boot backend starts (ddl-auto=update).
--
-- Services & their tables:
--   1. Auth Service              → users, user_sessions
--   2. Student Profile Service   → students
--   3. Course Enrolment Service  → courses, enrolments
--   4. Library/Booking Service   → room_bookings, book_loans  (exposed via SOAP)
--   5. Notification Service      → notifications  (populated by TCP socket events)
-- =========================================================================

CREATE DATABASE IF NOT EXISTS `smartcampus`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `smartcampus`;

-- =========================================================================
-- TABLE 1: users
-- Service  : Auth Service
-- Protocol : REST (POST /api/auth/login, GET /api/auth/me, POST /api/auth/logout)
-- NOTE     : No password required. Login is done using userId only.
--            Pelajar  → userId = matric number (e.g. B032310001)
--            Admin    → userId = "ADMIN"
-- =========================================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     VARCHAR(20)  NOT NULL COMMENT 'Matric number or ADMIN',
    `role`        ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
    `full_name`   VARCHAR(100) DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_users_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Auth Service - user accounts (no password, login by userId only)';

-- =========================================================================
-- TABLE 2: user_sessions
-- Service  : Auth Service
-- Protocol : REST (via X-Auth-Token header)
-- NOTE     : Token is a UUID string. Expires after 24 hours.
-- =========================================================================
CREATE TABLE IF NOT EXISTS `user_sessions` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `token`       VARCHAR(100) NOT NULL COMMENT 'UUID token returned after login',
    `user_id`     VARCHAR(20)  NOT NULL COMMENT 'Matric number or ADMIN',
    `role`        ENUM('STUDENT','ADMIN') NOT NULL,
    `full_name`   VARCHAR(100) DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at`  DATETIME     NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL 24 HOUR),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_sessions_token` (`token`),
    KEY `idx_user_sessions_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Auth Service - active login sessions with expiry tracking';

-- =========================================================================
-- TABLE 3: students
-- Service  : Student Profile Service
-- Protocol : REST  (GET/POST/PUT/DELETE /api/students)
-- =========================================================================
CREATE TABLE IF NOT EXISTS `students` (
    `id`             BIGINT          NOT NULL AUTO_INCREMENT,
    `student_id`     VARCHAR(20)     NOT NULL COMMENT 'Official student number e.g. B032310001',
    `name`           VARCHAR(100)    NOT NULL,
    `email`          VARCHAR(150)    NOT NULL,
    `programme`      VARCHAR(100)    DEFAULT NULL COMMENT 'e.g. Bachelor of Computer Science',
    `faculty`        VARCHAR(50)     DEFAULT NULL COMMENT 'e.g. FTMK',
    `semester`       VARCHAR(10)     NOT NULL DEFAULT '1',
    `gpa`            DECIMAL(4,2)    DEFAULT NULL,
    `phone_number`   VARCHAR(15)     DEFAULT NULL,
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_students_email`      (`email`),
    UNIQUE KEY `uq_students_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Student Profile Service - stores student demographic and academic data';

-- =========================================================================
-- TABLE 4: courses
-- Service  : Course Enrolment Service
-- Protocol : REST  (GET/POST /api/courses)
-- R5 NOTE  : current_capacity is the SHARED MUTABLE STATE protected by
--            ReentrantLock inside EnrolmentService.java
-- =========================================================================
CREATE TABLE IF NOT EXISTS `courses` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `course_code`       VARCHAR(20)  NOT NULL COMMENT 'e.g. BITP3123, CS301',
    `course_title`      VARCHAR(150) NOT NULL,
    `lecturer`          VARCHAR(100) DEFAULT NULL,
    `faculty`           VARCHAR(50)  DEFAULT NULL,
    `credit_hours`      INT          NOT NULL DEFAULT 3,
    `current_capacity`  INT          NOT NULL DEFAULT 0 COMMENT 'Protected by ReentrantLock (R5)',
    `max_capacity`      INT          NOT NULL DEFAULT 30,
    `semester`          VARCHAR(20)  DEFAULT NULL COMMENT 'e.g. 2024/2025 SEM 1',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_courses_code` (`course_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Course Enrolment Service - course catalog with capacity management';

-- =========================================================================
-- TABLE 5: enrolments
-- Service  : Course Enrolment Service
-- Protocol : REST  (POST /api/enrol, GET /api/enrol/{studentId}, DELETE /api/enrol/{id})
-- R4 NOTE  : Enrolment triggers async notification event (choreography)
-- R5 NOTE  : Concurrent enrolment protected by ReentrantLock
-- =========================================================================
CREATE TABLE IF NOT EXISTS `enrolments` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `student_id`    BIGINT       NOT NULL COMMENT 'FK-like reference to students.id',
    `course_code`   VARCHAR(20)  NOT NULL COMMENT 'FK-like reference to courses.course_code',
    `student_name`  VARCHAR(100) DEFAULT NULL COMMENT 'Denormalized for display',
    `course_title`  VARCHAR(150) DEFAULT NULL COMMENT 'Denormalized for display',
    `status`        ENUM('ACTIVE','DROPPED','COMPLETED') NOT NULL DEFAULT 'ACTIVE',
    `enrolled_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `dropped_at`    DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_student_course` (`student_id`, `course_code`),
    KEY `idx_enrolments_student`  (`student_id`),
    KEY `idx_enrolments_course`   (`course_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Enrolments junction table - links students to courses with status tracking';

-- =========================================================================
-- TABLE 6: room_bookings
-- Service  : Library / Booking Service (LEGACY SYSTEM SIMULATION)
-- Protocol : SOAP/WSDL  (http://localhost:8085/ws/booking)
-- R8 NOTE  : Managed by JAX-WS CampusBookingSoapService.
--            Operations: bookRoom, cancelBooking, checkAvailability
--            SOAP Fault triggered on duplicate (room_name + slot + booking_date).
-- =========================================================================
CREATE TABLE IF NOT EXISTS `room_bookings` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT,
    `booking_reference` VARCHAR(30) NOT NULL COMMENT 'e.g. BK-20250525-001',
    `student_id`        VARCHAR(20) NOT NULL,
    `student_name`      VARCHAR(100) DEFAULT NULL,
    `room_name`         VARCHAR(50) NOT NULL COMMENT 'e.g. DK-A, Library Room 1',
    `slot`              VARCHAR(50) NOT NULL COMMENT 'e.g. Monday 9AM-11AM',
    `booking_date`      DATE        NOT NULL,
    `status`            ENUM('CONFIRMED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'CONFIRMED',
    `purpose`           VARCHAR(300) DEFAULT NULL,
    `booked_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `cancelled_at`      DATETIME    DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_booking_reference`   (`booking_reference`),
    UNIQUE KEY `uq_room_slot_date`      (`room_name`, `slot`, `booking_date`),
    KEY `idx_room_bookings_student`     (`student_id`),
    KEY `idx_room_bookings_date`        (`booking_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Library/Booking Service - discussion room bookings via SOAP (R8)';

-- =========================================================================
-- TABLE 7: books
-- Service  : Library / Booking Service
-- Protocol : SOAP/WSDL  (operations: searchBooks, addBook, borrowBook, returnBook)
-- =========================================================================
CREATE TABLE IF NOT EXISTS `books` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `isbn`        VARCHAR(20)  NOT NULL COMMENT 'Unique Book ISBN',
    `title`       VARCHAR(200) NOT NULL,
    `author`      VARCHAR(150) DEFAULT NULL,
    `category`    VARCHAR(100) DEFAULT NULL,
    `status`      ENUM('AVAILABLE','BORROWED') NOT NULL DEFAULT 'AVAILABLE',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_books_isbn` (`isbn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Library/Booking Service - master book catalog';

-- =========================================================================
-- TABLE 8: book_loans
-- Service  : Library / Booking Service
-- Protocol : SOAP/WSDL  (operations: borrowBook, returnBook)
-- R8 NOTE  : Admin confirms return via SOAP returnBook operation.
-- =========================================================================
CREATE TABLE IF NOT EXISTS `book_loans` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `loan_reference`  VARCHAR(30)  NOT NULL COMMENT 'e.g. LN-20250525-001',
    `student_id`      VARCHAR(20)  NOT NULL,
    `student_name`    VARCHAR(100) DEFAULT NULL,
    `book_isbn`       VARCHAR(20)  NOT NULL,
    `book_title`      VARCHAR(200) DEFAULT NULL,
    `loan_date`       DATE         NOT NULL,
    `due_date`        DATE         NOT NULL COMMENT 'Typically 14 days from loan_date',
    `return_date`     DATE         DEFAULT NULL COMMENT 'NULL until book is returned',
    `status`          ENUM('BORROWED','RETURNED','OVERDUE','LOST') NOT NULL DEFAULT 'BORROWED',
    `fine_amount`     DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT 'Late return fee in RM',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_loan_reference` (`loan_reference`),
    KEY `idx_book_loans_student`   (`student_id`),
    KEY `idx_book_loans_isbn`      (`book_isbn`),
    KEY `idx_book_loans_status`    (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Library/Booking Service - book loan records with overdue and fine tracking';

-- =========================================================================
-- TABLE 9: notifications
-- Service  : Notification Service
-- Protocol : Internal TCP Socket (port 9090) — NOT exposed via HTTP
-- R6 NOTE  : This table logs every event pushed by the TCP Producer services.
--            The TCP Consumer (NotificationTcpServer) writes records here.
-- =========================================================================
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `type`            ENUM(
                          'ENROLMENT_SUCCESS',
                          'ENROLMENT_FAILED',
                          'ENROLMENT_DROPPED',
                          'ROOM_BOOKED',
                          'ROOM_CANCELLED',
                          'BOOK_BORROWED',
                          'BOOK_RETURNED',
                          'BOOK_OVERDUE',
                          'PAYMENT_DUE',
                          'SYSTEM_ALERT'
                      ) NOT NULL,
    `recipient_id`    VARCHAR(20)  NOT NULL COMMENT 'Student ID or staff ID',
    `recipient_name`  VARCHAR(100) DEFAULT NULL,
    `message`         VARCHAR(500) NOT NULL,
    `related_entity`  VARCHAR(50)  DEFAULT NULL COMMENT 'Course code or booking reference',
    `delivery_status` ENUM('SENT','FAILED','PENDING') NOT NULL DEFAULT 'SENT',
    `channel`         VARCHAR(20)  NOT NULL DEFAULT 'TCP_SOCKET',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notifications_recipient` (`recipient_id`),
    KEY `idx_notifications_type`      (`type`),
    KEY `idx_notifications_created`   (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Notification Service - log of all asynchronous events consumed via TCP socket (R6)';


-- =========================================================================
-- SAMPLE SEED DATA (for testing and demonstration)
-- Hibernate auto-creates tables; this provides initial demo records.
-- =========================================================================

-- Auth Users
INSERT IGNORE INTO `users` (`user_id`, `role`, `full_name`) VALUES
('ADMIN',       'ADMIN',   'System Administrator'),
('B032310001',  'STUDENT', 'Muhammad Azim bin Aminudin'),
('B032310002',  'STUDENT', 'Nur Aisyah binti Ahmad'),
('B032310003',  'STUDENT', 'Muhammad Hafiz bin Razali'),
('B032310004',  'STUDENT', 'Siti Nurhaliza binti Zainal'),
('B032310005',  'STUDENT', 'Ahmad Faris bin Othman');

-- Sample Students
INSERT IGNORE INTO `students` (`student_id`, `name`, `email`, `programme`, `faculty`, `semester`, `gpa`, `phone_number`) VALUES
('B032310001', 'Muhammad Azim bin Aminudin',  'azim@student.utem.edu.my',     'Bachelor of Computer Science (Hons)', 'FTMK', '1', 3.80, '0123456781'),
('B032310002', 'Nur Aisyah binti Ahmad',      'aisyah@student.utem.edu.my',   'Bachelor of Computer Science (Hons)', 'FTMK', '1', 3.65, '0123456782'),
('B032310003', 'Muhammad Hafiz bin Razali',   'hafiz@student.utem.edu.my',    'Bachelor of Computer Science (Hons)', 'FTMK', '1', 3.72, '0123456783'),
('B032310004', 'Siti Nurhaliza binti Zainal', 'nurhaliza@student.utem.edu.my','Bachelor of Computer Science (Hons)', 'FTMK', '1', 3.55, '0123456784'),
('B032310005', 'Ahmad Faris bin Othman',      'faris@student.utem.edu.my',    'Bachelor of Computer Science (Hons)', 'FTMK', '1', 3.90, '0123456785');

-- Sample Courses
INSERT IGNORE INTO `courses` (`course_code`, `course_title`, `lecturer`, `faculty`, `credit_hours`, `current_capacity`, `max_capacity`, `semester`) VALUES
('BITP3123', 'Distributed Application Development', 'Dr. Ramli',       'FTMK', 3, 0, 30, '2024/2025 SEM 1'),
('BITP3143', 'Mobile Application Development',      'Dr. Farahwahida', 'FTMK', 3, 0, 30, '2024/2025 SEM 1'),
('BITM3073', 'Network Security',                    'Dr. Azuan',       'FTMK', 3, 0, 25, '2024/2025 SEM 1'),
('BITU3033', 'Software Engineering',                'Dr. Ruzaini',     'FTMK', 3, 0, 35, '2024/2025 SEM 1'),
('BITP3113', 'Web Application Development',         'Dr. Shahrol',     'FTMK', 3, 0, 30, '2024/2025 SEM 1');

-- Sample Books
INSERT IGNORE INTO `books` (`isbn`, `title`, `author`, `category`, `status`) VALUES
('9780134685991', 'Effective Java', 'Joshua Bloch', 'Software Engineering', 'AVAILABLE'),
('9780132350884', 'Clean Code', 'Robert C. Martin', 'Software Engineering', 'AVAILABLE'),
('9780135957059', 'The Pragmatic Programmer', 'David Thomas', 'Software Engineering', 'AVAILABLE'),
('9780321356680', 'Design Patterns', 'Erich Gamma', 'Computer Science', 'AVAILABLE'),
('9780134092669', 'Introduction to Algorithms', 'Thomas H. Cormen', 'Algorithms', 'AVAILABLE');
