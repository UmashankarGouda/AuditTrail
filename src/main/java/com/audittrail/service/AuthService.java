package com.audittrail.service;

import com.audittrail.dto.AuthResponse;
import com.audittrail.dto.LoginRequest;
import com.audittrail.dto.RegisterRequest;
import com.audittrail.entity.User;
import com.audittrail.repository.UserRepository;
import com.audittrail.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResponse.builder()
                    .message("Username already exists")
                    .build();
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.builder()
                    .message("Email already exists")
                    .build();
        }

        // Determine role
        User.UserRole role;
        try {
            role = User.UserRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            role = User.UserRole.DEVELOPER;
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        // Generate token
        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getUsername(), 
                                                     savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .role(savedUser.getRole().name())
                .message("User registered successfully")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty()) {
            return AuthResponse.builder()
                    .message("User not found")
                    .build();
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return AuthResponse.builder()
                    .message("Invalid password")
                    .build();
        }

        // Generate token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), 
                                                     user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("Login successful")
                .build();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
