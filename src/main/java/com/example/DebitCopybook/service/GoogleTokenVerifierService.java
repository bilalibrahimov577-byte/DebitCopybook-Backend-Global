package com.example.DebitCopybook.service; // Paketi öz proyektinin adına uyğun dəyişdir

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory; // GsonFactory-i əlavə edək (və ya JacksonFactory)
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections; // Collections importu əlavə edildi

@Service
public class GoogleTokenVerifierService {

    private static final HttpTransport TRANSPORT = new NetHttpTransport();

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();


    @Value("${google.oauth.client-id.android}")
    private String androidClientId;

    @Value("${google.oauth.client-id.web}")
    private String webClientId;


    public GoogleIdToken.Payload verify(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(TRANSPORT, JSON_FACTORY)

                .setAudience(Arrays.asList(webClientId, androidClientId))

                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            }
        } catch (GeneralSecurityException | IOException e) {

            System.err.println("Google ID Token doğrulama xətası: " + e.getMessage());
        }
        return null;
    }
}