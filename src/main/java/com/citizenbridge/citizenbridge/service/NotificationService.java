package com.citizenbridge.citizenbridge.service;

import com.citizenbridge.citizenbridge.model.Complaint;
import com.citizenbridge.citizenbridge.model.Notification;
import com.citizenbridge.citizenbridge.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${citizenbridge.base-url}")
    private String baseUrl;

    /**
     * Sends a status update notification to the user
     */
    @Async
    public void sendStatusUpdateNotification(Complaint complaint, String oldStatus, String newStatus) {
        try {
            // Create notification record
            Notification notification = new Notification();
            notification.setUserId(complaint.getSubmittedBy());
            notification.setComplaintId(complaint.getId());
            notification.setType("STATUS_UPDATE");
            notification.setMessage("Your complaint status has been updated from " + oldStatus + " to " + newStatus);
            notification.setDeliveryChannels(Arrays.asList("EMAIL", "IN_APP"));
            notification.setCreatedAt(LocalDateTime.now());

            Map<String, Object> deliveryStatus = new HashMap<>();
            deliveryStatus.put("inApp", "PENDING");
            deliveryStatus.put("email", "PENDING");
            notification.setDeliveryStatus(deliveryStatus);

            // Save notification
            notification = notificationRepository.save(notification);

            // Send email notification
            sendStatusUpdateEmail(complaint, oldStatus, newStatus, notification.getId());

            // Update delivery status
            deliveryStatus.put("inApp", "DELIVERED");
            notification.setDeliveryStatus(deliveryStatus);
            notificationRepository.save(notification);

        } catch (Exception e) {
            logger.error("Error sending status update notification", e);
        }
    }

    /**
     * Sends a status update email using Thymeleaf template
     */
    private void sendStatusUpdateEmail(Complaint complaint, String oldStatus, String newStatus, String notificationId) {
        try {
            // Get user email (would normally be fetched from user service)
            String toEmail = "user@example.com"; // Placeholder - actual implementation would fetch user email

            // Prepare context for Thymeleaf template
            Context context = new Context();
            context.setVariable("complaint", complaint);
            context.setVariable("oldStatus", oldStatus);
            context.setVariable("newStatus", newStatus);
            context.setVariable("trackingUrl", baseUrl + "/complaints/track/" + complaint.getReferenceNumber());
            context.setVariable("referenceNumber", complaint.getReferenceNumber());

            String emailContent = templateEngine.process("status-update-notification", context);

            // Create email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Update on your complaint: " + complaint.getReferenceNumber());
            helper.setText(emailContent, true);

            // Send email
            mailSender.send(message);

            // Update notification status
            Notification notification = notificationRepository.findById(notificationId).orElse(null);
            if (notification != null) {
                Map<String, Object> deliveryStatus = (Map<String, Object>) notification.getDeliveryStatus();
                deliveryStatus.put("email", "DELIVERED");
                notification.setDeliveryStatus(deliveryStatus);
                notificationRepository.save(notification);
            }

        } catch (MessagingException e) {
            logger.error("Error sending status update email", e);
        }
    }

    /**
     * Sends a confirmation notification when a complaint is created
     */
    @Async
    public void sendComplaintConfirmationNotification(Complaint complaint) {
        try {
            // Create notification record
            Notification notification = new Notification();
            notification.setUserId(complaint.getSubmittedBy());
            notification.setComplaintId(complaint.getId());
            notification.setType("COMPLAINT_SUBMITTED");
            notification.setMessage("Your complaint has been successfully submitted with reference number " + complaint.getReferenceNumber());
            notification.setDeliveryChannels(Arrays.asList("EMAIL", "IN_APP"));
            notification.setCreatedAt(LocalDateTime.now());

            Map<String, Object> deliveryStatus = new HashMap<>();
            deliveryStatus.put("inApp", "PENDING");
            deliveryStatus.put("email", "PENDING");
            notification.setDeliveryStatus(deliveryStatus);

            // Save notification
            notification = notificationRepository.save(notification);

            // Send email notification
            sendComplaintConfirmationEmail(complaint, notification.getId());

            // Update delivery status
            deliveryStatus.put("inApp", "DELIVERED");
            notification.setDeliveryStatus(deliveryStatus);
            notificationRepository.save(notification);

        } catch (Exception e) {
            logger.error("Error sending complaint confirmation notification", e);
        }
    }

    /**
     * Sends a complaint confirmation email using Thymeleaf template
     */
    private void sendComplaintConfirmationEmail(Complaint complaint, String notificationId) {
        try {
            // Get user email (would normally be fetched from user service)
            String toEmail = "user@example.com"; // Placeholder - actual implementation would fetch user email

            // Prepare context for Thymeleaf template
            Context context = new Context();
            context.setVariable("complaint", complaint);
            context.setVariable("trackingUrl", baseUrl + "/complaints/track/" + complaint.getReferenceNumber());
            context.setVariable("referenceNumber", complaint.getReferenceNumber());

            String emailContent = templateEngine.process("complaint-confirmation", context);

            // Create email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your complaint has been submitted: " + complaint.getReferenceNumber());
            helper.setText(emailContent, true);

            // Send email
            mailSender.send(message);

            // Update notification status
            Notification notification = notificationRepository.findById(notificationId).orElse(null);
            if (notification != null) {
                Map<String, Object> deliveryStatus = (Map<String, Object>) notification.getDeliveryStatus();
                deliveryStatus.put("email", "DELIVERED");
                notification.setDeliveryStatus(deliveryStatus);
                notificationRepository.save(notification);
            }

        } catch (MessagingException e) {
            logger.error("Error sending complaint confirmation email", e);
        }
    }
}