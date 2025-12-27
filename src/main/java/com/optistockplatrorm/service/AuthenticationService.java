package com.optistockplatrorm.service;

import com.optistockplatrorm.dto.auth.AuthResponse;
import com.optistockplatrorm.dto.auth.LoginRequest;
import com.optistockplatrorm.dto.auth.RegisterRequest;
import com.optistockplatrorm.entity.RefreshToken;
import com.optistockplatrorm.entity.User;
import com.optistockplatrorm.repository.RefreshTokenRepository;
import com.optistockplatrorm.repository.UserRepository;
import com.optistockplatrorm.Security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final RefreshTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        var user = User.builder()
                .firstName(request.getFirstname())
                .lastName(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();
        var savedUser = repository.save(user);

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPassword())
                .roles(savedUser.getRole().name())
                .build();

        var jwtToken = jwtService.generateToken(userDetails);
        var refreshToken = createRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = repository.findByEmail(request.getEmail()).orElseThrow();

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        var jwtToken = jwtService.generateToken(userDetails);
        var refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(604800000))
                .revoked(false)
                .build();
        return tokenRepository.save(refreshToken);
    }
}