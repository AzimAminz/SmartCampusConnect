package smartcampus.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Library / Booking Service - Master Book Catalog Entity
 * Maps to table: books
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 150)
    private String author;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.AVAILABLE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum BookStatus {
        AVAILABLE,
        BORROWED
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Constructors ----
    public Book() {}

    public Book(String isbn, String title, String author, String category) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.status = BookStatus.AVAILABLE;
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
