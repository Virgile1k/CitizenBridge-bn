package com.citizenbridge.citizenbridge.repository;

import com.citizenbridge.citizenbridge.model.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRolesRepository extends JpaRepository<UserRoles, Long> {
    List<UserRoles> findByUserId(UUID userId);
}