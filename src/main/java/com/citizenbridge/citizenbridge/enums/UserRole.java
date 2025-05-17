package com.citizenbridge.citizenbridge.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    USER("Regular citizen user"),
    ADMIN("System administrator with full privileges"),
    AGENCY_REP("Government agency representative"),
    AGENCY_ADMIN("Agency administrator"),
    MODERATOR("Content and complaint moderator"),
    SUPPORT("Customer support representative");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }
}