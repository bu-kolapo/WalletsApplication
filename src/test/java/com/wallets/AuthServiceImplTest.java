package com.wallets;

import com.wallets.Auth.*;
import com.wallets.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;   // 👈 must be this
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;


@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("bukola")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .username("bukola")
                .password("password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("bukola")
                .password("encodedPassword")
                .userId("CUST-A1B2C3D4")
                .build();
    }

    // --- Register Tests ---
    @Test
    void register_Success() {
        when(userRepository.existsByUsername("bukola")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtils.generateToken("bukola")).thenReturn("mockToken");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        assertEquals("bukola", response.getUsername());
        assertEquals("Registration successful", response.getMessage());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_UsernameAlreadyExists() {
        when(userRepository.existsByUsername("bukola")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PasswordIsEncoded() {
        when(userRepository.existsByUsername("bukola")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtUtils.generateToken("bukola")).thenReturn("mockToken");

        authService.register(registerRequest);

        // Verify raw password was never saved
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("encodedPassword") // encoded not raw
        ));
    }

    // --- Login Tests ---
    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("bukola")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken("bukola")).thenReturn("mockToken");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        assertEquals("bukola", response.getUsername());
        assertEquals("CUST-A1B2C3D4", response.getUserId());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void login_UserNotFound() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("bukola")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_InvalidCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(userRepository, never()).findByUsername(any());
    }
}