package com.citizenbridge.citizenbridge.dtos;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ComplaintStatusUpdateDTO {
    private String status;
    private String comments;
    private String internalNotes;
    private String Comment;
}