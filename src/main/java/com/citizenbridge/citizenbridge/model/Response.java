package com.citizenbridge.citizenbridge.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@Document(collection = "responses")
public class Response {
    @Id
    private String id;

    private String complaintId;
    private String respondedBy;
    private String agencyId;
    private String responseText;
    private Boolean isPublic;
    private List<String> attachments;
    private List<String> internalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}