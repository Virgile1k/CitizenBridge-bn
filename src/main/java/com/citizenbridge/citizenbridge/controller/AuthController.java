package com.citizenbridge.citizenbridge.controller;

import com.citizenbridge.citizenbridge.dtos.LoginRequest;
import com.citizenbridge.citizenbridge.dtos.LoginResponse;
import com.citizenbridge.citizenbridge.dtos.SignupRequest;
import com.citizenbridge.citizenbridge.dtos.SignupResponse;
import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        SignupResponse response = authService.signup(signupRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<UserRole>> getCurrentUserRoles(Authentication authentication) {
        // Get current user ID from authentication
        UUID userId = UUID.fromString(authentication.getName());
        List<UserRole> roles = authService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/roles/{userId}/{role}")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable UUID userId,
            @PathVariable UserRole role) {
        // This endpoint should be protected by ADMIN role in security config
        authService.assignRoleToUser(userId, role);
        return ResponseEntity.ok().build();
    }
}