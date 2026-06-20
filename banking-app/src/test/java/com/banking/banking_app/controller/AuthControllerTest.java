package com.banking.banking_app.controller;

import com.banking.banking_app.model.User;
import com.banking.banking_app.repository.UserRepository;
import com.banking.banking_app.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 *
 * @SpringBootTest      — loads the full Spring context (real beans)
 * @AutoConfigureMockMvc — gives us MockMvc to fire HTTP requests
 *
 * Unlike unit tests these hit the real service + repository + H2 DB.
 * They test the whole request → response flow end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        // Start each test with a clean slate
        userRepository.deleteAll();
    }

    // ── Register tests ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register → 201 with token")
    void register_success() throws Exception {
        var body = Map.of(
                "fullName", "Alice Smith",
                "email", "alice@bank.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("alice@bank.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Smith"));
    }

    @Test
    @DisplayName("POST /api/auth/register → 409 when email already exists")
    void register_duplicateEmail_returns409() throws Exception {
        // Pre-create a user with this email
        User existing = new User(
                "Existing User",
                "alice@bank.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(existing);

        // Try to register with the same email
        var body = Map.of(
                "fullName", "Another Alice",
                "email", "alice@bank.com",
                "password", "different123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict()); // 409
    }

    // ── Login tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login → 200 with token")
    void login_success() throws Exception {
        // Pre-create user
        User user = new User(
                "Alice Smith",
                "alice@bank.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(user);

        var body = Map.of(
                "email", "alice@bank.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("alice@bank.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login → 401 with wrong password")
    void login_wrongPassword_returns401() throws Exception {
        User user = new User(
                "Alice Smith",
                "alice@bank.com",
                passwordEncoder.encode("correctPassword")
        );
        userRepository.save(user);

        var body = Map.of(
                "email", "alice@bank.com",
                "password", "wrongPassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized()); // 401
    }

    @Test
    @DisplayName("POST /api/auth/login → 401 for non-existent user")
    void login_userNotFound_returns401() throws Exception {
        var body = Map.of(
                "email", "nobody@bank.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}