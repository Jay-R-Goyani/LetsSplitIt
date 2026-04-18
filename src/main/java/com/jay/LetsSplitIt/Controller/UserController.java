package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Services.FriendshipService;
import com.jay.LetsSplitIt.Services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final FriendshipService friendshipService;

    public UserController(UserService userService, FriendshipService friendshipService) {
        this.userService = userService;
        this.friendshipService = friendshipService;
    }

    @GetMapping("/me")
    public User getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        return userService.getUserByEmail(principal.getUsername());
    }

}
