package com.citizenbridge.citizenbridge.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@Document(collection = "complaints")
public class Complaint {
    @Id
    private String id;

    private String referenceNumber;
    private String title;
    private String description;
    private String categoryId;
    private String subcategoryId;
    private String submittedBy;
    private Object location; // GeoJSON
    private List<String> attachments;
    private String status;
    private String priority;
    private List<String> agencyId;
    private List<StatusHistory> statusHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expectedResolutionDate;
    private Object metadata;


}
