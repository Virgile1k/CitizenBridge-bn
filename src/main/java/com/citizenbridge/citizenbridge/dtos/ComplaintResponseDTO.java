package com.citizenbridge.citizenbridge.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ComplaintResponseDTO {
    private String id;
    private String referenceNumber;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String submittedBy;
    private String categoryId;
    private String categoryName;
    private String subcategoryId;
    private String subcategoryName;
    private List<String> agencyId;
    private List<String> agencyNames;
    private String location;
    private List<String> attachments;
    private List<String> attachmentUrls;
    private Object metadata;
    private String createdAt;
    private String updatedAt;
    private List<StatusHistoryDTO> statusHistory;
}

