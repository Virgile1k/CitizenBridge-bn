package com.citizenbridge.citizenbridge.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiCategoryResponse {
    private String categoryId;
    private String categoryName;
    private String reasoning;
    private Double confidence;
}