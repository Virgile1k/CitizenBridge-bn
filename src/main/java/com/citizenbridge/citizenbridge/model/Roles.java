package com.citizenbridge.citizenbridge.model;

import com.citizenbridge.citizenbridge.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public Roles() {}

    public Roles(UserRole name) {
        this.name = name;
        this.description = name.getDescription();
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
}
