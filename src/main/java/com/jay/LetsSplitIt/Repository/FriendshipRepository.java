package com.jay.LetsSplitIt.Repository;

import com.jay.LetsSplitIt.Entities.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    Optional<Friendship> findBySentByAndSentToAndStatus(
            UUID sentBy, UUID sentTo, Friendship.Status status);

    boolean existsBySentByAndSentToAndStatus(
            UUID sentBy, UUID sentTo, Friendship.Status status);

}
