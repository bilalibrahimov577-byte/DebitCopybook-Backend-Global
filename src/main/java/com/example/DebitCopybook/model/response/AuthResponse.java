package com.example.DebitCopybook.model.response; // Paketi öz proyektinin adına uyğun dəyişdir

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder; // Əgər builder istifadə etmək istəsəniz

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token; // Yaradılmış JWT token
    private Long userId;
    private String userName;
    private String userEmail;

    // Xəta mesajları üçün əlavə konstruktor
    public AuthResponse(String message) {
        this.token = null;
        this.userId = null;
        this.userName = message; // Mesajı burada saxlaya bilərik
        this.userEmail = null;
    }
}