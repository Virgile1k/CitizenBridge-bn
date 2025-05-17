package com.citizenbridge.citizenbridge.dtos;
import com.citizenbridge.citizenbridge.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRolesResponse {
    private List<UserRole> roles;
    private String username;
}