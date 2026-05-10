package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Dto.ExpenseRequest;
import com.jay.LetsSplitIt.Dto.ExpenseResponse;
import com.jay.LetsSplitIt.Entities.Expense;
import com.jay.LetsSplitIt.Services.ExpenseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.createExpense(userDetails, request));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable UUID expenseId) {
        expenseService.deleteExpense(userDetails, expenseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<Expense.Category[]> categories() {
        return ResponseEntity.ok(Expense.Category.values());
    }

    @GetMapping("/me")
    public ResponseEntity<Page<ExpenseResponse>> myActivity(@AuthenticationPrincipal UserDetails userDetails,
                                                            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getMyActivity(userDetails, pageable));
    }

    @GetMapping("/with/{friendId}")
    public ResponseEntity<Page<ExpenseResponse>> activityWithFriend(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @PathVariable UUID friendId,
                                                                    @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getActivityWithFriend(userDetails, friendId, pageable));
    }
}
