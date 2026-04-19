package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Dto.BalanceSummary;
import com.jay.LetsSplitIt.Dto.FriendBalance;
import com.jay.LetsSplitIt.Services.BalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/balances")
public class BalanceController {

    private final BalanceService balanceService;

    BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/me")
    public ResponseEntity<BalanceSummary> summary(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(balanceService.getSummary(userDetails));
    }

    @GetMapping("/friends")
    public ResponseEntity<List<FriendBalance>> friends(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(balanceService.getFriendsBalances(userDetails));
    }

    @GetMapping("/friends/{friendId}")
    public ResponseEntity<FriendBalance> friendDetail(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable UUID friendId) {
        return ResponseEntity.ok(balanceService.getPairDetail(userDetails, friendId));
    }
}
