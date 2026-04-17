package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public User getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        return userService.getUserByEmail(principal.getUsername());
    }
}
