package com.citizenbridge.citizenbridge.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
@Setter
@Getter

@Document(collection = "categories")
public class Category {
    @Id
    private String id;

    private String name;
    private String description;
    private String parentId;
    private Boolean isActive;
    private String defaultAgencyId;
    private List<String> requiredFields;
    private List<String> customFields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}