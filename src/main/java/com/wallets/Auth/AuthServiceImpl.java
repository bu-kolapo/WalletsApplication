package com.wallets.Auth;

import com.wallets.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Generate userId
        String userId = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Save user
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .userId(userId)
                .build();

        userRepository.save(user);
        log.info("User registered: {}", request.getUsername());

        // Generate token
        String token = jwtUtils.generateToken(request.getUsername());

        return AuthResponse.builder()
                .token(token)
                .userId(userId)
                .username(request.getUsername())
                .message("Registration successful")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Get user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate token
        String token = jwtUtils.generateToken(request.getUsername());
        log.info("User logged in: {}", request.getUsername());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .username(user.getUsername())
                .message("Login successful")
                .build();
    }
}
