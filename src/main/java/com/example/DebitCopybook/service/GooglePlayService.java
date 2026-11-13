package com.example.DebitCopybook.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.SubscriptionPurchase;
import com.google.api.services.androidpublisher.model.SubscriptionPurchasesAcknowledgeRequest;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.Collections;

@Service
public class GooglePlayService {

    // Bura Flutter-də qeyd etdiyin paketin adını yazırsan (məsələn: com.example.debitcopybook)
    private final String packageName = "com.bilalibrahimov.borcdefteri";

    private final AndroidPublisher androidPublisher;

    public GooglePlayService() throws Exception {
        // Render-də "Secret Files" bölməsində yaratdığın faylın yolu
        String filePath = "/etc/secrets/google-key.json";

        // Faylı birbaşa path vasitəsilə oxuyuruq
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream(filePath)
        ).createScoped(Collections.singleton("https://www.googleapis.com/auth/androidpublisher"));

        this.androidPublisher = new AndroidPublisher.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("DebitCopybook").build();
    }

    public SubscriptionPurchase verifyAndAcknowledge(String subscriptionId, String purchaseToken) throws Exception {
        // Google-dan ödənişin statusunu çəkirik
        SubscriptionPurchase purchase = androidPublisher.purchases().subscriptions()
                .get(packageName, subscriptionId, purchaseToken).execute();

        // Əgər hələ təsdiqlənməyibsə, TƏSDİQLƏYİRİK! (Refund-un qarşısı alınır)
        if (purchase.getAcknowledgementState() != null && purchase.getAcknowledgementState() == 0) {
            SubscriptionPurchasesAcknowledgeRequest ackRequest = new SubscriptionPurchasesAcknowledgeRequest();
            androidPublisher.purchases().subscriptions()
                    .acknowledge(packageName, subscriptionId, purchaseToken, ackRequest).execute();
        }

        return purchase;
    }


    /**
     * Yalnız statusu oxumaq üçün - acknowledge etmir.
     * Scheduler-də mövcud abunəliyin Google-dakı real vəziyyətini yoxlamaq üçün istifadə olunur.
     */
    public SubscriptionPurchase getSubscriptionStatus(String subscriptionId, String purchaseToken) throws Exception {
        return androidPublisher.purchases().subscriptions()
                .get(packageName, subscriptionId, purchaseToken).execute();
    }
}


