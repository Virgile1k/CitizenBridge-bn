package com.citizenbridge.citizenbridge.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter

@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;

    private String userId;
    private String complaintId;
    private String type;
    private String message;
    private List<String> deliveryChannels;
    private Object deliveryStatus;
    private LocalDateTime createdAt;
}