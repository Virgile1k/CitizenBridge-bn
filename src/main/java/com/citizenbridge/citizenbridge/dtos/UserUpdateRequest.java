package com.citizenbridge.citizenbridge.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class UserUpdateRequest {

    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;

    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;

    // Additional fields for admin/agency users
    private String organization;
    private String department;
    private String jobTitle;

    // For arbitrary preferences
    private Map<String, String> customPreferences;
    private String profileImageKey;
}