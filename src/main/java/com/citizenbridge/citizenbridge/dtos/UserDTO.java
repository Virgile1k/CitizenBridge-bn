package com.citizenbridge.citizenbridge.dtos;

import com.citizenbridge.citizenbridge.enums.UserRole;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserDTO {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role; // Primary role of the user
}