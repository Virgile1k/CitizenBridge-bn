package com.citizenbridge.citizenbridge.controller;

import com.citizenbridge.citizenbridge.dtos.AdminUserCreateRequest;
import com.citizenbridge.citizenbridge.dtos.UserProfileResponse;
import com.citizenbridge.citizenbridge.dtos.UserUpdateRequest;
import com.citizenbridge.citizenbridge.dtos.PasswordChangeRequest;
import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserManagementController {

    @Autowired
    private UserManagementService userManagementService;

    /**
     * Endpoint for creating administrative users
     * Only accessible by ADMIN users
     */
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UUID> createAdminUser(@Valid @RequestBody AdminUserCreateRequest request) {
        UUID userId = userManagementService.createAdminUser(request);
        return new ResponseEntity<>(userId, HttpStatus.CREATED);
    }

    // ============================================
    // SELF-SERVICE ENDPOINTS (No Admin Required)
    // ============================================

    /**
     * Get current user's profile
     * Users can access their own profile without admin privileges
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserProfileResponse profile = userManagementService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update current user's profile
     * Users can update their own profile without admin privileges
     */
    @PutMapping("/profile")
    public ResponseEntity<Void> updateCurrentUserProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        userManagementService.updateUserProfile(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Upload profile image for current user
     * Users can upload their own profile image without admin privileges
     */
    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadCurrentUserProfileImage(
            @RequestParam("image") MultipartFile imageFile,
            Authentication authentication) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        String imageUrl = userManagementService.uploadProfileImage(userId, imageFile);
        return ResponseEntity.ok(imageUrl);
    }

    /**
     * Delete current user's profile image
     * Users can delete their own profile image without admin privileges
     */
    @DeleteMapping("/profile/image")
    public ResponseEntity<Void> deleteCurrentUserProfileImage(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        userManagementService.deleteProfileImage(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get presigned URL for direct client-side profile image upload
     * Users can get upload URL for their own image without admin privileges
     */
    @GetMapping("/profile/image-upload-url")
    public ResponseEntity<String> getProfileImageUploadUrl(
            Authentication authentication,
            @RequestParam String fileExtension,
            @RequestParam String contentType) {
        UUID userId = UUID.fromString(authentication.getName());
        String presignedUrl = userManagementService.getProfileImageUploadUrl(userId, fileExtension, contentType);
        return ResponseEntity.ok(presignedUrl);
    }

    /**
     * Change current user's password
     * Users can change their own password without admin privileges
     */
    @PutMapping("/profile/change-password")
    public ResponseEntity<Void> changeCurrentUserPassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        userManagementService.changeUserPassword(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deactivate current user's account
     * Users can deactivate their own account without admin privileges
     */
    @PutMapping("/profile/deactivate")
    public ResponseEntity<Void> deactivateCurrentUserAccount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        userManagementService.deactivateOwnAccount(userId);
        return ResponseEntity.ok().build();
    }

    // ========================================
    // ADMIN-ONLY ENDPOINTS
    // ========================================

    /**
     * Get specific user's profile (admin only)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID userId) {
        UserProfileResponse profile = userManagementService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update any user's profile (admin only)
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        userManagementService.updateUserProfile(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Activate/deactivate user (admin only)
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam boolean active) {
        userManagementService.updateUserStatus(userId, active);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all users (admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userManagementService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Update user roles (admin only)
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserRoles(
            @PathVariable UUID userId,
            @RequestBody List<UserRole> roles) {
        userManagementService.updateUserRoles(userId, roles);
        return ResponseEntity.ok().build();
    }

    /**
     * Reset user password (admin only)
     */
    @PutMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetUserPassword(
            @PathVariable UUID userId,
            @RequestParam String newPassword) {
        userManagementService.resetUserPassword(userId, newPassword);
        return ResponseEntity.ok().build();
    }

    /**
     * Upload profile image for any user (admin only)
     */
    @PostMapping(value = "/{userId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadUserProfileImage(
            @PathVariable UUID userId,
            @RequestParam("image") MultipartFile imageFile) throws IOException {
        String imageUrl = userManagementService.uploadProfileImage(userId, imageFile);
        return ResponseEntity.ok(imageUrl);
    }

    /**
     * Get presigned URL for direct client-side profile image upload (admin can generate for any user)
     */
    @GetMapping("/{userId}/image-upload-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getUserProfileImageUploadUrl(
            @PathVariable UUID userId,
            @RequestParam String fileExtension,
            @RequestParam String contentType) {
        String presignedUrl = userManagementService.getProfileImageUploadUrl(userId, fileExtension, contentType);
        return ResponseEntity.ok(presignedUrl);
    }
}