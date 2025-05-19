
package com.citizenbridge.citizenbridge.service;

import com.citizenbridge.citizenbridge.dtos.ComplaintDTO;
import com.citizenbridge.citizenbridge.dtos.ComplaintStatusUpdateDTO;
import com.citizenbridge.citizenbridge.dtos.GeminiCategoryResponse;
import com.citizenbridge.citizenbridge.model.Category;
import com.citizenbridge.citizenbridge.model.Complaint;
import com.citizenbridge.citizenbridge.model.StatusHistory;
import com.citizenbridge.citizenbridge.repository.CategoryRepository;
import com.citizenbridge.citizenbridge.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private static final Logger logger = LoggerFactory.getLogger(ComplaintService.class);

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private GeminiAIService geminiAIService;

    /**
     * Creates a new complaint with AI-assisted categorization
     */
    public Complaint createComplaint(ComplaintDTO complaintDTO, List<MultipartFile> attachments, UUID userId) {
        try {
            Complaint complaint = new Complaint();
            complaint.setReferenceNumber(generateReferenceNumber());
            complaint.setTitle(complaintDTO.getTitle());
            complaint.setDescription(complaintDTO.getDescription());
            complaint.setSubmittedBy(userId.toString());
            complaint.setLocation(complaintDTO.getLocation());
            complaint.setStatus("SUBMITTED");
            complaint.setPriority(complaintDTO.getPriority() != null ? complaintDTO.getPriority() : "MEDIUM");

            // Use AI to categorize if no category provided
            if (complaintDTO.getCategoryId() == null || complaintDTO.getCategoryId().isEmpty()) {
                GeminiCategoryResponse aiCategorization = geminiAIService.predictCategory(
                        complaintDTO.getTitle(),
                        complaintDTO.getDescription());

                if (aiCategorization.getCategoryId() != null) {
                    complaint.setCategoryId(aiCategorization.getCategoryId());
                    logger.info("AI categorized complaint as '{}' with confidence {}",
                            aiCategorization.getCategoryName(),
                            aiCategorization.getConfidence());

                    // Store the AI reasoning in metadata
                    complaint.setMetadata(createAICategoryMetadata(aiCategorization));
                } else {
                    // Default to uncategorized if AI categorization fails
                    Optional<Category> uncategorized = categoryRepository.findByIsActiveTrue().stream()
                            .filter(c -> "Uncategorized".equals(c.getName()))
                            .findFirst();

                    complaint.setCategoryId(uncategorized.map(Category::getId).orElse(null));
                    logger.warn("AI categorization failed: {}", aiCategorization.getReasoning());
                }
            } else {
                complaint.setCategoryId(complaintDTO.getCategoryId());
                complaint.setSubcategoryId(complaintDTO.getSubcategoryId());
            }

            // Handle agency assignment based on category
            if (complaint.getCategoryId() != null) {
                Optional<Category> category = categoryRepository.findById(complaint.getCategoryId());
                if (category.isPresent() && category.get().getDefaultAgencyId() != null) {
                    List<String> agencies = new ArrayList<>();
                    agencies.add(category.get().getDefaultAgencyId());
                    complaint.setAgencyId(agencies);
                }
            }

            // Set timestamps
            LocalDateTime now = LocalDateTime.now();
            complaint.setCreatedAt(now);
            complaint.setUpdatedAt(now);

            // Add initial status history
            StatusHistory initialStatus = new StatusHistory();
            initialStatus.setStatus("SUBMITTED");
            initialStatus.setUpdatedAt(now);
            complaint.setStatusHistory(List.of(initialStatus));

            // Handle attachments
            if (attachments != null && !attachments.isEmpty()) {
                List<String> attachmentKeys = new ArrayList<>();
                for (MultipartFile file : attachments) {
                    String key = s3Service.uploadFile(file, "complaints", userId);
                    attachmentKeys.add(key);
                }
                complaint.setAttachments(attachmentKeys);
            }

            // Save the complaint
            return complaintRepository.save(complaint);

        } catch (Exception e) {
            logger.error("Error creating complaint", e);
            throw new RuntimeException("Failed to create complaint: " + e.getMessage());
        }
    }

    /**
     * Creates metadata object for AI categorization
     */
    private Object createAICategoryMetadata(GeminiCategoryResponse aiCategorization) {
        return new Object() {
            public final String source = "gemini-ai";
            public final Double confidence = aiCategorization.getConfidence();
            public final String reasoning = aiCategorization.getReasoning();
            public final LocalDateTime timestamp = LocalDateTime.now();
        };
    }

    /**
     * Generates a unique reference number for the complaint
     */
    private String generateReferenceNumber() {
        // Format: CB-YEAR-RANDOMID (e.g., CB-2025-A7B3C2)
        String year = String.valueOf(LocalDateTime.now().getYear());
        String randomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "CB-" + year + "-" + randomId;
    }

    /**
     * Get complaints with filtering options
     */
    public Page<Complaint> getComplaints(UUID userId, String status, String priority,
                                         String categoryId, String agencyId, String searchTerm,
                                         Pageable pageable) {
        // Implementation would depend on the repository methods
        // This is a placeholder for the actual implementation

        // For a citizen user, only return their own complaints
        // For admin/agency users, filter based on their permissions and the provided filters

        // The actual implementation would need custom repository methods or specifications
        // to handle the filtering criteria

        return complaintRepository.findByFilters(userId.toString(), status, priority,
                categoryId, agencyId, searchTerm, pageable);
    }

    /**
     * Get a complaint by ID
     */
    public Optional<Complaint> getComplaintById(String id, UUID userId) {
        Optional<Complaint> complaint = complaintRepository.findById(id);

        // Check access rights - citizens can only view their own complaints
        // Agency users can view complaints assigned to their agency
        // Admins can view all complaints

        if (complaint.isPresent()) {
            Complaint foundComplaint = complaint.get();

            // For simplicity, assuming regular users can only see their own complaints
            // The actual implementation would need to check user roles
            if (foundComplaint.getSubmittedBy().equals(userId.toString()) ||
                    hasAccessRights(userId, foundComplaint)) {
                return complaint;
            }
            return Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * Update complaint status
     */
    public Complaint updateComplaintStatus(String id, ComplaintStatusUpdateDTO statusUpdateDTO, UUID userId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(id);
        if (complaintOpt.isEmpty()) {
            throw new RuntimeException("Complaint not found with ID: " + id);
        }

        Complaint complaint = complaintOpt.get();

        // Check if user has rights to update status
        if (!hasAccessRights(userId, complaint)) {
            throw new RuntimeException("User does not have permission to update this complaint");
        }

        // Update status
        complaint.setStatus(statusUpdateDTO.getStatus());
        complaint.setUpdatedAt(LocalDateTime.now());

        // Add new status history entry
        StatusHistory statusHistory = new StatusHistory();
        statusHistory.setStatus(statusUpdateDTO.getStatus());
        statusHistory.setComment(statusUpdateDTO.getComment());
        statusHistory.setUpdatedAt(LocalDateTime.now());
        statusHistory.setUpdatedBy(userId.toString());

        // Get existing history or create new list
        List<StatusHistory> history = complaint.getStatusHistory();
        if (history == null) {
            history = new ArrayList<>();
        } else {
            // Create a new list to avoid immutable list issues
            history = new ArrayList<>(history);
        }

        history.add(statusHistory);
        complaint.setStatusHistory(history);

        return complaintRepository.save(complaint);
    }

    /**
     * Assign complaint to agencies
     */
    public Complaint assignComplaintToAgencies(String id, List<String> agencyIds, UUID userId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(id);
        if (complaintOpt.isEmpty()) {
            throw new RuntimeException("Complaint not found with ID: " + id);
        }

        Complaint complaint = complaintOpt.get();

        // Check if user has rights to assign agencies
        if (!hasAdminRights(userId)) {
            throw new RuntimeException("User does not have permission to assign agencies");
        }

        complaint.setAgencyId(agencyIds);
        complaint.setUpdatedAt(LocalDateTime.now());

        // Add status history entry for assignment
        StatusHistory statusHistory = new StatusHistory();
        statusHistory.setStatus(complaint.getStatus());
        statusHistory.setComment("Assigned to agencies: " + String.join(", ", agencyIds));
        statusHistory.setUpdatedAt(LocalDateTime.now());
        statusHistory.setUpdatedBy(userId.toString());

        List<StatusHistory> history = complaint.getStatusHistory();
        if (history == null) {
            history = new ArrayList<>();
        } else {
            history = new ArrayList<>(history);
        }

        history.add(statusHistory);
        complaint.setStatusHistory(history);

        return complaintRepository.save(complaint);
    }

    /**
     * Add attachments to an existing complaint
     */
    public Complaint addAttachmentsToComplaint(String id, List<MultipartFile> attachments, UUID userId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(id);
        if (complaintOpt.isEmpty()) {
            throw new RuntimeException("Complaint not found with ID: " + id);
        }

        Complaint complaint = complaintOpt.get();

        // Check if user has rights to add attachments
        if (!complaint.getSubmittedBy().equals(userId.toString()) && !hasAccessRights(userId, complaint)) {
            throw new RuntimeException("User does not have permission to add attachments to this complaint");
        }

        if (attachments != null && !attachments.isEmpty()) {
            List<String> existingAttachments = complaint.getAttachments();
            if (existingAttachments == null) {
                existingAttachments = new ArrayList<>();
            } else {
                existingAttachments = new ArrayList<>(existingAttachments);
            }

            for (MultipartFile file : attachments) {
                try {
                    String key = s3Service.uploadFile(file, "complaints", userId);
                    existingAttachments.add(key);
                } catch (Exception e) {
                    logger.error("Error uploading attachment", e);
                    throw new RuntimeException("Failed to upload attachment: " + e.getMessage());
                }
            }

            complaint.setAttachments(existingAttachments);
            complaint.setUpdatedAt(LocalDateTime.now());

            return complaintRepository.save(complaint);
        }

        return complaint;
    }

    /**
     * Remove an attachment from a complaint
     */
    public Complaint removeAttachment(String id, String attachmentKey, UUID userId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(id);
        if (complaintOpt.isEmpty()) {
            throw new RuntimeException("Complaint not found with ID: " + id);
        }

        Complaint complaint = complaintOpt.get();

        // Check if user has rights to remove attachments
        if (!complaint.getSubmittedBy().equals(userId.toString()) && !hasAccessRights(userId, complaint)) {
            throw new RuntimeException("User does not have permission to remove attachments from this complaint");
        }

        List<String> attachments = complaint.getAttachments();
        if (attachments != null && attachments.contains(attachmentKey)) {
            // Remove from S3
            try {
                s3Service.deleteFile(attachmentKey);
            } catch (Exception e) {
                logger.error("Error deleting attachment from S3", e);
                // Continue with removal from database even if S3 deletion fails
            }

            // Remove from complaint
            attachments = new ArrayList<>(attachments);
            attachments.remove(attachmentKey);
            complaint.setAttachments(attachments);
            complaint.setUpdatedAt(LocalDateTime.now());

            return complaintRepository.save(complaint);
        } else {
            throw new RuntimeException("Attachment not found in complaint");
        }
    }

    /**
     * Get dashboard statistics
     */
    public Map<String, Object> getDashboardStats(UUID userId, String agencyId, String startDateStr, String endDateStr) {
        // Parse date strings if provided
        LocalDate startDate = startDateStr != null ?
                LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE) :
                LocalDate.now().minusMonths(1);

        LocalDate endDate = endDateStr != null ?
                LocalDate.parse(endDateStr, DateTimeFormatter.ISO_DATE) :
                LocalDate.now();

        // Check if user has admin rights
        boolean isAdmin = hasAdminRights(userId);

        // Get all relevant complaints based on user role and filters
        List<Complaint> complaints;
        if (isAdmin) {
            if (agencyId != null && !agencyId.isEmpty()) {
                complaints = complaintRepository.findByAgencyIdAndDateRange(agencyId, startDate, endDate);
            } else {
                complaints = complaintRepository.findByDateRange(startDate, endDate);
            }
        } else {
            // For agency users, only show complaints assigned to their agency
            String userAgencyId = getUserAgencyId(userId);
            if (userAgencyId == null) {
                throw new RuntimeException("User is not associated with any agency");
            }
            complaints = complaintRepository.findByAgencyIdAndDateRange(userAgencyId, startDate, endDate);
        }

        // Calculate statistics
        Map<String, Object> stats = new HashMap<>();

        // Total complaints
        stats.put("totalComplaints", complaints.size());

        // Complaints by status
        Map<String, Long> complaintsByStatus = complaints.stream()
                .collect(Collectors.groupingBy(Complaint::getStatus, Collectors.counting()));
        stats.put("complaintsByStatus", complaintsByStatus);

        // Complaints by priority
        Map<String, Long> complaintsByPriority = complaints.stream()
                .collect(Collectors.groupingBy(Complaint::getPriority, Collectors.counting()));
        stats.put("complaintsByPriority", complaintsByPriority);

        // Complaints by category
        Map<String, Long> complaintsByCategory = complaints.stream()
                .filter(c -> c.getCategoryId() != null)
                .collect(Collectors.groupingBy(Complaint::getCategoryId, Collectors.counting()));
        stats.put("complaintsByCategory", complaintsByCategory);

        // Complaints over time (by week)
        Map<String, Long> complaintsByWeek = complaints.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()));
        stats.put("complaintsByDate", complaintsByWeek);

        return stats;
    }

    /**
     * Check if a user has access rights to a complaint
     * This would be implemented based on the application's authorization model
     */
    private boolean hasAccessRights(UUID userId, Complaint complaint) {
        // Implementation would depend on the user roles and authorization model
        // For now, return true for simplicity
        return true;
    }

    /**
     * Check if a user has admin rights
     * This would be implemented based on the application's authorization model
     */
    private boolean hasAdminRights(UUID userId) {
        // Implementation would depend on the user roles and authorization model
        // For now, return true for simplicity
        return true;
    }

    /**
     * Get the agency ID associated with a user
     * This would be implemented based on the application's user model
     */
    private String getUserAgencyId(UUID userId) {
        // Implementation would depend on the user-agency relationship
        // For now, return null
        return null;
    }
}