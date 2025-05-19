package com.citizenbridge.citizenbridge.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrackingUpdateDTO {
    private String complaintId;
    private String referenceNumber;
    private String status;
    private LocalDateTime updatedAt;
    private String comment;
}