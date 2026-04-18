package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Services.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Page<User> listUsers(Pageable pageable) {
        return adminService.listUsers(pageable);
    }

//    @GetMapping("/users/{id}")
//    public User getUser(@PathVariable UUID id) {
//        return adminService.getUser(id);
//    }
//
//    @PatchMapping("/users/{id}/role")
//    public User updateUserRole(
//            @PathVariable UUID id,
//            @RequestBody UpdateRoleRequest request,
//            @AuthenticationPrincipal UserDetails principal) {
//        return adminService.updateUserRole(id, request.getRole(), principal.getUsername());
//    }
//
//    @DeleteMapping("/users/{id}")
//    public ResponseEntity<Void> deleteUser(
//            @PathVariable UUID id,
//            @AuthenticationPrincipal UserDetails principal) {
//        adminService.deleteUser(id, principal.getUsername());
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping("/stats")
//    public UserStats getStats() {
//        return adminService.getStats();
//    }
}
