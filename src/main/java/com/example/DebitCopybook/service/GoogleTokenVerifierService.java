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
import java.util.Collections; // Collections importu əlavə edildi

@Service
public class GoogleTokenVerifierService {

    private static final HttpTransport TRANSPORT = new NetHttpTransport();
    // BILAL, GsonFactory və ya JacksonFactory istifadə etməlisən.
    // Əgər pom.xml/build.gradle faylına com.google.api.client:google-api-client əlavə etmisənsə,
    // adətən orada GsonFactory və ya JacksonFactory üçün də asılılıq olur.
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance(); // Və ya JacksonFactory.getDefaultInstance();

    // BILAL, bu client ID-lər application.properties-də təyin edilməlidir.
    // Flutter tətbiqin üçün Android client ID-si və backend üçün Web client ID-si.
    // Bunlar olmadan Google ID token doğrulama İŞLƏMƏYƏCƏK.
    @Value("${google.oauth.client-id.android}")
    private String androidClientId;

    @Value("${google.oauth.client-id.web}") // Backend-in özü də tokenin auditoriyası ola bilər
    private String webClientId;

    // BILAL, bu metod ÇOX VACİBDİR. Flutter-dən gələn Google ID tokenlərini doğrular.
    // Bunu əlavə etməsək Google ilə daxil olma İŞLƏMƏYƏCƏK.
    public GoogleIdToken.Payload verify(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(TRANSPORT, JSON_FACTORY)
                // Audience olaraq həm Flutter tətbiqin (Android client ID), həm də backend-in (Web client ID)
                // ID-lərini təyin edə bilərik. Token ya Flutter üçün, ya da backend üçün buraxılmış ola bilər.
                .setAudience(Collections.singletonList(androidClientId))
                // .setAudience(Arrays.asList(webClientId, androidClientId)) // Əgər hər ikisini dəstəkləmək istəsən
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            }
        } catch (GeneralSecurityException | IOException e) {
            // Xətanı log-a yazmaq vacibdir.
            System.err.println("Google ID Token doğrulama xətası: " + e.getMessage());
        }
        return null;
    }
}