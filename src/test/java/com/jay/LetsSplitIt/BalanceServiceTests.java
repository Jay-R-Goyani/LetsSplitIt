package com.jay.LetsSplitIt;

import com.jay.LetsSplitIt.Dto.BalanceSummary;
import com.jay.LetsSplitIt.Dto.FriendBalance;
import com.jay.LetsSplitIt.Entities.PairBalance;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.PairBalanceRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Services.BalanceService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class BalanceServiceTests {

    @Autowired BalanceService balanceService;
    @Autowired UserRepository userRepository;
    @Autowired PairBalanceRepository pairBalanceRepository;

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        alice = saveUser("alice@example.com", "Alice");
        bob = saveUser("bob@example.com", "Bob");
        carol = saveUser("carol@example.com", "Carol");
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

    private Optional<PairBalance> findBalance(User debtor, User creditor) {
        return pairBalanceRepository.findByDebtorIdAndCreditorIdAndGroupIdIsNull(debtor.getId(), creditor.getId());
    }

    private PairBalance insertBalance(User debtor, User creditor, String amount) {
        return pairBalanceRepository.saveAndFlush(
                new PairBalance(null, debtor.getId(), creditor.getId(), null, new BigDecimal(amount), null));
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    // ---------- applyDebt ----------

    @Test
    void applyDebt_createsNewBalanceWhenNoneExists() {
        balanceService.applyDebt(alice.getId(), bob.getId(), bd("40.00"));

        PairBalance pb = findBalance(alice, bob).orElseThrow();
        assertEquals(0, pb.getAmount().compareTo(bd("40.00")));
        assertTrue(findBalance(bob, alice).isEmpty());
    }

    @Test
    void applyDebt_isNoOpForSelfDebt() {
        long before = pairBalanceRepository.count();
        balanceService.applyDebt(alice.getId(), alice.getId(), bd("10.00"));
        assertEquals(before, pairBalanceRepository.count());
    }

    @Test
    void applyDebt_isNoOpForNullAmount() {
        long before = pairBalanceRepository.count();
        balanceService.applyDebt(alice.getId(), bob.getId(), null);
        assertEquals(before, pairBalanceRepository.count());
    }

    @Test
    void applyDebt_isNoOpForZeroAmount() {
        long before = pairBalanceRepository.count();
        balanceService.applyDebt(alice.getId(), bob.getId(), BigDecimal.ZERO);
        assertEquals(before, pairBalanceRepository.count());
    }

    @Test
    void applyDebt_isNoOpForNegativeAmount() {
        long before = pairBalanceRepository.count();
        balanceService.applyDebt(alice.getId(), bob.getId(), bd("-5.00"));
        assertEquals(before, pairBalanceRepository.count());
    }

    @Test
    void applyDebt_accumulatesOntoExistingSameDirectionBalance() {
        balanceService.applyDebt(alice.getId(), bob.getId(), bd("20.00"));
        balanceService.applyDebt(alice.getId(), bob.getId(), bd("15.00"));

        PairBalance pb = findBalance(alice, bob).orElseThrow();
        assertEquals(0, pb.getAmount().compareTo(bd("35.00")));
    }

    @Test
    void applyDebt_partiallyOffsetsOppositeBalance() {
        balanceService.applyDebt(bob.getId(), alice.getId(), bd("100.00"));
        balanceService.applyDebt(alice.getId(), bob.getId(), bd("40.00"));

        PairBalance remaining = findBalance(bob, alice).orElseThrow();
        assertEquals(0, remaining.getAmount().compareTo(bd("60.00")));
        assertTrue(findBalance(alice, bob).isEmpty());
    }

    @Test
    void applyDebt_exactOffsetDeletesBalance() {
        balanceService.applyDebt(bob.getId(), alice.getId(), bd("50.00"));
        balanceService.applyDebt(alice.getId(), bob.getId(), bd("50.00"));

        assertTrue(findBalance(bob, alice).isEmpty());
        assertTrue(findBalance(alice, bob).isEmpty());
    }

    @Test
    void applyDebt_overpayCreatesReversedBalance() {
        balanceService.applyDebt(bob.getId(), alice.getId(), bd("30.00"));
        balanceService.applyDebt(alice.getId(), bob.getId(), bd("80.00"));

        assertTrue(findBalance(bob, alice).isEmpty());
        PairBalance reversed = findBalance(alice, bob).orElseThrow();
        assertEquals(0, reversed.getAmount().compareTo(bd("50.00")));
    }

    // ---------- getSummary ----------

    @Test
    void getSummary_aggregatesDebtsAndCreditsAcrossFriends() {
        insertBalance(alice, bob, "20.00");
        insertBalance(alice, carol, "30.00");
        insertBalance(bob, alice, "15.00");
        insertBalance(carol, alice, "10.00");

        BalanceSummary summary = balanceService.getSummary(asUserDetails(alice));

        assertEquals(0, summary.youOwe().compareTo(bd("50.00")));
        assertEquals(0, summary.youAreOwed().compareTo(bd("25.00")));
        assertEquals(0, summary.net().compareTo(bd("-25.00")));
    }

    @Test
    void getSummary_returnsZerosWhenUserHasNoBalances() {
        BalanceSummary summary = balanceService.getSummary(asUserDetails(alice));

        assertEquals(0, summary.youOwe().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.youAreOwed().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.net().compareTo(BigDecimal.ZERO));
    }

    @Test
    void getSummary_throwsWhenCurrentUserMissing() {
        UserDetails ghost = org.springframework.security.core.userdetails.User
                .withUsername("ghost@example.com").password("x").authorities("ROLE_USER").build();

        assertThrows(UsernameNotFoundException.class, () -> balanceService.getSummary(ghost));
    }

    // ---------- getFriendsBalances ----------

    @Test
    void getFriendsBalances_returnsEntriesInBothDirections() {
        insertBalance(alice, bob, "20.00");
        insertBalance(carol, alice, "10.00");

        List<FriendBalance> result = balanceService.getFriendsBalances(asUserDetails(alice));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(fb ->
                fb.friendId().equals(bob.getId())
                        && fb.direction() == FriendBalance.Direction.I_OWE
                        && fb.amount().compareTo(bd("20.00")) == 0));
        assertTrue(result.stream().anyMatch(fb ->
                fb.friendId().equals(carol.getId())
                        && fb.direction() == FriendBalance.Direction.OWES_ME
                        && fb.amount().compareTo(bd("10.00")) == 0));
    }

    @Test
    void getFriendsBalances_emptyWhenNoBalances() {
        List<FriendBalance> result = balanceService.getFriendsBalances(asUserDetails(alice));
        assertTrue(result.isEmpty());
    }

    // ---------- getPairDetail ----------

    @Test
    void getPairDetail_returnsIOweWhenUserOwesFriend() {
        insertBalance(alice, bob, "25.00");

        FriendBalance fb = balanceService.getPairDetail(asUserDetails(alice), bob.getId());

        assertEquals(bob.getId(), fb.friendId());
        assertEquals(FriendBalance.Direction.I_OWE, fb.direction());
        assertEquals(0, fb.amount().compareTo(bd("25.00")));
    }

    @Test
    void getPairDetail_returnsOwesMeWhenFriendOwesUser() {
        insertBalance(bob, alice, "15.00");

        FriendBalance fb = balanceService.getPairDetail(asUserDetails(alice), bob.getId());

        assertEquals(bob.getId(), fb.friendId());
        assertEquals(FriendBalance.Direction.OWES_ME, fb.direction());
        assertEquals(0, fb.amount().compareTo(bd("15.00")));
    }

    @Test
    void getPairDetail_returnsSettledWhenNoBalance() {
        FriendBalance fb = balanceService.getPairDetail(asUserDetails(alice), bob.getId());

        assertEquals(bob.getId(), fb.friendId());
        assertEquals(FriendBalance.Direction.SETTLED, fb.direction());
        assertEquals(0, fb.amount().compareTo(BigDecimal.ZERO));
    }
}
