package com.jay.LetsSplitIt;

import com.jay.LetsSplitIt.Dto.ExpenseRequest;
import com.jay.LetsSplitIt.Dto.ExpenseResponse;
import com.jay.LetsSplitIt.Dto.SplitInput;
import com.jay.LetsSplitIt.Entities.Expense;
import com.jay.LetsSplitIt.Entities.Expense.SplitType;
import com.jay.LetsSplitIt.Entities.PairBalance;
import com.jay.LetsSplitIt.Entities.SharedExpense;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.ExpenseRepository;
import com.jay.LetsSplitIt.Repository.PairBalanceRepository;
import com.jay.LetsSplitIt.Repository.ShareExpenseRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Services.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ExpenseServiceTests {

    @Autowired ExpenseService expenseService;
    @Autowired ExpenseRepository expenseRepository;
    @Autowired ShareExpenseRepository shareExpenseRepository;
    @Autowired PairBalanceRepository pairBalanceRepository;
    @Autowired UserRepository userRepository;

    private User payer;
    private User friend1;
    private User friend2;

    @BeforeEach
    void setUp() {
        payer = saveUser("payer@example.com", "Payer");
        friend1 = saveUser("f1@example.com", "Friend One");
        friend2 = saveUser("f2@example.com", "Friend Two");
    }

    private User saveUser(String email, String name) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("secret");
        return userRepository.saveAndFlush(u);
    }

    private UserDetails asUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private BigDecimal pairBalanceAmount(User debtor, User creditor) {
        return pairBalanceRepository.findByDebtorIdAndCreditorId(debtor.getId(), creditor.getId())
                .map(PairBalance::getAmount)
                .orElse(null);
    }

    // ---------- EQUAL split ----------

    @Test
    void createExpense_equalSplitWithTwoPeople_persistsExpenseSharesAndBalance() {
        ExpenseRequest request = new ExpenseRequest(
                bd("100.00"),
                SplitType.EQUAL,
                List.of(new SplitInput(payer.getId(), null), new SplitInput(friend1.getId(), null))
        );

        ExpenseResponse response = expenseService.createExpense(asUserDetails(payer), request);

        assertNotNull(response.id());
        assertEquals(payer.getId(), response.paidBy());
        assertEquals(SplitType.EQUAL, response.splitType());
        assertEquals(0, response.amount().compareTo(bd("100.00")));
        assertEquals(2, response.shares().size());

        Expense saved = expenseRepository.findById(response.id()).orElseThrow();
        assertEquals(payer.getId(), saved.getPaidBy());

        // Only the non-payer share row is persisted
        List<SharedExpense> sharedRows = shareExpenseRepository.findByExpenseId(response.id());
        assertEquals(1, sharedRows.size());
        assertEquals(friend1.getId(), sharedRows.get(0).getUserId());
        assertEquals(0, sharedRows.get(0).getAmount().compareTo(bd("50.00")));

        // friend1 now owes payer 50
        assertEquals(0, pairBalanceAmount(friend1, payer).compareTo(bd("50.00")));
        assertTrue(pairBalanceRepository.findByDebtorIdAndCreditorId(payer.getId(), friend1.getId()).isEmpty());
    }

    @Test
    void createExpense_equalSplitWithThreePeople_recordsDebtForEachFriend() {
        ExpenseRequest request = new ExpenseRequest(
                bd("90.00"),
                SplitType.EQUAL,
                List.of(new SplitInput(payer.getId(), null),
                        new SplitInput(friend1.getId(), null),
                        new SplitInput(friend2.getId(), null))
        );

        ExpenseResponse response = expenseService.createExpense(asUserDetails(payer), request);

        assertEquals(3, response.shares().size());
        List<SharedExpense> sharedRows = shareExpenseRepository.findByExpenseId(response.id());
        assertEquals(2, sharedRows.size());

        assertEquals(0, pairBalanceAmount(friend1, payer).compareTo(bd("30.00")));
        assertEquals(0, pairBalanceAmount(friend2, payer).compareTo(bd("30.00")));
    }

    @Test
    void createExpense_equalSplitWithRounding_lastParticipantAbsorbsRemainder() {
        ExpenseRequest request = new ExpenseRequest(
                bd("100.00"),
                SplitType.EQUAL,
                List.of(new SplitInput(payer.getId(), null),
                        new SplitInput(friend1.getId(), null),
                        new SplitInput(friend2.getId(), null))
        );

        ExpenseResponse response = expenseService.createExpense(asUserDetails(payer), request);

        // 100 / 3 = 33.33, 33.33, remainder 33.34 on the last participant
        BigDecimal f1Debt = pairBalanceAmount(friend1, payer);
        BigDecimal f2Debt = pairBalanceAmount(friend2, payer);
        assertEquals(0, f1Debt.compareTo(bd("33.33")));
        assertEquals(0, f2Debt.compareTo(bd("33.34")));
    }

    // ---------- EXACT split ----------

    @Test
    void createExpense_exactSplit_persistsProvidedAmounts() {
        ExpenseRequest request = new ExpenseRequest(
                bd("100.00"),
                SplitType.EXACT,
                List.of(new SplitInput(payer.getId(), bd("40.00")),
                        new SplitInput(friend1.getId(), bd("60.00")))
        );

        ExpenseResponse response = expenseService.createExpense(asUserDetails(payer), request);

        assertEquals(0, pairBalanceAmount(friend1, payer).compareTo(bd("60.00")));
        List<SharedExpense> sharedRows = shareExpenseRepository.findByExpenseId(response.id());
        assertEquals(1, sharedRows.size());
        assertEquals(0, sharedRows.get(0).getAmount().compareTo(bd("60.00")));
    }

    @Test
    void createExpense_exactSplitWithMismatchingSum_throwsAndSavesNothing() {
        ExpenseRequest request = new ExpenseRequest(
                bd("100.00"),
                SplitType.EXACT,
                List.of(new SplitInput(payer.getId(), bd("40.00")),
                        new SplitInput(friend1.getId(), bd("50.00")))
        );

        assertThrows(IllegalArgumentException.class,
                () -> expenseService.createExpense(asUserDetails(payer), request));

        assertEquals(0, expenseRepository.count());
        assertEquals(0, shareExpenseRepository.count());
        assertEquals(0, pairBalanceRepository.count());
    }

    // ---------- PERCENTAGE split ----------

    @Test
    void createExpense_percentageSplit_distributesByPercent() {
        ExpenseRequest request = new ExpenseRequest(
                bd("200.00"),
                SplitType.PERCENTAGE,
                List.of(new SplitInput(payer.getId(), bd("25")),
                        new SplitInput(friend1.getId(), bd("75")))
        );

        expenseService.createExpense(asUserDetails(payer), request);

        assertEquals(0, pairBalanceAmount(friend1, payer).compareTo(bd("150.00")));
    }

    @Test
    void createExpense_percentageSplitNotSummingTo100_throws() {
        ExpenseRequest request = new ExpenseRequest(
                bd("100.00"),
                SplitType.PERCENTAGE,
                List.of(new SplitInput(payer.getId(), bd("40")),
                        new SplitInput(friend1.getId(), bd("50")))
        );

        assertThrows(IllegalArgumentException.class,
                () -> expenseService.createExpense(asUserDetails(payer), request));
    }

    // ---------- SHARES split ----------

    @Test
    void createExpense_sharesSplit_distributesByShareRatio() {
        ExpenseRequest request = new ExpenseRequest(
                bd("120.00"),
                SplitType.SHARES,
                List.of(new SplitInput(payer.getId(), bd("1")),
                        new SplitInput(friend1.getId(), bd("2")),
                        new SplitInput(friend2.getId(), bd("3")))
        );

        expenseService.createExpense(asUserDetails(payer), request);

        // totalShares=6 → 20 / 40 / 60; payer's 20 is excluded
        assertEquals(0, pairBalanceAmount(friend1, payer).compareTo(bd("40.00")));
        assertEquals(0, pairBalanceAmount(friend2, payer).compareTo(bd("60.00")));
    }

    // ---------- Cross-cutting behaviors ----------

    @Test
    void createExpense_throwsWhenPayerMissingFromDb() {
        UserDetails ghost = org.springframework.security.core.userdetails.User
                .withUsername("ghost@example.com").password("x").authorities("ROLE_USER").build();

        ExpenseRequest request = new ExpenseRequest(
                bd("50.00"),
                SplitType.EQUAL,
                List.of(new SplitInput(friend1.getId(), null), new SplitInput(friend2.getId(), null))
        );

        assertThrows(UsernameNotFoundException.class,
                () -> expenseService.createExpense(ghost, request));
    }

    @Test
    void createExpense_whenPayerNotInParticipants_allSharesAreDebtsToPayer() {
        ExpenseRequest request = new ExpenseRequest(
                bd("80.00"),
                SplitType.EQUAL,
                List.of(new SplitInput(friend1.getId(), null), new SplitInput(friend2.getId(), null))
        );

        ExpenseResponse response = expenseService.createExpense(asUserDetails(payer), request);

        List<SharedExpense> sharedRows = shareExpenseRepository.findByExpenseId(response.id());
        assertEquals(2, sharedRows.size());
        assertEquals(0, pairBalanceAmount(friend1, payer).compareTo(bd("40.00")));
        assertEquals(0, pairBalanceAmount(friend2, payer).compareTo(bd("40.00")));
    }

    @Test
    void createExpense_subsequentExpenseOffsetsPreviousDebt() {
        expenseService.createExpense(asUserDetails(payer), new ExpenseRequest(
                bd("100.00"),
                SplitType.EQUAL,
                List.of(new SplitInput(payer.getId(), null), new SplitInput(friend1.getId(), null))
        ));
        // friend1 owes payer 50

        expenseService.createExpense(asUserDetails(friend1), new ExpenseRequest(
                bd("60.00"),
                SplitType.EQUAL,
                List.of(new SplitInput(payer.getId(), null), new SplitInput(friend1.getId(), null))
        ));
        // payer owes friend1 30 → offsets to friend1 still owing payer 20

        Optional<PairBalance> forward = pairBalanceRepository.findByDebtorIdAndCreditorId(friend1.getId(), payer.getId());
        Optional<PairBalance> reverse = pairBalanceRepository.findByDebtorIdAndCreditorId(payer.getId(), friend1.getId());
        assertTrue(reverse.isEmpty());
        assertEquals(0, forward.orElseThrow().getAmount().compareTo(bd("20.00")));
    }
}
