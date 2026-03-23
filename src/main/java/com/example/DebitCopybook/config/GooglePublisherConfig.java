package com.example.DebitCopybook.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GooglePublisherConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    // Render-də təyin etdiyimiz environment variable-dan yolu oxuyuruq
    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String credentialsPath;

    @Bean
    public AndroidPublisher androidPublisher() throws IOException, GeneralSecurityException {
        File file = new File(credentialsPath);
        if (!file.exists()) {
            throw new FileNotFoundException("Fayl tapılmadı! Baxılan yol: " + file.getAbsolutePath());
        }

        InputStream credentialsStream = new FileInputStream(file);
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
               // .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROID_PUBLISHER));
                .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));

        return new AndroidPublisher.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }
}
