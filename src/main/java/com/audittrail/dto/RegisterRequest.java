package com.audittrail.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String role; // ADMIN, DEVELOPER, VIEWER
}
