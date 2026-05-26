package smartcampus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartcampus.backend.model.BookLoan;

import java.util.List;
import java.util.Optional;

/**
 * Library Service - Book Loan Data Access Layer
 * Satisfies: R8 (SOAP operations), R3 (SOA)
 */
@Repository
public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {

    Optional<BookLoan> findByLoanReference(String loanReference);

    List<BookLoan> findByStudentId(String studentId);

    List<BookLoan> findByBookIsbn(String bookIsbn);

    List<BookLoan> findByStatus(BookLoan.LoanStatus status);

    long countByStatus(BookLoan.LoanStatus status);


    boolean existsByStudentIdAndBookIsbnAndStatus(String studentId, String bookIsbn, BookLoan.LoanStatus status);
}
