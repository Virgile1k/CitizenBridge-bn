package com.citizenbridge.citizenbridge.service;

import com.citizenbridge.citizenbridge.dtos.AdminUserCreateRequest;
import com.citizenbridge.citizenbridge.dtos.PasswordChangeRequest;
import com.citizenbridge.citizenbridge.dtos.UserProfileResponse;
import com.citizenbridge.citizenbridge.dtos.UserUpdateRequest;
import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.exceptions.AuthException;
import com.citizenbridge.citizenbridge.exceptions.ResourceNotFoundException;
import com.citizenbridge.citizenbridge.model.Users;
import com.citizenbridge.citizenbridge.repository.UsersRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserManagementService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private S3Service s3Service;

    private static final String PROFILE_IMAGES_DIRECTORY = "profile-images";

    /**
     * Creates a new user with administrative roles
     * Only users with ADMIN role can create users with administrative privileges
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UUID createAdminUser(AdminUserCreateRequest request) {
        // Check if username already exists
        if (usersRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AuthException.UserAlreadyExistsException("Username is already taken");
        }

        // Check if email already exists
        if (usersRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException.UserAlreadyExistsException("Email is already registered");
        }

        // Create new user
        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        user.setIsActive(true);

        // Set the primary role - updated to use the role field directly
        UserRole primaryRole = request.getRoles() != null && !request.getRoles().isEmpty()
                ? request.getRoles().get(0)
                : UserRole.AGENCY_REP;
        user.setRole(primaryRole);

        // Set organization or department if provided
        try {
            ObjectNode preferencesNode = objectMapper.createObjectNode();
            if (request.getOrganization() != null && !request.getOrganization().isEmpty()) {
                preferencesNode.put("organization", request.getOrganization());
            }
            if (request.getDepartment() != null && !request.getDepartment().isEmpty()) {
                preferencesNode.put("department", request.getDepartment());
            }
            if (request.getJobTitle() != null && !request.getJobTitle().isEmpty()) {
                preferencesNode.put("jobTitle", request.getJobTitle());
            }

            user.setPreferences(preferencesNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user preferences", e);
        }

        Users savedUser = usersRepository.save(user);

        return savedUser.getId();
    }

    /**
     * Retrieves user profile information with profile image URL
     * Can be used by the user themselves or by admins
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Use the direct role from the entity instead of authService
        List<UserRole> roles = user.getRole() != null ?
                Collections.singletonList(user.getRole()) :
                Collections.emptyList();

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRoles(roles);
        response.setIsActive(user.getIsActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        // Full name is automatically derived via getFullName() method in the DTO

        // Generate profile image URL if image key exists
        if (user.getProfileImageKey() != null && !user.getProfileImageKey().isEmpty()) {
            String imageUrl = s3Service.generatePresignedUrl(user.getProfileImageKey());
            response.setProfileImageUrl(imageUrl);
        }

        // Extract additional user preferences
        try {
            JsonNode preferences = user.getPreferences();
            if (preferences != null) {
                if (preferences.has("organization")) {
                    response.setOrganization(preferences.get("organization").asText());
                }
                if (preferences.has("department")) {
                    response.setDepartment(preferences.get("department").asText());
                }
                if (preferences.has("jobTitle")) {
                    response.setJobTitle(preferences.get("jobTitle").asText());
                }
                // Extract any other custom preferences as needed
            }
        } catch (Exception e) {
            // Log the error but continue with available data
            System.err.println("Failed to parse user preferences: " + e.getMessage());
        }

        return response;
    }

    /**
     * Updates user profile information
     * Can be used by the user themselves or by admins
     */
    @Transactional
    public void updateUserProfile(UUID userId, UserUpdateRequest request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update basic user properties
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if new email is already in use
            usersRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(userId)) {
                            throw new AuthException.UserAlreadyExistsException("Email is already registered");
                        }
                    });
            user.setEmail(request.getEmail());
        }

        // Update profile image key if provided
        if (request.getProfileImageKey() != null) {
            // Remove old image if exists
            if (user.getProfileImageKey() != null && !user.getProfileImageKey().isEmpty()) {
                try {
                    s3Service.deleteFile(user.getProfileImageKey());
                } catch (Exception e) {
                    // Log error but continue
                    System.err.println("Failed to delete old profile image: " + e.getMessage());
                }
            }
            user.setProfileImageKey(request.getProfileImageKey());
        }

        // Update preferences
        try {
            JsonNode currentPreferences = user.getPreferences();
            ObjectNode updatedPreferences = (currentPreferences instanceof ObjectNode)
                    ? (ObjectNode) currentPreferences
                    : objectMapper.createObjectNode();

            // Update specific preference fields if provided
            if (request.getOrganization() != null) {
                updatedPreferences.put("organization", request.getOrganization());
            }
            if (request.getDepartment() != null) {
                updatedPreferences.put("department", request.getDepartment());
            }
            if (request.getJobTitle() != null) {
                updatedPreferences.put("jobTitle", request.getJobTitle());
            }

            // Add any additional custom preferences
            if (request.getCustomPreferences() != null) {
                for (Map.Entry<String, String> entry : request.getCustomPreferences().entrySet()) {
                    updatedPreferences.put(entry.getKey(), entry.getValue());
                }
            }

            user.setPreferences(updatedPreferences);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user preferences", e);
        }

        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        usersRepository.save(user);
    }

    /**
     * Uploads and sets user profile image
     * Can be used by the user themselves or by admins
     */
    @Transactional
    public String uploadProfileImage(UUID userId, MultipartFile imageFile) throws IOException {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Delete old image if exists
        if (user.getProfileImageKey() != null && !user.getProfileImageKey().isEmpty()) {
            try {
                s3Service.deleteFile(user.getProfileImageKey());
            } catch (Exception e) {
                // Log error but continue
                System.err.println("Failed to delete old profile image: " + e.getMessage());
            }
        }

        // Upload new image
        String imageKey = s3Service.uploadFile(imageFile, PROFILE_IMAGES_DIRECTORY, userId);

        // Update user profile
        user.setProfileImageKey(imageKey);
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        usersRepository.save(user);

        // Return the presigned URL
        return s3Service.generatePresignedUrl(imageKey);
    }

    /**
     * Deletes user profile image
     * Can be used by the user themselves or by admins
     */
    @Transactional
    public void deleteProfileImage(UUID userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getProfileImageKey() != null && !user.getProfileImageKey().isEmpty()) {
            try {
                s3Service.deleteFile(user.getProfileImageKey());
                user.setProfileImageKey(null);
                user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                usersRepository.save(user);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete profile image", e);
            }
        }
    }

    /**
     * Changes user's own password
     * Users can change their own password without admin privileges
     */
    @Transactional
    public void changeUserPassword(UUID userId, @Valid PasswordChangeRequest request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect");
        }

        // Validate new password (you might want to add more validation)
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AuthException("New password must be at least 8 characters long");
        }

        // Check if new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AuthException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        usersRepository.save(user);
    }

    /**
     * Updates user status (active/inactive)
     * Admin only functionality
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void updateUserStatus(UUID userId, boolean isActive) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setIsActive(isActive);
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        usersRepository.save(user);
    }

    /**
     * Retrieves all system users (for admin management)
     * Admin only functionality
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        List<Users> users = usersRepository.findAll();

        return users.stream()
                .map(user -> {
                    UserProfileResponse response = new UserProfileResponse();
                    response.setId(user.getId());
                    response.setUsername(user.getUsername());
                    response.setEmail(user.getEmail());
                    response.setFirstName(user.getFirstName());
                    response.setLastName(user.getLastName());

                    // Use the direct role from the entity instead of authService
                    List<UserRole> roles = user.getRole() != null ?
                            Collections.singletonList(user.getRole()) :
                            Collections.emptyList();
                    response.setRoles(roles);

                    response.setIsActive(user.getIsActive());
                    response.setCreatedAt(user.getCreatedAt());
                    response.setUpdatedAt(user.getUpdatedAt());

                    // Full name is automatically derived via getFullName() method in the DTO

                    // Generate profile image URL if image key exists
                    if (user.getProfileImageKey() != null && !user.getProfileImageKey().isEmpty()) {
                        String imageUrl = s3Service.generatePresignedUrl(user.getProfileImageKey());
                        response.setProfileImageUrl(imageUrl);
                    }

                    // Extract organization from preferences if available
                    try {
                        JsonNode preferences = user.getPreferences();
                        if (preferences != null) {
                            if (preferences.has("organization")) {
                                response.setOrganization(preferences.get("organization").asText());
                            }
                            if (preferences.has("department")) {
                                response.setDepartment(preferences.get("department").asText());
                            }
                            if (preferences.has("jobTitle")) {
                                response.setJobTitle(preferences.get("jobTitle").asText());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse user preferences: " + e.getMessage());
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Updates user roles
     * Admin only functionality
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void updateUserRoles(UUID userId, List<UserRole> roles) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update the user's role to the first role in the list
        if (roles != null && !roles.isEmpty()) {
            user.setRole(roles.get(0));
            usersRepository.save(user);
        }
    }

    /**
     * Changes user password (for admins)
     * Admin only functionality
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resetUserPassword(UUID userId, String newPassword) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        usersRepository.save(user);
    }

    /**
     * Generates a presigned URL for direct client image upload
     * Can be used by the user themselves or by admins
     */
    public String getProfileImageUploadUrl(UUID userId, String fileExtension, String contentType) {
        String key = PROFILE_IMAGES_DIRECTORY + "/" + userId + "/" + System.currentTimeMillis() + fileExtension;
        return s3Service.createPresignedUploadUrl(key, contentType, java.time.Duration.ofMinutes(15));
    }

    /**
     * Deactivates user account (soft delete)
     * Users can deactivate their own account
     */
    @Transactional
    public void deactivateOwnAccount(UUID userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setIsActive(false);
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        usersRepository.save(user);
    }
}