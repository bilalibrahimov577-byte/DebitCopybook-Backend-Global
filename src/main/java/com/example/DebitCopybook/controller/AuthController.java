package com.example.DebitCopybook.controller;

import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.model.request.GoogleSignInRequest;
// AuthenticationResponse DTO-sunu import edirik
import com.example.DebitCopybook.model.response.AuthenticationResponse;
import com.example.DebitCopybook.service.GoogleTokenVerifierService;
import com.example.DebitCopybook.service.JwtService;
import com.example.DebitCopybook.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/google")
    // Metodun qaytardığı tipi yeni AuthenticationResponse DTO-su ilə əvəz edirik
    public ResponseEntity<AuthenticationResponse> authenticateWithGoogle(@RequestBody GoogleSignInRequest request) {

        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(request.getIdToken());
        if (payload == null) {
            // Xəta halında body-də heç nə göndərmirik, sadəcə 401 statusu kifayətdir.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // UserService bizim üçün user-i tapır və ya yaradır (artıq debtId-ni də yaradır).
        UserEntity user = userService.findOrCreateUser(payload);

        // Bu yoxlama artıq UserService-in içində olduğu üçün burada ehtiyac yoxdur,
        // amma təhlükəsizlik üçün qala bilər.
        if (user == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // İstifadəçi üçün JWT token yaradırıq.
        String jwt = jwtService.generateToken(user);

        // Yeni DTO-muzdan istifadə edərək səliqəli bir cavab hazırlayırıq.
        AuthenticationResponse response = AuthenticationResponse.builder()
                .token(jwt)
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .debtId(user.getDebtId()) // <-- ƏSAS ƏLAVƏ BUDUR
                .build();

        return ResponseEntity.ok(response);
    }
}