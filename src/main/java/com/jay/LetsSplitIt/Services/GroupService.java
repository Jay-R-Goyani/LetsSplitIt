package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Entities.Friendship;
import com.jay.LetsSplitIt.Entities.Group;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.FriendshipRepository;
import com.jay.LetsSplitIt.Repository.GroupRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupService {

    private GroupRepository groupRepository;
    private UserRepository userRepository;
    private FriendshipRepository friendshipRepository;

    public GroupService(GroupRepository groupRepository,
                        UserRepository userRepository,
                        FriendshipRepository friendshipRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    public List<Group> getMyGroups(String email) {
        List<Group> userGroups = new ArrayList<>();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for : "+email));
        for(UUID groupId : user.getMemberOfGroup()){
            groupRepository.findById(groupId).ifPresent(userGroups::add);
        }
        return userGroups;
    }

    public Group getGroupById(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(()->new RuntimeException("Group not found for : "+id));
    }

    @Transactional
    public Group createGroup(String creatorEmail, String name, List<UUID> selectedMembers) {
        if (selectedMembers == null || selectedMembers.size() < 2) {
            throw new IllegalArgumentException("A group must have at least 2 selected members besides the creator");
        }

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for : " + creatorEmail));

        Set<UUID> uniqueMembers = new HashSet<>(selectedMembers);
        if (uniqueMembers.contains(creator.getId())) {
            throw new IllegalArgumentException("Creator should not be listed in selectedMembers");
        }
        if (uniqueMembers.size() < 3) {
            throw new IllegalArgumentException("A group must have at least 3 unique selected members");
        }

        for (UUID memberId : uniqueMembers) {
            if (!userRepository.existsById(memberId)) {
                throw new NoSuchElementException("User not found: " + memberId);
            }
            if (!areFriends(creator.getId(), memberId)) {
                throw new IllegalArgumentException("User " + memberId + " is not a friend of the creator");
            }
        }

        List<UUID> members = new ArrayList<>();
        members.add(creator.getId());
        members.addAll(uniqueMembers);

        Group group = new Group();
        group.setName(name);
        group.setCreatedBy(creator.getId());
        group.setAdminId(creator.getId());
        group.setMembers(members);

        Group saved = groupRepository.save(group);

        attachGroupToUser(creator, saved.getId());
        for (UUID memberId : uniqueMembers) {
            User member = userRepository.findById(memberId).orElseThrow();
            attachGroupToUser(member, saved.getId());
        }

        return saved;
    }

    @Transactional
    public Group addMember(String requesterEmail, UUID groupId, UUID newMemberId) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for : " + requesterEmail));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));

        if (!group.getAdminId().equals(requester.getId())) {
            throw new AccessDeniedException("Only the group admin can add members");
        }
        if (requester.getId().equals(newMemberId)) {
            throw new IllegalArgumentException("Admin is already a member");
        }
        if (group.getMembers().contains(newMemberId)) {
            throw new IllegalStateException("User is already a member of this group");
        }
        User newMember = userRepository.findById(newMemberId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + newMemberId));
        if (!areFriends(requester.getId(), newMemberId)) {
            throw new IllegalArgumentException("User " + newMemberId + " is not a friend of the admin");
        }

        group.getMembers().add(newMemberId);
        Group saved = groupRepository.save(group);
        attachGroupToUser(newMember, groupId);
        return saved;
    }

    @Transactional
    public Group removeMember(String requesterEmail, UUID groupId, UUID memberId) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for : " + requesterEmail));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));

        if (!group.getAdminId().equals(requester.getId())) {
            throw new AccessDeniedException("Only the group admin can remove members");
        }
        if (memberId.equals(group.getAdminId())) {
            throw new IllegalArgumentException("Admin cannot be removed from the group");
        }
        if (!group.getMembers().remove(memberId)) {
            throw new NoSuchElementException("User is not a member of this group");
        }

        Group saved = groupRepository.save(group);
        userRepository.findById(memberId).ifPresent(user -> {
            if (user.getMemberOfGroup() != null) {
                user.getMemberOfGroup().remove(groupId);
                userRepository.save(user);
            }
        });
        return saved;
    }

    private boolean areFriends(UUID a, UUID b) {
        return friendshipRepository.existsBySentByAndSentToAndStatus(a, b, Friendship.Status.ACCEPTED)
                || friendshipRepository.existsBySentByAndSentToAndStatus(b, a, Friendship.Status.ACCEPTED);
    }

    private void attachGroupToUser(User user, UUID groupId) {
        List<UUID> memberOf = user.getMemberOfGroup();
        if (memberOf == null) {
            memberOf = new ArrayList<>();
        }
        if (!memberOf.contains(groupId)) {
            memberOf.add(groupId);
        }
        user.setMemberOfGroup(memberOf);
        userRepository.save(user);
    }
}
