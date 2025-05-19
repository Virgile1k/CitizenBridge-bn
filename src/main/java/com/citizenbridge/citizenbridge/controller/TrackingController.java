package com.citizenbridge.citizenbridge.controller;

import com.citizenbridge.citizenbridge.model.StatusHistory;
import com.citizenbridge.citizenbridge.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Complaint Tracking", description = "APIs for tracking complaint status and updates")
public class TrackingController {

    @Autowired
    private TrackingService trackingService;

    @GetMapping("/{complaintId}")
    @Operation(summary = "Get tracking history", description = "Retrieves the full tracking history of a complaint")
    @ApiResponse(responseCode = "200", description = "Tracking history retrieved successfully")
    public ResponseEntity<List<StatusHistory>> getTrackingHistory(
            @Parameter(description = "Complaint ID") @PathVariable String complaintId) {

        List<StatusHistory> history = trackingService.getTrackingHistory(complaintId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{complaintId}/status")
    @Operation(summary = "Update complaint status", description = "Updates the status of a complaint")
    @ApiResponse(responseCode = "200", description = "Status updated successfully")
    public ResponseEntity<?> updateStatus(
            @Parameter(description = "Complaint ID") @PathVariable String complaintId,
            @Parameter(description = "New status") @RequestParam String status,
            @Parameter(description = "Status update comment") @RequestParam(required = false) String comment,
            @AuthenticationPrincipal Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        trackingService.updateStatus(complaintId, status, comment, userId);
        return ResponseEntity.ok().build();
    }
}