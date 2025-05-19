package com.citizenbridge.citizenbridge.service;
import com.citizenbridge.citizenbridge.dtos.TrackingUpdateDTO;
import com.citizenbridge.citizenbridge.model.Complaint;
import com.citizenbridge.citizenbridge.model.StatusHistory;
import com.citizenbridge.citizenbridge.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingService.class);

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    /**
     * Updates the status of a complaint and notifies the user
     */
    public Complaint updateStatus(String complaintId, String newStatus, String comment, UUID updatedBy) {
        try {
            Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);

            if (complaintOpt.isEmpty()) {
                throw new RuntimeException("Complaint not found with ID: " + complaintId);
            }

            Complaint complaint = complaintOpt.get();
            String oldStatus = complaint.getStatus();
            complaint.setStatus(newStatus);

            // Create status history entry
            StatusHistory statusUpdate = new StatusHistory();
            statusUpdate.setStatus(newStatus);
//            statusUpdate.setUpdatedBy(updatedBy.toString());
            statusUpdate.setComment(comment);
            statusUpdate.setUpdatedAt(LocalDateTime.now());

            // Add to history list
            List<StatusHistory> history = complaint.getStatusHistory();
            if (history == null) {
                history = new ArrayList<>();
            }
            history.add(statusUpdate);
            complaint.setStatusHistory(history);

            // Update complaint
            complaint.setUpdatedAt(LocalDateTime.now());

            // Calculate expected resolution date if not already set and status = IN_PROGRESS
            if (complaint.getExpectedResolutionDate() == null && "IN_PROGRESS".equals(newStatus)) {
                // Default to 7 days from now for resolution
                complaint.setExpectedResolutionDate(LocalDateTime.now().plusDays(7));
            }

            // Save complaint
            complaint = complaintRepository.save(complaint);

            // Send real-time update via WebSocket
            sendRealTimeUpdate(complaint);

            // Send notification to user
            notificationService.sendStatusUpdateNotification(complaint, oldStatus, newStatus);

            return complaint;
        } catch (Exception e) {
            logger.error("Error updating complaint status", e);
            throw new RuntimeException("Failed to update complaint status: " + e.getMessage());
        }
    }

    /**
     * Sends real-time updates to subscribed clients via WebSocket
     */
    private void sendRealTimeUpdate(Complaint complaint) {
        try {
            TrackingUpdateDTO update = new TrackingUpdateDTO();
            update.setComplaintId(complaint.getId());
            update.setReferenceNumber(complaint.getReferenceNumber());
            update.setStatus(complaint.getStatus());
            update.setUpdatedAt(complaint.getUpdatedAt());

            // Get the most recent status history entry
            if (complaint.getStatusHistory() != null && !complaint.getStatusHistory().isEmpty()) {
                StatusHistory latestUpdate = complaint.getStatusHistory().get(complaint.getStatusHistory().size() - 1);
                update.setComment(latestUpdate.getComment());
            }

            // Send to specific user's topic
            messagingTemplate.convertAndSend("/topic/complaint/" + complaint.getId(), update);

            // Also send to user-specific topic
            messagingTemplate.convertAndSend("/topic/user/" + complaint.getSubmittedBy(), update);
        } catch (Exception e) {
            logger.error("Error sending real-time update via WebSocket", e);
        }
    }

    /**
     * Gets the tracking information for a complaint
     */
    public List<StatusHistory> getTrackingHistory(String complaintId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);

        if (complaintOpt.isEmpty()) {
            throw new RuntimeException("Complaint not found with ID: " + complaintId);
        }

        return complaintOpt.get().getStatusHistory();
    }
}