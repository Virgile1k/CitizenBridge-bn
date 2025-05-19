package com.citizenbridge.citizenbridge.controller;

import com.citizenbridge.citizenbridge.dtos.*;
import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserDTO userDetails = authService.getUserDetails(userId);
        return ResponseEntity.ok(userDetails);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<UserRole>> getCurrentUserRoles(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<UserRole> roles = authService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/role")
    public ResponseEntity<UserRole> getCurrentUserPrimaryRole(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserRole primaryRole = authService.getUserPrimaryRole(userId);
        return ResponseEntity.ok(primaryRole);
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