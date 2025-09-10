package com.example.DebitCopybook.controller; // Paketi öz proyektinin adına uyğun dəyişdir

import com.example.DebitCopybook.dao.entity.UserEntity; // Sizin UserEntity adınız
import com.example.DebitCopybook.model.request.GoogleSignInRequest; // Yeni request DTO
import com.example.DebitCopybook.model.response.AuthResponse; // Yeni response DTO
import com.example.DebitCopybook.service.GoogleTokenVerifierService;
import com.example.DebitCopybook.service.JwtService;
import com.example.DebitCopybook.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth") // Bu controller üçün əsas URL
@RequiredArgsConstructor
public class AuthController {

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserService userService;
    private final JwtService jwtService;

    // BILAL, bu endpoint Flutter tətbiqindən Google ID tokeni qəbul edəcək.
    // Bunu əlavə etməsək Flutter tətbiqi ilə Google ilə qeydiyyat/daxil olma İŞLƏMƏYƏCƏK.
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@RequestBody GoogleSignInRequest request) {
        // 1. Flutter-dən gələn Google ID Tokeni doğrula
        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(request.getIdToken());
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse("Invalid Google ID Token"));
        }

        // 2. İstifadəçini tap və ya yarat
        // Admin rolunun təyin edilməsi məntiqini burada və ya UserService-də həyata keçirəcəyik.
        // Qeyd etdiyin kimi, ilk qeydiyyatdan keçən admin, qalanları user olacaq.
        UserEntity user = userService.findOrCreateUser(payload); // UserEntity qaytarır
        if (user == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse("User creation failed"));
        }

        // 3. Tətbiq üçün JWT yarat
        String jwt = jwtService.generateToken(user); // UserEntity obyektini ötürürük

        // 4. JWT-ni və istifadəçi məlumatlarını Flutter tətbiqinə geri qaytar
        return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getName(), user.getEmail()));
    }
}