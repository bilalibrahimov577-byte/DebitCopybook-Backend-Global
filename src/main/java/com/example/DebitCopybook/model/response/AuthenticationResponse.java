package com.example.DebitCopybook.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationResponse {
    private String token;
    private Long userId;
    private String userName;
    private String userEmail;
    private String debtId;
}
