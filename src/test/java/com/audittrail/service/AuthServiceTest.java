package com.audittrail.service;

import com.audittrail.dto.AuthResponse;
import com.audittrail.dto.LoginRequest;
import com.audittrail.dto.RegisterRequest;
import com.audittrail.entity.User;
import com.audittrail.repository.UserRepository;
import com.audittrail.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$encrypted_password")
                .role(User.UserRole.DEVELOPER)
                .build();
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void register_shouldCreateNewUser_whenValidRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setRole("DEVELOPER");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(anyLong(), anyString(), anyString())).thenReturn("test_token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals("test_token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration for existing username")
    void register_shouldFail_whenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully with correct password")
    void login_shouldReturnToken_whenCredentialsValid() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyLong(), anyString(), anyString())).thenReturn("test_token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("successful"));
        assertEquals("test_token", response.getToken());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    @DisplayName("Should reject login with incorrect password")
    void login_shouldFail_whenPasswordIncorrect() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("Invalid"));
        assertNull(response.getToken());
    }

    @Test
    @DisplayName("Should reject login for non-existent user")
    void login_shouldFail_whenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Should retrieve user by ID")
    void getUserById_shouldReturnUser_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = authService.getUserById(1L);

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }
}
