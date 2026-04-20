package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Entities.Friendship;
import com.jay.LetsSplitIt.Repository.FriendshipRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class FriendshipService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    FriendshipService(UserRepository userRepository, FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    public void sendFriendRequest(UserDetails userDetails, UUID friendId) {
        UUID userId = currentUserId(userDetails);

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        if (!userRepository.existsById(friendId)) {
            throw new NoSuchElementException("Target user not found: " + friendId);
        }
        if (friendshipRepository.existsBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.BLOCKED)
            || friendshipRepository.existsBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.BLOCKED)) {
            throw new IllegalStateException("Cannot send friend request: blocked");
        }
        if (friendshipRepository.existsBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.PENDING)
            || friendshipRepository.existsBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.PENDING)) {
            throw new IllegalStateException("A pending request already exists");
        }
        if (friendshipRepository.existsBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.ACCEPTED)
            || friendshipRepository.existsBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.ACCEPTED)) {
            throw new IllegalStateException("Already friends");
        }

        Friendship friendship = new Friendship();
        friendship.setSentBy(userId);
        friendship.setSentTo(friendId);
        friendship.setStatus(Friendship.Status.PENDING);

        friendshipRepository.save(friendship);
    }

    public void acceptFriendRequest(UserDetails userDetails, UUID friendId) {
        Friendship friendship = findPendingRequest(friendId, currentUserId(userDetails));
        friendship.setStatus(Friendship.Status.ACCEPTED);
        friendshipRepository.save(friendship);
    }

    public void rejectFriendRequest(UserDetails userDetails, UUID friendId) {
        Friendship friendship = findPendingRequest(friendId, currentUserId(userDetails));
        friendshipRepository.delete(friendship);
    }

    public void blockFriend(UserDetails userDetails, UUID friendId) {
        UUID userId = currentUserId(userDetails);
        Friendship friendship = findAcceptedFriendship(userId, friendId);
        friendship.setStatus(Friendship.Status.BLOCKED);
        friendshipRepository.save(friendship);
    }

    private UUID currentUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .map((user)->user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private Friendship findPendingRequest(UUID sentBy, UUID sentTo) {
        return friendshipRepository
                .findBySentByAndSentToAndStatus(sentBy, sentTo, Friendship.Status.PENDING)
                .orElseThrow(() -> new NoSuchElementException("Friend request not found"));
    }

    private Friendship findAcceptedFriendship(UUID a, UUID b) {
        return friendshipRepository
                .findBySentByAndSentToAndStatus(a, b, Friendship.Status.ACCEPTED)
                .or(() -> friendshipRepository
                        .findBySentByAndSentToAndStatus(b, a, Friendship.Status.ACCEPTED))
                .orElseThrow(() -> new NoSuchElementException("Friendship not found"));
    }
}
