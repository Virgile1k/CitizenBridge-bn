
package com.citizenbridge.citizenbridge.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StatusHistory {
    private String status;
    private String comment;
    private LocalDateTime updatedAt;
    private String updatedBy;  // Added this field to match service usage
}