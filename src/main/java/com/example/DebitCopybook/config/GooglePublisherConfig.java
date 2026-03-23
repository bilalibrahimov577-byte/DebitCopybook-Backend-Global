package com.example.DebitCopybook.config;



import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Slf4j
@Configuration
public class GooglePublisherConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${google.application-credentials}")
    private String credentialsSource;

    @Bean
    public AndroidPublisher androidPublisher() throws IOException, GeneralSecurityException {
        // MAL BAZARI proyektindəki kimi konkret yolu bura yazırıq
        String filePath = "/etc/secrets/google-key.json";
       // String filePath =  "/etc/secrets/debitcopybook-backend-1224a0e5082.json";

        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Google JSON faylı tapılmadı! Baxılan yol: " + file.getAbsolutePath());
        }

        InputStream credentialsStream = new FileInputStream(file);

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));

        return new AndroidPublisher.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }
}
