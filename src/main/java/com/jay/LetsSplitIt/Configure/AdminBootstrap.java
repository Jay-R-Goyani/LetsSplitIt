package com.jay.LetsSplitIt.Configure;

import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String adminEmail;
    private final String adminName;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.name}") String adminName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminEmail = adminEmail;
        this.adminName = adminName;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<User> admins = userRepository.findByRole(ADMIN_ROLE);

        if (!admins.isEmpty()) {
            log.info("==========================================================");
            log.info(" Admin already exists ({} total):", admins.size());
            admins.forEach(a -> log.info("   - {}", a.getEmail()));
            admins.forEach(a -> log.info("   - 12345jay"));
            log.info("==========================================================");
            return;
        }

        String rawPassword = "12345jay";
        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(ADMIN_ROLE);
        User saved = userRepository.save(admin);

        String token = jwtService.generateToken(saved);

        log.warn("==========================================================");
        log.warn(" NO ADMIN FOUND — BOOTSTRAP ADMIN CREATED");
        log.warn(" Email:    {}", adminEmail);
        log.warn(" Password: {}   (CHANGE AFTER FIRST LOGIN)", rawPassword);
        log.warn(" Token:    {}", token);
        log.warn("==========================================================");
    }
}
