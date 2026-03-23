package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.PaymentHistoryEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.PaymentHistoryRepository;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.google.api.services.androidpublisher.AndroidPublisher;

import com.google.api.services.androidpublisher.model.SubscriptionPurchase;
import com.google.api.services.androidpublisher.model.SubscriptionPurchasesAcknowledgeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleBillingService {

    private final AndroidPublisher androidPublisher;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;

    @Value("${google.play-store.package-name}")
    private String packageName;

    /**
     * Google-dan gələn ödəniş tokenini doğrulayır və abunəliyi aktiv edir.
     */
    @Transactional
    public void verifyAndActivateSubscription(String purchaseToken, String productId, UserEntity user) throws Exception {

        // 1. DUBLİKAT YOXLAMASI
        if (paymentHistoryRepository.existsByPurchaseToken(purchaseToken)) {
            log.info("Bu ödəniş tokeni artıq işlənilib: {}", purchaseToken);
            return;
        }

        // 2. GOOGLE SERVERLƏRİNDƏN YOXLAMA
        SubscriptionPurchase purchase = androidPublisher.purchases().subscriptions()
                .get(packageName, productId, purchaseToken)
                .execute();

        // 3. STATUS YOXLAMASI (paymentState: 1 = Ödəniş uğurludur, 0 = Gözləmədə, 2 = Sınaq)
        if (purchase.getPaymentState() != null && purchase.getPaymentState() == 1) {

            // 4. GOOGLE-A TƏSDİQ (ACKNOWLEDGE) GÖNDƏRMƏK
            // Bu mütləqdir, yoxsa pul geri qayıdır!
            if (purchase.getAcknowledgementState() == 0) {
                SubscriptionPurchasesAcknowledgeRequest acknowledgeRequest = new SubscriptionPurchasesAcknowledgeRequest();
                androidPublisher.purchases().subscriptions()
                        .acknowledge(packageName, productId, purchaseToken, acknowledgeRequest)
                        .execute();
                log.info("Google Play-ə abunəlik təsdiqi göndərildi.");
            }

            // 5. İSTİFADƏÇİ MƏLUMATLARINI YENİLƏMƏK
            LocalDateTime expiryDate = Instant.ofEpochMilli(purchase.getExpiryTimeMillis())
                    .atZone(ZoneId.of("Asia/Baku"))
                    .toLocalDateTime();

            user.setSubscriptionActive(true);
            user.setSubscriptionEndDate(expiryDate);
            userRepository.save(user);

            // 6. ÖDƏNİŞ TARİXÇƏSİ
            PaymentHistoryEntity history = PaymentHistoryEntity.builder()
                    .purchaseToken(purchaseToken)
                    .productId(productId)
                    .amount(new BigDecimal("0.50"))
                    .paymentDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
                    .user(user)
                    .build();

            paymentHistoryRepository.save(history);

            log.info("İstifadəçi {} üçün abunəlik {} tarixinə qədər aktiv edildi.", user.getId(), expiryDate);
        } else {
            throw new RuntimeException("Ödəniş hələ tamamlanmayıb və ya ləğv edilib. Status: " + purchase.getPaymentState());
        }
    }
}
//
//
//
//    @Transactional
//    public void verifyAndActivateSubscription(String purchaseToken, String productId, UserEntity user) throws Exception {
//
//        // 1. DUBLİKAT YOXLAMASI (Idempotency)
//        // Əgər bu purchaseToken artıq bizim bazada varsa, prosesi dərhal dayandırırıq.
//        if (paymentHistoryRepository.existsByPurchaseToken(purchaseToken)) {
//            log.info("Bu ödəniş tokeni artıq işlənilib: {}", purchaseToken);
//            return;
//        }
//
//        // 2. GOOGLE SERVERLƏRİNDƏN YOXLAMA
//        // Paket adı, məhsul ID-si və token ilə Google-a sorğu atırıq.
//        SubscriptionPurchase purchase = androidPublisher.purchases().subscriptions()
//                .get(packageName, productId, purchaseToken)
//                .execute();
//
//        // 3. STATUS YOXLAMASI (paymentState: 1 = Ödəniş uğurludur)
//        if (purchase.getPaymentState() != null && purchase.getPaymentState() == 1) {
//
//            // Google-dan gələn vaxtı (milliseconds) LocalDateTime formatına salırıq
//            LocalDateTime expiryDate = Instant.ofEpochMilli(purchase.getExpiryTimeMillis())
//                    .atZone(ZoneId.of("Asia/Baku"))
//                    .toLocalDateTime();
//
//            // 4. İSTİFADƏÇİ MƏLUMATLARINI YENİLƏYİRİK
//            user.setSubscriptionActive(true);
//            user.setSubscriptionEndDate(expiryDate);
//
//            if (purchase.getAcknowledgementState() == 0) {
//                androidPublisher.purchases().subscriptions()
//                        .acknowledge(packageName, productId, purchaseToken, null) // null = boş body
//                        .execute();
//                log.info("Google Play-ə abunəlik təsdiqi (Acknowledgement) göndərildi.");
//            }
//
//            // 5. İSTİFADƏÇİ MƏLUMATLARINI YENİLƏYİRİK
//            user.setSubscriptionActive(true);
//
//            userRepository.save(user);
//
//            // 5. ÖDƏNİŞİ TARİXÇƏYƏ YAZIRIQ (Təkrarın qarşısını almaq üçün unikal tokenlə)
//            PaymentHistoryEntity history = PaymentHistoryEntity.builder()
//                    .purchaseToken(purchaseToken)
//                    .productId(productId)
//                    .amount(new BigDecimal("0.50")) // Sabit abunə haqqı
//                    .paymentDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
//                    .user(user)
//                    .build();
//
//            paymentHistoryRepository.save(history);
//
//            log.info("İstifadəçi {} üçün abunəlik {} tarixinə qədər aktiv edildi.",
//                    user.getId(), expiryDate);
//        } else {
//            throw new RuntimeException("Ödəniş hələ tamamlanmayıb və ya ləğv edilib.");
//        }
//    }

