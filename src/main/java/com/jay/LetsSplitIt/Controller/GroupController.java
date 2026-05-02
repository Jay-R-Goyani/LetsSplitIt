package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Dto.ExpenseRequest;
import com.jay.LetsSplitIt.Dto.ExpenseResponse;
import com.jay.LetsSplitIt.Entities.Expense;
import com.jay.LetsSplitIt.Entities.Group;
import com.jay.LetsSplitIt.Entities.PairBalance;
import com.jay.LetsSplitIt.Repository.ExpenseRepository;
import com.jay.LetsSplitIt.Services.BalanceService;
import com.jay.LetsSplitIt.Services.ExpenseService;
import com.jay.LetsSplitIt.Services.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class GroupController {

    private final GroupService groupService;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final BalanceService balanceService;

    public GroupController(GroupService groupService,
                           ExpenseService expenseService,
                           ExpenseRepository expenseRepository,
                           BalanceService balanceService) {
        this.groupService = groupService;
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
        this.balanceService = balanceService;
    }

    @GetMapping("/groups")
    public List<Group> getMyGroups(@AuthenticationPrincipal UserDetails principal) {
        return groupService.getMyGroups(principal.getUsername());
    }

    @GetMapping("/groups/{id}")
    public Group getGroupById(@PathVariable UUID id) {
        return groupService.getGroupById(id);
    }

    @PostMapping("/groups")
    public ResponseEntity<Group> createGroup(@AuthenticationPrincipal UserDetails principal,
                                             @RequestBody CreateGroupRequest request) {
        Group created = groupService.createGroup(
                principal.getUsername(), request.name(), request.selectedMembers());
        return ResponseEntity.ok(created);
    }

    @PostMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Group> addMember(@AuthenticationPrincipal UserDetails principal,
                                           @PathVariable UUID groupId,
                                           @PathVariable UUID userId) {
        return ResponseEntity.ok(groupService.addMember(principal.getUsername(), groupId, userId));
    }

    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Group> removeMember(@AuthenticationPrincipal UserDetails principal,
                                              @PathVariable UUID groupId,
                                              @PathVariable UUID userId) {
        return ResponseEntity.ok(groupService.removeMember(principal.getUsername(), groupId, userId));
    }

    @PostMapping("/groups/{groupId}/leave")
    public ResponseEntity<Group> leaveGroup(@AuthenticationPrincipal UserDetails principal,
                                            @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.leaveGroup(principal.getUsername(), groupId));
    }

    @PostMapping("/groups/{groupId}/expenses")
    public ResponseEntity<ExpenseResponse> addGroupExpense(@AuthenticationPrincipal UserDetails principal,
                                                           @PathVariable UUID groupId,
                                                           @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.createExpense(principal, request, groupId));
    }

    @GetMapping("/groups/{groupId}/expenses")
    public List<Expense> listGroupExpenses(@PathVariable UUID groupId) {
        return expenseRepository.findByGroupId(groupId);
    }

    @PostMapping("/groups/{groupId}/simplify-debts")
    public ResponseEntity<List<PairBalance>> simplifyGroupDebts(@AuthenticationPrincipal UserDetails principal,
                                                                @PathVariable UUID groupId) {
        return ResponseEntity.ok(balanceService.simplifyGroupDebts(principal, groupId));
    }

    public record CreateGroupRequest(String name,List<UUID> selectedMembers) {}
}
