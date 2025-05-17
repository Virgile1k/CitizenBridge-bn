package com.citizenbridge.citizenbridge.dtos;

import com.citizenbridge.citizenbridge.enums.UserRole;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<UserRole> roles;
    private Boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Additional fields for admin/agency users
    private String organization;
    private String department;
    private String jobTitle;

    // Profile image URL
    private String profileImageUrl;

    // Useful for summary views
    public String getFullName() {
        return firstName + " " + lastName;
    }
}