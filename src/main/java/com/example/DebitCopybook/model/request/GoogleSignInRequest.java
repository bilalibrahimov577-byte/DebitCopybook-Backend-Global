package com.example.DebitCopybook.model.request; // Paketi öz proyektinin adına uyğun dəyişdir

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder; // Əgər builder istifadə etmək istəsəniz

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleSignInRequest {
    private String idToken; // Flutter-dən gələn Google ID token
}