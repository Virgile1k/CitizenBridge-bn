package com.citizenbridge.citizenbridge.controller;

import com.citizenbridge.citizenbridge.dtos.ComplaintDTO;
import com.citizenbridge.citizenbridge.dtos.ComplaintStatusUpdateDTO;
import com.citizenbridge.citizenbridge.dtos.GeminiCategoryResponse;
import com.citizenbridge.citizenbridge.dtos.S3PresignedUrlResponse;
import com.citizenbridge.citizenbridge.model.Complaint;
import com.citizenbridge.citizenbridge.service.ComplaintService;
import com.citizenbridge.citizenbridge.service.GeminiAIService;
import com.citizenbridge.citizenbridge.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "Complaint Management", description = "APIs for complaint submission and management")
@SecurityRequirement(name = "bearerAuth")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private GeminiAIService geminiAIService;

    @Autowired
    private S3Service s3Service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a new complaint", description = "Creates a new complaint with optional attachments")
    @ApiResponse(responseCode = "201", description = "Complaint created successfully")
    @PreAuthorize("hasAnyAuthority('CITIZEN', 'ADMIN', 'AGENCY_ADMIN', 'AGENCY_REP')")
    public ResponseEntity<Complaint> createComplaint(
            @Parameter(description = "Complaint data") @RequestPart("complaint") ComplaintDTO complaintDTO,
            @Parameter(description = "Optional file attachments") @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        Complaint createdComplaint = complaintService.createComplaint(complaintDTO, attachments, userId);
        return new ResponseEntity<>(createdComplaint, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all complaints", description = "Retrieves all complaints with pagination and filtering options")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved complaints")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Complaint>> getAllComplaints(
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) String categoryId,
            @Parameter(description = "Filter by agency ID") @RequestParam(required = false) String agencyId,
            @Parameter(description = "Search term for title/description") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        Page<Complaint> complaints = complaintService.getComplaints(userId, status, priority, categoryId, agencyId, searchTerm, pageable);
        return ResponseEntity.ok(complaints);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get complaint by ID", description = "Retrieves a specific complaint by its ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved complaint")
    @ApiResponse(responseCode = "404", description = "Complaint not found")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Complaint> getComplaintById(
            @Parameter(description = "Complaint ID") @PathVariable String id,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        Optional<Complaint> complaint = complaintService.getComplaintById(id, userId);

        return complaint
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update complaint status", description = "Updates the status of a complaint")
    @ApiResponse(responseCode = "200", description = "Status updated successfully")
    @ApiResponse(responseCode = "404", description = "Complaint not found")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AGENCY_ADMIN', 'AGENCY_REP')")
    public ResponseEntity<Complaint> updateComplaintStatus(
            @Parameter(description = "Complaint ID") @PathVariable String id,
            @Parameter(description = "Status update data") @RequestBody ComplaintStatusUpdateDTO statusUpdateDTO,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        Complaint updatedComplaint = complaintService.updateComplaintStatus(id, statusUpdateDTO, userId);
        return ResponseEntity.ok(updatedComplaint);
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Assign complaint to agencies", description = "Assigns a complaint to one or more government agencies")
    @ApiResponse(responseCode = "200", description = "Complaint assigned successfully")
    @ApiResponse(responseCode = "404", description = "Complaint not found")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AGENCY_ADMIN')")
    public ResponseEntity<Complaint> assignComplaintToAgencies(
            @Parameter(description = "Complaint ID") @PathVariable String id,
            @Parameter(description = "List of agency IDs") @RequestBody List<String> agencyIds,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        Complaint updatedComplaint = complaintService.assignComplaintToAgencies(id, agencyIds, userId);
        return ResponseEntity.ok(updatedComplaint);
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "Add attachments to complaint", description = "Adds new attachments to an existing complaint")
    @ApiResponse(responseCode = "200", description = "Attachments added successfully")
    @ApiResponse(responseCode = "404", description = "Complaint not found")
    @PreAuthorize("hasAnyAuthority('CITIZEN', 'ADMIN', 'AGENCY_ADMIN', 'AGENCY_REP')")
    public ResponseEntity<Complaint> addAttachmentsToComplaint(
            @Parameter(description = "Complaint ID") @PathVariable String id,
            @Parameter(description = "File attachments") @RequestPart("attachments") List<MultipartFile> attachments,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        Complaint updatedComplaint = complaintService.addAttachmentsToComplaint(id, attachments, userId);
        return ResponseEntity.ok(updatedComplaint);
    }

    @DeleteMapping("/{id}/attachments/{attachmentKey}")
    @Operation(summary = "Delete attachment", description = "Removes an attachment from a complaint")
    @ApiResponse(responseCode = "200", description = "Attachment deleted successfully")
    @ApiResponse(responseCode = "404", description = "Complaint or attachment not found")
    @PreAuthorize("hasAnyAuthority('CITIZEN', 'ADMIN', 'AGENCY_ADMIN', 'AGENCY_REP')")
    public ResponseEntity<Complaint> deleteAttachment(
            @Parameter(description = "Complaint ID") @PathVariable String id,
            @Parameter(description = "Attachment key") @PathVariable String attachmentKey,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        Complaint updatedComplaint = complaintService.removeAttachment(id, attachmentKey, userId);
        return ResponseEntity.ok(updatedComplaint);
    }

    @GetMapping("/attachments/{attachmentKey}")
    @Operation(summary = "Get attachment download URL", description = "Generates a pre-signed URL for downloading an attachment")
    @ApiResponse(responseCode = "200", description = "URL generated successfully")
    @ApiResponse(responseCode = "404", description = "Attachment not found")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getAttachmentUrl(
            @Parameter(description = "Attachment key") @PathVariable String attachmentKey) {

        String presignedUrl = s3Service.generatePresignedUrl(attachmentKey);
        return ResponseEntity.ok(Map.of("downloadUrl", presignedUrl));
    }

    @GetMapping("/presigned-upload-url")
    @Operation(summary = "Get presigned upload URL", description = "Generates a pre-signed URL for direct upload to S3")
    @ApiResponse(responseCode = "200", description = "URL generated successfully")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<S3PresignedUrlResponse> getPresignedUploadUrl(
            @Parameter(description = "File content type") @RequestParam String contentType,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        String key = "complaints/" + userId + "/" + System.currentTimeMillis() + getExtensionFromContentType(contentType);
        String presignedUrl = s3Service.createPresignedUploadUrl(key, contentType, Duration.ofMinutes(15));

        S3PresignedUrlResponse response = new S3PresignedUrlResponse();
        response.setUploadUrl(presignedUrl);
        response.setKey(key);
        response.setExpiresIn(15 * 60); // 15 minutes in seconds

        return ResponseEntity.ok(response);
    }

    @PostMapping("/predict-category")
    @Operation(summary = "Predict category using AI", description = "Uses Gemini AI to predict the most appropriate category for a complaint")
    @ApiResponse(responseCode = "200", description = "Category prediction successful",
            content = @Content(schema = @Schema(implementation = GeminiCategoryResponse.class)))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GeminiCategoryResponse> predictCategory(
            @Parameter(description = "Complaint title") @RequestParam String title,
            @Parameter(description = "Complaint description") @RequestParam String description) {

        GeminiCategoryResponse prediction = geminiAIService.predictCategory(title, description);
        return ResponseEntity.ok(prediction);
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get dashboard statistics", description = "Retrieves statistics for the dashboard")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AGENCY_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @Parameter(description = "Filter by agency ID") @RequestParam(required = false) String agencyId,
            @Parameter(description = "Start date (ISO format)") @RequestParam(required = false) String startDate,
            @Parameter(description = "End date (ISO format)") @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        Map<String, Object> stats = complaintService.getDashboardStats(userId, agencyId, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    private String getExtensionFromContentType(String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "application/pdf":
                return ".pdf";
            case "application/msword":
                return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";
            default:
                return "";
        }
    }
}