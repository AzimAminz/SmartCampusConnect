package smartcampus.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartcampus.booking.model.BookLoan;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {

    Optional<BookLoan> findByLoanReference(String loanReference);

    List<BookLoan> findByStudentId(String studentId);

    List<BookLoan> findByBookIsbn(String bookIsbn);

    List<BookLoan> findByStatus(BookLoan.LoanStatus status);

    long countByStatus(BookLoan.LoanStatus status);

    boolean existsByStudentIdAndBookIsbnAndStatus(String studentId, String bookIsbn, BookLoan.LoanStatus status);
}
