package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

//    public List<User> getAllUsers() {
//        return userRepository.findAll();
//    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

//    public User getUserById(UUID id) {
//        return userRepository.findById(id)
//                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
//    }
//
//    public User updateRole(UUID targetId, String newRole, String actingEmail) {
//        if (!ALLOWED_ROLES.contains(newRole)) {
//            throw new IllegalArgumentException("Invalid role: " + newRole);
//        }
//        User target = getUserById(targetId);
//        if (target.getEmail().equals(actingEmail)) {
//            throw new IllegalArgumentException("Admins cannot change their own role");
//        }
//        target.setRole(newRole);
//        return userRepository.save(target);
//    }
//
//    public void deleteUser(UUID targetId, String actingEmail) {
//        User target = getUserById(targetId);
//        if (target.getEmail().equals(actingEmail)) {
//            throw new IllegalArgumentException("Admins cannot delete their own account");
//        }
//        userRepository.delete(target);
//    }
//
//    public long countAll() {
//        return userRepository.count();
//    }
//
//    public long countByRole(String role) {
//        return userRepository.countByRole(role);
//    }
}
