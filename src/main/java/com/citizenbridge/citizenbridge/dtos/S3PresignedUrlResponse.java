package com.citizenbridge.citizenbridge.dtos;

import lombok.Data;

@Data
public class S3PresignedUrlResponse {
    private String uploadUrl;
    private String key;
    private Integer expiresIn;
}
