package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Dto.UserStats;
import com.jay.LetsSplitIt.Entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdminService {

    private final UserService userService;

    public AdminService(UserService userService) {
        this.userService = userService;
    }

    public Page<User> listUsers(Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

//    public User getUser(UUID id) {
//        return userService.getUserById(id);
//    }
//
//    public User updateUserRole(UUID targetId, String newRole, String actingEmail) {
//        return userService.updateRole(targetId, newRole, actingEmail);
//    }
//
//    public void deleteUser(UUID targetId, String actingEmail) {
//        userService.deleteUser(targetId, actingEmail);
//    }

//    public UserStats getStats() {
//        long total = userService.countAll();
//        long admins = userService.countByRole("ADMIN");
//        long regular = userService.countByRole("USER");
//        return new UserStats(total, admins, regular, Instant.now());
//    }
}
