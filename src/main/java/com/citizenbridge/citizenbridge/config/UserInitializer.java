package com.citizenbridge.citizenbridge.config;

import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.model.Users;
import com.citizenbridge.citizenbridge.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Initializes default users with predefined roles on application startup.
 */
@Component
@Order(2)
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(String... args) {
        createDefaultUsers();
    }

    private void createDefaultUsers() {
        createUserIfNotExists("john.doe", "john.doe@citizenbridge.com", "John", "Doe", UserRole.USER);
        createUserIfNotExists("admin", "admin@citizenbridge.com", "System", "Administrator", UserRole.ADMIN);
        createUserIfNotExists("agency.rep", "agency.rep@citizenbridge.com", "Agency", "Representative", UserRole.AGENCY_REP);
        createUserIfNotExists("agency.admin", "agency.admin@citizenbridge.com", "Agency", "Administrator", UserRole.AGENCY_ADMIN);
        createUserIfNotExists("moderator", "moderator@citizenbridge.com", "Content", "Moderator", UserRole.MODERATOR);
        createUserIfNotExists("support", "support@citizenbridge.com", "Customer", "Support", UserRole.SUPPORT);
    }

    private void createUserIfNotExists(String username, String email, String firstName, String lastName, UserRole userRole) {
        if (usersRepository.existsByUsername(username) || usersRepository.existsByEmail(email)) {
            System.out.println("User " + username + " already exists, skipping creation.");
            return;
        }

        try {
            Users user = new Users();
            user.setId(UUID.randomUUID());
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(getDefaultPassword(userRole)));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhoneNumber(generatePhoneNumber(username));
            user.setIsActive(true);
            user.setRole(userRole);
            user.setPreferences(createDefaultPreferences(userRole));

            Timestamp now = new Timestamp(System.currentTimeMillis());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            usersRepository.save(user);
            System.out.println("Created user: " + username + " with role: " + userRole);

        } catch (Exception e) {
            System.err.println("Failed to create user " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getDefaultPassword(UserRole role) {
        return switch (role) {
            case USER -> "user123";
            case ADMIN -> "admin123";
            case AGENCY_REP -> "agency123";
            case AGENCY_ADMIN -> "agencyadmin123";
            case MODERATOR -> "moderator123";
            case SUPPORT -> "support123";
        };
    }

    private String generatePhoneNumber(String username) {
        int hash = Math.abs(username.hashCode());
        return String.format("+1555%07d", hash % 10000000);
    }

    private com.fasterxml.jackson.databind.JsonNode createDefaultPreferences(UserRole role) {
        try {
            String preferencesJson = switch (role) {
                case USER -> "{\"notifications\":{\"email\":true,\"sms\":false,\"push\":true},\"privacy\":{\"profileVisibility\":\"public\",\"allowContactFromAgencies\":true},\"ui\":{\"theme\":\"light\",\"language\":\"en\"}}";
                case ADMIN -> "{\"notifications\":{\"email\":true,\"sms\":true,\"push\":true,\"adminAlerts\":true},\"admin\":{\"dashboardLayout\":\"advanced\",\"autoApproveUsers\":false,\"systemNotifications\":true},\"ui\":{\"theme\":\"dark\",\"language\":\"en\"}}";
                case AGENCY_REP -> "{\"notifications\":{\"email\":true,\"sms\":false,\"push\":true,\"citizenRequests\":true},\"agency\":{\"autoResponse\":false,\"responseTimeTarget\":24,\"publicProfile\":true},\"ui\":{\"theme\":\"light\",\"language\":\"en\"}}";
                case AGENCY_ADMIN -> "{\"notifications\":{\"email\":true,\"sms\":true,\"push\":true,\"agencyAlerts\":true,\"staffManagement\":true},\"agency\":{\"autoResponse\":false,\"responseTimeTarget\":12,\"staffNotifications\":true,\"publicProfile\":true},\"ui\":{\"theme\":\"light\",\"language\":\"en\"}}";
                case MODERATOR -> "{\"notifications\":{\"email\":true,\"sms\":false,\"push\":true,\"contentReports\":true,\"urgentFlags\":true},\"moderation\":{\"autoHideReported\":false,\"requiredApprovals\":1,\"escalationThreshold\":3},\"ui\":{\"theme\":\"light\",\"language\":\"en\"}}";
                case SUPPORT -> "{\"notifications\":{\"email\":true,\"sms\":false,\"push\":true,\"supportTickets\":true,\"urgentSupport\":true},\"support\":{\"autoAssignment\":true,\"responseTimeTarget\":2,\"escalationEnabled\":true},\"ui\":{\"theme\":\"light\",\"language\":\"en\"}}";
            };
            return objectMapper.readTree(preferencesJson);
        } catch (Exception e) {
            System.err.println("Failed to create preferences for role " + role + ": " + e.getMessage());
            try {
                return objectMapper.readTree("{}");
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
