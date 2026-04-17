package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Security.AuthRequest;
import com.jay.LetsSplitIt.Security.AuthResponse;
import com.jay.LetsSplitIt.Security.JwtService;
import com.jay.LetsSplitIt.Services.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody User user) {
        User saved = userService.createUser(user);
        return new AuthResponse(jwtService.generateToken(saved));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userService.getUserByEmail(request.getEmail());
        return new AuthResponse(jwtService.generateToken(user));
    }
}
