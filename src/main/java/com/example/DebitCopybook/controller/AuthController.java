package com.example.DebitCopybook.controller;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.model.request.GoogleSignInRequest;
import com.example.DebitCopybook.model.response.AuthResponse;
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
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@RequestBody GoogleSignInRequest request) {

        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(request.getIdToken());
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse("Invalid Google ID Token"));
        }


        UserEntity user = userService.findOrCreateUser(payload);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse("User creation failed"));
        }


        String jwt = jwtService.generateToken(user);


        return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getName(), user.getEmail()));
    }
}