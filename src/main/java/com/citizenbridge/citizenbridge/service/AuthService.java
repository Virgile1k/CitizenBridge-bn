package com.citizenbridge.citizenbridge.service;

import com.citizenbridge.citizenbridge.dtos.AdminUserCreateRequest;
import com.citizenbridge.citizenbridge.dtos.LoginRequest;
import com.citizenbridge.citizenbridge.dtos.LoginResponse;
import com.citizenbridge.citizenbridge.dtos.SignupRequest;
import com.citizenbridge.citizenbridge.dtos.SignupResponse;
import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.exceptions.AuthException;
import com.citizenbridge.citizenbridge.model.Roles;
import com.citizenbridge.citizenbridge.model.UserRoles;
import com.citizenbridge.citizenbridge.model.Users;
import com.citizenbridge.citizenbridge.repository.RolesRepository;
import com.citizenbridge.citizenbridge.repository.UserRolesRepository;
import com.citizenbridge.citizenbridge.repository.UsersRepository;
import com.citizenbridge.citizenbridge.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private UserRolesRepository userRolesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            User userDetails = (User) authentication.getPrincipal();

            // Check if user is active
            Users user = usersRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new AuthException.InvalidCredentialsException("Invalid username or password"));

            if (!user.getIsActive()) {
                throw new AuthException.AccountDeactivatedException("Your account has been deactivated. Please contact the administrator.");
            }

            String token = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            return new LoginResponse(token, "Bearer", jwtExpiration / 1000, refreshToken);
        } catch (AuthException.AccountDeactivatedException e) {
            // Rethrow account-specific exceptions
            throw e;
        } catch (Exception e) {
            throw new AuthException.InvalidCredentialsException("Invalid username or password");
        }
    }

    @Transactional
    public SignupResponse signup(SignupRequest signupRequest) {
        // Check if username already exists
        if (usersRepository.findByUsername(signupRequest.getUsername()).isPresent()) {
            throw new AuthException.UserAlreadyExistsException("Username is already taken");
        }

        // Check if email already exists
        if (usersRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new AuthException.UserAlreadyExistsException("Email is already registered");
        }

        // Create new user
        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(signupRequest.getPassword()));
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setPhoneNumber(signupRequest.getPhoneNumber());
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        user.setIsActive(true);

        // Set default empty JSON object for preferences
        try {
            user.setPreferences(objectMapper.readTree("{}"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set default preferences", e);
        }

        Users savedUser = usersRepository.save(user);

        // Assign USER role to the new user
        assignRoleToUser(savedUser.getId(), UserRole.USER);

        // Send welcome email
        emailService.sendSignupEmail(savedUser);

        return new SignupResponse(
                "User registered successfully",
                savedUser.getUsername(),
                savedUser.getEmail()
        );
    }

    /**
     * Creates a new admin/agency user (for admin use)
     */
    @Transactional
    public UUID createAdministrativeUser(AdminUserCreateRequest request) {
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

        // Set organization and other preferences in the JSON field
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

        // Assign roles to the user
        List<UserRole> assignedRoles;
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            assignedRoles = request.getRoles();
            for (UserRole role : assignedRoles) {
                assignRoleToUser(savedUser.getId(), role);
            }
        } else {
            // Assign default AGENCY_REP role if no roles specified
            assignRoleToUser(savedUser.getId(), UserRole.AGENCY_REP);
            assignedRoles = List.of(UserRole.AGENCY_REP);
        }

        // Send email notification to the new user
        emailService.sendAdminCreatedAccountEmail(savedUser, assignedRoles, request.getPassword());

        return savedUser.getId();
    }

    @Transactional(readOnly = true)
    public List<UserRole> getUserRoles(UUID userId) {
        return userRolesRepository.findByUserId(userId)
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignRoleToUser(UUID userId, UserRole roleName) {
        // Find or create the role
        Roles role = rolesRepository.findByName(roleName)
                .orElseGet(() -> rolesRepository.save(new Roles(roleName)));

        // Check if user already has this role
        boolean hasRole = userRolesRepository.findByUserId(userId)
                .stream()
                .anyMatch(ur -> ur.getRole().getName() == roleName);

        if (!hasRole) {
            // Create user_role association
            UserRoles userRole = new UserRoles(userId, role.getId());
            userRolesRepository.save(userRole);
        }
    }

    /**
     * Updates roles for a user and sends notification
     */
    @Transactional
    public void updateUserRoles(UUID userId, List<UserRole> newRoles) {
        // First check if user exists
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new AuthException.UserNotFoundException("User not found with ID: " + userId));

        // Remove existing roles (optional - depends on your requirements)
        // userRolesRepository.deleteByUserId(userId);

        // Assign new roles
        for (UserRole role : newRoles) {
            assignRoleToUser(userId, role);
        }

        // Send notification about role change
        emailService.sendRoleChangeEmail(user, newRoles);
    }

    /**
     * Resets a user's password and sends notification
     */
    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new AuthException.UserNotFoundException("User not found with ID: " + userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        usersRepository.save(user);

        // Generate a password reset link (for self-service reset)
        String resetLink = frontendUrl + "/reset-password?token=" + jwtUtil.generatePasswordResetToken(user.getUsername());

        // Send notification
        emailService.sendPasswordResetEmail(user, resetLink);
    }

    /**
     * Activates or deactivates a user account
     */
    @Transactional
    public void updateAccountStatus(UUID userId, boolean isActive) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new AuthException.UserNotFoundException("User not found with ID: " + userId));

        user.setIsActive(isActive);
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        usersRepository.save(user);

        // Send notification about account status change
        emailService.sendAccountStatusChangeEmail(user, isActive);
    }
}