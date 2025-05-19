package com.citizenbridge.citizenbridge.dtos;

import lombok.Data;

@Data
public class StatusHistoryDTO {
    private String status;
    private String updatedBy;
    private String updatedAt;
    private String comments;
}
