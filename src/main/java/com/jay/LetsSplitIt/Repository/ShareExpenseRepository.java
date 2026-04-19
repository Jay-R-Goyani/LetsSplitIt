package com.jay.LetsSplitIt.Repository;

import com.jay.LetsSplitIt.Entities.SharedExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShareExpenseRepository extends JpaRepository<SharedExpense, UUID> {
    List<SharedExpense> findByExpenseId(UUID expenseId);
    List<SharedExpense> findByUserId(UUID userId);
}
