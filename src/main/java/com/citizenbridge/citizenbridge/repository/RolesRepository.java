package com.citizenbridge.citizenbridge.repository;

import com.citizenbridge.citizenbridge.enums.UserRole;
import com.citizenbridge.citizenbridge.model.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolesRepository extends JpaRepository<Roles, Long> {
    Optional<Roles> findByName(UserRole name);
}