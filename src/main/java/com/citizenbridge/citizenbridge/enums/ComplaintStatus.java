package com.citizenbridge.citizenbridge.enums;

/**
 * Enum representing the different statuses a complaint can have
 */
public enum ComplaintStatus {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    REJECTED("Rejected");

    private final String displayName;

    ComplaintStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}