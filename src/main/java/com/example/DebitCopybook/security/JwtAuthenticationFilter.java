package com.example.DebitCopybook.security; // Yeni paket yarada bilərsən və ya config paketində saxlaya bilərsən

import com.example.DebitCopybook.dao.entity.UserEntity; // Sizin UserEntity adınız
import com.example.DebitCopybook.service.JwtService;
import com.example.DebitCopybook.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull; // Yeni import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
// BILAL, bu filter Spring Security zəncirinə əlavə ediləcək.
// Bu filter olmadan, hər hansı bir API endpoint-i təhlükəsiz olmayacaq və JWT tokenləri yoxlanılmayacaq.
// Deməli, təhlükəsizlik sistemi İŞLƏMƏYƏCEK.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService; // İstifadəçi məlumatlarını yükləmək üçün

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization"); // Authorization başlığını oxuyuruq
        final String jwt;
        final String userEmail; // JWT-də subject olaraq email-i saxlayırıq

        // Əgər Authorization başlığı yoxdursa və ya "Bearer " ilə başlamırsa, növbəti filterə keçirik
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // "Bearer " hissəsini kəsib tokeni alırıq
        userEmail = jwtService.extractUsername(jwt); // Tokendən istifadəçi emailini çıxarırıq

        // Əgər email tapılıbsa və cari anda SecurityContext-də heç bir identifikasiya yoxdursa
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // İstifadəçini email ilə verilənlər bazasından yükləyirik
            UserEntity userDetails = this.userService.findByEmail(userEmail)
                    .orElse(null); // UserService-dən findByEmail metodunu istifadə edirik

            if (userDetails != null && jwtService.isTokenValid(jwt, userDetails)) {
                // Əgər token etibarlıdırsa, istifadəçini Spring SecurityContext-ə daxil edirik
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, // Bizim UserEntity obyekti UserDetails implementasiya etdiyi üçün birbaşa istifadə edirik
                        null,
                        userDetails.getAuthorities() // İstifadəçinin rollarını əlavə edirik (ROLE_USER)
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request) // Sorğu detallarını əlavə edirik
                );
                SecurityContextHolder.getContext().setAuthentication(authToken); // İstifadəçini kontekstə qoyuruq
            }
        }
        filterChain.doFilter(request, response); // Növbəti filterə keçirik
    }
}