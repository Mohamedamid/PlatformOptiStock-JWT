package com.optistockplatrorm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optistockplatrorm.dto.auth.LoginRequest;
import com.optistockplatrorm.dto.auth.RegisterRequest;
import com.optistockplatrorm.entity.Enums.Role;
import com.optistockplatrorm.repository.RefreshTokenRepository;
import com.optistockplatrorm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    public void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Ali", "Baba", "ali@test.com", "password123", Role.CLIENT
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    public void shouldLoginSuccessfully() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Hassan", "El", "hassan@test.com", "password123", Role.ADMIN
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest(
                "hassan@test.com", "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    public void shouldAccessProtectedResourceWithToken() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Secure", "User", "secure@test.com", "password123", Role.ADMIN
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest(
                "secure@test.com", "password123"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String accessToken = jsonNode.get("access_token").asText();

        mockMvc.perform(get("/api/v1/demo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }
}