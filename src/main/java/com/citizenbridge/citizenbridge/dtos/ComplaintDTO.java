package com.citizenbridge.citizenbridge.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ComplaintDTO {
    private String title;
    private String description;
    private String categoryId;
    private String subcategoryId;
    private String priority;
    private String location;
    private List<String> tags;
    private Double latitude;
    private Double longitude;
    private String additionalDetails;
}