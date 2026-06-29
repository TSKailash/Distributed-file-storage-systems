package com.kailash.storageservice.dto;

import com.kailash.storageservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class UserDTO {
    private UUID id;
    private String name;
    private String email;
    private Role role;
}
