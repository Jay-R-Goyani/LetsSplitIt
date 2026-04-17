package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Services.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users/list")
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }
}
