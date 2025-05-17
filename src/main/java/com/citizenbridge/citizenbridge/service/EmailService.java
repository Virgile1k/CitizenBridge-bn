package com.citizenbridge.citizenbridge.service;

import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Sends a welcome email to a newly registered user
     */
    public void sendSignupEmail(Users user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to CitizenBridge");

            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("loginUrl", frontendUrl + "/login");
            String htmlContent = templateEngine.process("signup-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            // Log the error but don't throw exception - email sending should not block the process
            System.err.println("Failed to send signup email: " + e.getMessage());
        }
    }

    /**
     * Sends an email to a user created by an admin, including their roles
     */
    public void sendAdminCreatedAccountEmail(Users user, List<UserRole> roles, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(user.getEmail());
            helper.setSubject("Your CitizenBridge Account");

            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("loginUrl", frontendUrl + "/login");

            // Format roles for display
            String roleDisplay = roles.stream()
                    .map(role -> role.getDescription())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("User");
            context.setVariable("userRole", roleDisplay);

            // Include temporary password if provided
            if (temporaryPassword != null && !temporaryPassword.isEmpty()) {
                context.setVariable("tempPassword", temporaryPassword);
            }

            String htmlContent = templateEngine.process("admin-created-account-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send admin-created account email: " + e.getMessage());
        }
    }

    /**
     * Sends a password reset notification
     */
    public void sendPasswordResetEmail(Users user, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(user.getEmail());
            helper.setSubject("CitizenBridge Password Reset");

            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("resetLink", resetLink);

            String htmlContent = templateEngine.process("password-reset-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    /**
     * Sends a notification when a user's account status changes
     */
    public void sendAccountStatusChangeEmail(Users user, boolean isActive) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(user.getEmail());
            helper.setSubject(isActive ?
                    "Your CitizenBridge Account has been Activated" :
                    "Your CitizenBridge Account has been Deactivated");

            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("isActive", isActive);
            context.setVariable("loginUrl", frontendUrl + "/login");

            String templateName = isActive ? "account-activated-email" : "account-deactivated-email";
            String htmlContent = templateEngine.process(templateName, context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send account status change email: " + e.getMessage());
        }
    }

    /**
     * Sends a notification when a user's role changes
     */
    public void sendRoleChangeEmail(Users user, List<UserRole> newRoles) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(user.getEmail());
            helper.setSubject("Your CitizenBridge Role Has Been Updated");

            Context context = new Context();
            context.setVariable("user", user);

            // Format roles for display
            String roleDisplay = newRoles.stream()
                    .map(role -> role.getDescription())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("User");
            context.setVariable("roles", roleDisplay);
            context.setVariable("loginUrl", frontendUrl + "/login");

            String htmlContent = templateEngine.process("role-changed-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send role change email: " + e.getMessage());
        }
    }
}