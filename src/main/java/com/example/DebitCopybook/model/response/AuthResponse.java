package com.example.DebitCopybook.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String userName;
    private String userEmail;


    public AuthResponse(String message) {
        this.token = null;
        this.userId = null;
        this.userName = message;
        this.userEmail = null;
    }
}