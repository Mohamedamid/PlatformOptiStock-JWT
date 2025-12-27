package com.optistockplatrorm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optistockplatrorm.dto.auth.AuthResponse;
import com.optistockplatrorm.dto.auth.LoginRequest;
import com.optistockplatrorm.dto.auth.RegisterRequest;
import com.optistockplatrorm.entity.Enums.Role;
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
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Ali")
                .lastname("Baba")
                .email("ali@test.com")
                .password("password123")
                .role(Role.CLIENT)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {

        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname("Hassan")
                .lastname("El")
                .email("hassan@test.com")
                .password("password123")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("hassan@test.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    void shouldAccessAdminResourceWithToken() throws Exception {

        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname("Admin")
                .lastname("User")
                .email("admin@test.com")
                .password("password123")
                .role(Role.ADMIN)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(content, AuthResponse.class);
        String token = response.getAccessToken();

        mockMvc.perform(get("/api/admin/demo")
                        .header("Authorization", "Bearer " + token)) // Hna kan-nssifto Token
                .andExpect(status().isOk());
    }

    @Test
    void shouldBlockAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/demo"))
                .andExpect(status().isForbidden());
    }
}