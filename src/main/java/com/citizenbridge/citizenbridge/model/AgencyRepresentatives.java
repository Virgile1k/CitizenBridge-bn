package com.citizenbridge.citizenbridge.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "agency_representatives")
public class AgencyRepresentatives {
    @Id
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "agency_id", columnDefinition = "UUID")
    private UUID agencyId;

    @Column(name = "assigned_at", nullable = false)
    private Timestamp assignedAt;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "name") // Added name of representative
    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "agency_id", insertable = false, updatable = false)
    private Agencies agency;

}