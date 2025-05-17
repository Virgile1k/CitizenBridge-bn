package com.citizenbridge.citizenbridge.model;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class StatusHistory {
    private String status;
    private LocalDateTime updatedAt;


}