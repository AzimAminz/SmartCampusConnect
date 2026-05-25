package smartcampus.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Library / Booking Service - Book Loan Entity
 * Maps to table: book_loans
 * Satisfies: R3 (SOA - Library Service), R8 (SOAP operations for loan management)
 */
@Entity
@Table(name = "book_loans")
public class BookLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_reference", nullable = false, unique = true, length = 30)
    private String loanReference;  // e.g. LN-20250525-001

    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @Column(name = "student_name", length = 100)
    private String studentName;

    @Column(name = "book_isbn", nullable = false, length = 20)
    private String bookIsbn;

    @Column(name = "book_title", length = 200)
    private String bookTitle;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;    // Typically 14 days from loan date

    @Column(name = "return_date")
    private LocalDate returnDate; // NULL until returned

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private LoanStatus status = LoanStatus.BORROWED;

    @Column(name = "fine_amount")
    private Double fineAmount = 0.0; // Late return fee in RM

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum LoanStatus {
        BORROWED,   // Book is currently borrowed
        RETURNED,   // Book has been returned
        OVERDUE,    // Past due date and not returned
        LOST        // Book reported lost
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Constructors ----
    public BookLoan() {}

    public BookLoan(String loanReference, String studentId, String studentName,
                    String bookIsbn, String bookTitle, LocalDate loanDate, LocalDate dueDate) {
        this.loanReference = loanReference;
        this.studentId = studentId;
        this.studentName = studentName;
        this.bookIsbn = bookIsbn;
        this.bookTitle = bookTitle;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.status = LoanStatus.BORROWED;
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoanReference() { return loanReference; }
    public void setLoanReference(String loanReference) { this.loanReference = loanReference; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public Double getFineAmount() { return fineAmount; }
    public void setFineAmount(Double fineAmount) { this.fineAmount = fineAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
