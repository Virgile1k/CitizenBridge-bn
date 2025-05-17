package com.citizenbridge.citizenbridge.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "agencies")
@Getter
@Setter
public class Agencies {
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "jurisdiction", columnDefinition = "jsonb")
    private String jurisdiction;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

}