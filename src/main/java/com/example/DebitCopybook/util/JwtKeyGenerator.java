package com.example.DebitCopybook.util; // Uyğun paketi təyin et

import java.security.SecureRandom;
import java.util.Base64;

public class JwtKeyGenerator {
    public static void main(String[] args) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32]; // 256 bit açar (HS256 üçün minimum 32 bayt = 256 bit)
        secureRandom.nextBytes(key);
        String base64Key = Base64.getEncoder().encodeToString(key);
        System.out.println("Generated JWT Secret Key: " + base64Key);
    }
}