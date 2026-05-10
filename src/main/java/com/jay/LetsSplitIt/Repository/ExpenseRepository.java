package com.jay.LetsSplitIt.Repository;

import com.jay.LetsSplitIt.Entities.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByPaidBy(UUID paidBy);

    List<Expense> findByGroupId(UUID groupId);

    @Query(value = """
            SELECT DISTINCT e FROM Expense e
            LEFT JOIN e.shares s
            WHERE e.paidBy = :userId OR s.userId = :userId
            ORDER BY e.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e) FROM Expense e
            LEFT JOIN e.shares s
            WHERE e.paidBy = :userId OR s.userId = :userId
            """)
    Page<Expense> findActivityForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT e FROM Expense e
            LEFT JOIN e.shares s
            WHERE (e.paidBy = :me AND s.userId = :friend)
               OR (e.paidBy = :friend AND s.userId = :me)
            ORDER BY e.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e) FROM Expense e
            LEFT JOIN e.shares s
            WHERE (e.paidBy = :me AND s.userId = :friend)
               OR (e.paidBy = :friend AND s.userId = :me)
            """)
    Page<Expense> findActivityWithFriend(@Param("me") UUID me,
                                         @Param("friend") UUID friend,
                                         Pageable pageable);
}
