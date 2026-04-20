package com.jay.LetsSplitIt;

import com.jay.LetsSplitIt.Entities.Friendship;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.FriendshipRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Services.FriendshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipTests {

    @Mock UserRepository userRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock UserDetails userDetails;

    @InjectMocks FriendshipService friendshipService;

    private UUID userId;
    private UUID friendId;
    private final String userEmail = "me@example.com";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        friendId = UUID.randomUUID();
    }

    private void stubCurrentUser() {
        User me = new User();
        me.setId(userId);
        me.setEmail(userEmail);
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(me));
    }

    private Friendship pendingFromFriend() {
        Friendship f = new Friendship();
        f.setSentBy(friendId);
        f.setSentTo(userId);
        f.setStatus(Friendship.Status.PENDING);
        return f;
    }

    // ---------- sendFriendRequest ----------

    @Test
    void sendFriendRequest_savesPendingFriendship() {
        stubCurrentUser();
        when(userRepository.existsById(friendId)).thenReturn(true);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(any(), any(), any()))
                .thenReturn(false);

        friendshipService.sendFriendRequest(userDetails, friendId);

        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(captor.capture());
        Friendship saved = captor.getValue();
        assertEquals(userId, saved.getSentBy());
        assertEquals(friendId, saved.getSentTo());
        assertEquals(Friendship.Status.PENDING, saved.getStatus());
    }

    @Test
    void sendFriendRequest_toSelf_throws() {
        stubCurrentUser();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> friendshipService.sendFriendRequest(userDetails, userId));
        assertTrue(ex.getMessage().contains("yourself"));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_targetUserNotFound_throws() {
        stubCurrentUser();
        when(userRepository.existsById(friendId)).thenReturn(false);

        assertThrows(NoSuchElementException.class,
                () -> friendshipService.sendFriendRequest(userDetails, friendId));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_whenBlockedByCurrentUser_throws() {
        stubCurrentUser();
        when(userRepository.existsById(friendId)).thenReturn(true);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.BLOCKED)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> friendshipService.sendFriendRequest(userDetails, friendId));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_whenBlockedByFriend_throws() {
        stubCurrentUser();
        when(userRepository.existsById(friendId)).thenReturn(true);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.BLOCKED)).thenReturn(false);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.BLOCKED)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> friendshipService.sendFriendRequest(userDetails, friendId));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_whenPendingAlreadyExists_throws() {
        stubCurrentUser();
        when(userRepository.existsById(friendId)).thenReturn(true);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(
                any(), any(), eq(Friendship.Status.BLOCKED))).thenReturn(false);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.PENDING)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> friendshipService.sendFriendRequest(userDetails, friendId));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_whenAlreadyFriends_throws() {
        stubCurrentUser();
        when(userRepository.existsById(friendId)).thenReturn(true);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(
                any(), any(), eq(Friendship.Status.BLOCKED))).thenReturn(false);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(
                any(), any(), eq(Friendship.Status.PENDING))).thenReturn(false);
        when(friendshipRepository.existsBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.ACCEPTED)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> friendshipService.sendFriendRequest(userDetails, friendId));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_currentUserMissingFromDb_throws() {
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> friendshipService.sendFriendRequest(userDetails, friendId));
    }

    // ---------- acceptFriendRequest ----------

    @Test
    void acceptFriendRequest_marksAcceptedAndSaves() {
        stubCurrentUser();
        Friendship pending = pendingFromFriend();
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.PENDING)).thenReturn(Optional.of(pending));

        friendshipService.acceptFriendRequest(userDetails, friendId);

        assertEquals(Friendship.Status.ACCEPTED, pending.getStatus());
        verify(friendshipRepository).save(pending);
    }

    @Test
    void acceptFriendRequest_noPendingRequest_throws() {
        stubCurrentUser();
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.PENDING)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> friendshipService.acceptFriendRequest(userDetails, friendId));
        verify(friendshipRepository, never()).save(any());
    }

    // ---------- rejectFriendRequest ----------

    @Test
    void rejectFriendRequest_deletesPending() {
        stubCurrentUser();
        Friendship pending = pendingFromFriend();
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.PENDING)).thenReturn(Optional.of(pending));

        friendshipService.rejectFriendRequest(userDetails, friendId);

        verify(friendshipRepository).delete(pending);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void rejectFriendRequest_noPendingRequest_throws() {
        stubCurrentUser();
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.PENDING)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> friendshipService.rejectFriendRequest(userDetails, friendId));
        verify(friendshipRepository, never()).delete(any());
    }

    // ---------- blockFriend ----------

    @Test
    void blockFriend_currentUserInitiatedFriendship_setsBlocked() {
        stubCurrentUser();
        Friendship accepted = new Friendship();
        accepted.setSentBy(userId);
        accepted.setSentTo(friendId);
        accepted.setStatus(Friendship.Status.ACCEPTED);
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.ACCEPTED)).thenReturn(Optional.of(accepted));

        friendshipService.blockFriend(userDetails, friendId);

        assertEquals(Friendship.Status.BLOCKED, accepted.getStatus());
        verify(friendshipRepository).save(accepted);
    }

    @Test
    void blockFriend_friendInitiatedFriendship_setsBlocked() {
        stubCurrentUser();
        Friendship accepted = new Friendship();
        accepted.setSentBy(friendId);
        accepted.setSentTo(userId);
        accepted.setStatus(Friendship.Status.ACCEPTED);
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.ACCEPTED)).thenReturn(Optional.empty());
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.ACCEPTED)).thenReturn(Optional.of(accepted));

        friendshipService.blockFriend(userDetails, friendId);

        assertEquals(Friendship.Status.BLOCKED, accepted.getStatus());
        verify(friendshipRepository).save(accepted);
    }

    @Test
    void blockFriend_whenNotFriends_throws() {
        stubCurrentUser();
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                userId, friendId, Friendship.Status.ACCEPTED)).thenReturn(Optional.empty());
        when(friendshipRepository.findBySentByAndSentToAndStatus(
                friendId, userId, Friendship.Status.ACCEPTED)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> friendshipService.blockFriend(userDetails, friendId));
        verify(friendshipRepository, never()).save(any());
    }
}
