package com.example.demo;

import com.example.demo.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest
public class JwtTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testGenerateToken() {
        String token = jwtUtil.generateToken("user123", "ADMIN");
        assertNotNull(token);
    }

    @Test
    void testParseToken() {
        String token = jwtUtil.generateToken("user123", "ADMIN");
        Claims claims = jwtUtil.parseToken(token);
        assertEquals("user123", claims.getSubject());
    }
}