package com.wallets;


import com.wallets.security.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.userdetails.UserDetails;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
 class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret",
                "your-super-secret-key-must-be-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 86400000L);
    }

    @Test
    void generateToken_Success() {
        String token = jwtUtils.generateToken("bukola");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_Success() {
        String token = jwtUtils.generateToken("bukola");
        String username = jwtUtils.extractUsername(token);
        assertEquals("bukola", username);
    }

    @Test
    void isTokenValid_Success() {
        String token = jwtUtils.generateToken("bukola");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .builder()
                .username("bukola")
                .password("password")
                .roles("USER")
                .build();

        assertTrue(jwtUtils.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_WrongUsername() {
        String token = jwtUtils.generateToken("bukola");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .builder()
                .username("wronguser")
                .password("password")
                .roles("USER")
                .build();

        assertFalse(jwtUtils.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_ExpiredToken() {
        // Set expiration to -1000 (already expired)
        ReflectionTestUtils.setField(jwtUtils, "expiration", -1000L);
        String token = jwtUtils.generateToken("bukola");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .builder()
                .username("bukola")
                .password("password")
                .roles("USER")
                .build();

        // ✅ Expect ExpiredJwtException to be thrown
        assertThrows(ExpiredJwtException.class, () -> jwtUtils.isTokenValid(token, userDetails));
    }
}
