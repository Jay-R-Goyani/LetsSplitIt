package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Dto.ExpenseRequest;
import com.jay.LetsSplitIt.Dto.ExpenseResponse;
import com.jay.LetsSplitIt.Dto.ExpenseShare;
import com.jay.LetsSplitIt.Dto.SplitInput;
import com.jay.LetsSplitIt.Entities.Expense;
import com.jay.LetsSplitIt.Entities.Group;
import com.jay.LetsSplitIt.Entities.SharedExpense;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Dto.ExpenseCreatedEvent;
import com.jay.LetsSplitIt.Repository.ExpenseRepository;
import com.jay.LetsSplitIt.Repository.GroupRepository;
import com.jay.LetsSplitIt.Repository.ShareExpenseRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Services.Split.SplitStrategy;
import com.jay.LetsSplitIt.Services.Split.SplitStrategyRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ShareExpenseRepository shareExpenseRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SplitStrategyRegistry registry;
    private final BalanceService balanceService;
    private final ApplicationEventPublisher eventPublisher;

    ExpenseService(ExpenseRepository expenseRepository,
                   ShareExpenseRepository shareExpenseRepository,
                   UserRepository userRepository,
                   GroupRepository groupRepository,
                   SplitStrategyRegistry registry,
                   BalanceService balanceService,
                   ApplicationEventPublisher eventPublisher) {
        this.expenseRepository = expenseRepository;
        this.shareExpenseRepository = shareExpenseRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.registry = registry;
        this.balanceService = balanceService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ExpenseResponse createExpense(UserDetails userDetails, ExpenseRequest request) {
        return createExpense(userDetails, request, null);
    }

    @Transactional
    public ExpenseResponse createExpense(UserDetails userDetails, ExpenseRequest request, UUID groupId) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (request.category() == null) {
            throw new IllegalArgumentException("Category is required");
        }

        SplitStrategy strategy = registry.get(request.splitType());
        strategy.validate(request.amount(), request.participants());
        List<ExpenseShare> shares = strategy.split(request.amount(), request.participants());

        UUID paidBy = currentUserId(userDetails);

        if (groupId != null) {
            validateGroupParticipants(groupId, paidBy, request.participants());
        }

        Expense expense = new Expense();
        expense.setPaidBy(paidBy);
        expense.setAmount(request.amount());
        expense.setSplitType(request.splitType());
        expense.setGroupId(groupId);
        expense.setTitle(request.title().trim());
        expense.setCategory(request.category());
        expense.setDescription(request.description());

        Expense saved = expenseRepository.save(expense);

        List<SharedExpense> sharedRows = shares.stream()
                .filter(share -> !share.userId().equals(paidBy))
                .map(share -> new SharedExpense(null, saved, share.userId(), share.amountOwed()))
                .toList();
        shareExpenseRepository.saveAll(sharedRows);

        for (SharedExpense row : sharedRows) {
            balanceService.applyDebt(row.getUserId(), paidBy, row.getAmount(), groupId);
        }

        eventPublisher.publishEvent(new ExpenseCreatedEvent(
                saved.getId(),
                saved.getPaidBy(),
                saved.getTitle(),
                saved.getCategory(),
                saved.getDescription(),
                saved.getSplitType(),
                saved.getAmount(),
                shares
        ));

        return new ExpenseResponse(
                saved.getId(),
                saved.getPaidBy(),
                saved.getTitle(),
                saved.getCategory(),
                saved.getDescription(),
                saved.getAmount(),
                saved.getSplitType(),
                shares
        );
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getMyActivity(UserDetails userDetails, Pageable pageable) {
        UUID me = currentUserId(userDetails);
        return expenseRepository.findActivityForUser(me, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getActivityWithFriend(UserDetails userDetails, UUID friendId, Pageable pageable) {
        UUID me = currentUserId(userDetails);
        if (me.equals(friendId)) {
            throw new IllegalArgumentException("Cannot fetch activity with self");
        }
        return expenseRepository.findActivityWithFriend(me, friendId, pageable).map(this::toResponse);
    }

    private ExpenseResponse toResponse(Expense expense) {
        List<ExpenseShare> shares = new ArrayList<>();
        BigDecimal payerShare = expense.getAmount();
        for (SharedExpense row : expense.getShares()) {
            shares.add(new ExpenseShare(row.getUserId(), row.getAmount()));
            payerShare = payerShare.subtract(row.getAmount());
        }
        if (payerShare.signum() > 0) {
            shares.add(new ExpenseShare(expense.getPaidBy(), payerShare));
        }
        return new ExpenseResponse(
                expense.getId(),
                expense.getPaidBy(),
                expense.getTitle(),
                expense.getCategory(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getSplitType(),
                shares
        );
    }

    @Transactional
    public void deleteExpense(UserDetails userDetails, UUID expenseId) {
        UUID me = currentUserId(userDetails);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException("Expense not found: " + expenseId));

        if (!expense.getPaidBy().equals(me)) {
            throw new AccessDeniedException("Only the expense creator can delete this expense");
        }

        List<SharedExpense> shares = shareExpenseRepository.findByExpenseId(expenseId);
        UUID groupId = expense.getGroupId();
        UUID paidBy = expense.getPaidBy();

        for (SharedExpense row : shares) {
            balanceService.applyDebt(paidBy, row.getUserId(), row.getAmount(), groupId);
        }

        expenseRepository.delete(expense);
    }

    private void validateGroupParticipants(UUID groupId, UUID paidBy, List<SplitInput> participants) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));
        Set<UUID> members = new HashSet<>(group.getMembers());
        if (!members.contains(paidBy)) {
            throw new IllegalArgumentException("Payer is not a member of this group");
        }
        for (SplitInput p : participants) {
            if (!members.contains(p.userId())) {
                throw new IllegalArgumentException("Participant " + p.userId() + " is not a member of this group");
            }
        }
    }

    private UUID currentUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
