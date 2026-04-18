package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Services.FriendshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;

    FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/requests/{friendId}")
    public ResponseEntity<Void> sendRequest(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable UUID friendId) {
        friendshipService.sendFriendRequest(userDetails, friendId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{friendId}/accept")
    public ResponseEntity<Void> acceptRequest(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable UUID friendId) {
        friendshipService.acceptFriendRequest(userDetails, friendId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{friendId}/reject")
    public ResponseEntity<Void> rejectRequest(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable UUID friendId) {
        friendshipService.rejectFriendRequest(userDetails, friendId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{friendId}/block")
    public ResponseEntity<Void> blockFriend(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable UUID friendId) {
        friendshipService.blockFriend(userDetails, friendId);
        return ResponseEntity.ok().build();
    }
}
