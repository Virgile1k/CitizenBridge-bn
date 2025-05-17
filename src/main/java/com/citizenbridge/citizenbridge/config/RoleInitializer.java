package com.citizenbridge.citizenbridge.config;

import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.model.Roles;
import com.citizenbridge.citizenbridge.repository.RolesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Arrays;

/**
 * This component initializes the default roles in the database on application startup
 */
@Component
public class RoleInitializer implements CommandLineRunner {

    @Autowired
    private RolesRepository rolesRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Initialize all roles from the UserRole enum
        Arrays.stream(UserRole.values()).forEach(role -> {
            if (!rolesRepository.findByName(role).isPresent()) {
                Roles newRole = new Roles();
                newRole.setName(role);
                newRole.setDescription(role.getDescription());
                newRole.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                rolesRepository.save(newRole);
            }
        });
    }
}