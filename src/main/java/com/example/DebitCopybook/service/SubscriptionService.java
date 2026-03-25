package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.SubscriptionEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.SubscriptionRepository;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.example.DebitCopybook.exception.InvalidRequestException;
import com.example.DebitCopybook.exception.UserNotFoundException;
import com.google.api.services.androidpublisher.model.SubscriptionPurchase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final GooglePlayService googlePlayService;

    public boolean hasActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(sub -> sub.getIsActive() && sub.getExpiryTime() != null &&
                        sub.getExpiryTime().isAfter(LocalDateTime.now(ZoneOffset.ofHours(4))))
                .orElse(false);
    }

    @Transactional
    public void activateOrUpdateSubscription(Long userId, String purchaseToken, String subscriptionId) {
        try {
            // 1. Google-dan ən son məlumatı soruşuruq
            SubscriptionPurchase purchaseInfo = googlePlayService.verifyAndAcknowledge(subscriptionId, purchaseToken);

            // 2. Google-un qaytardığı yeni bitmə vaxtını alırıq
            long expiryMillis = purchaseInfo.getExpiryTimeMillis();
            LocalDateTime expiryDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiryMillis), ZoneOffset.ofHours(4));

            // 3. İstifadəçini tapırıq
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı"));

            // 4. Əgər bu istifadəçinin artıq abunəlik rekordu varsa, onu yeniləyirik, yoxdursa yenisini yaradırıq
            SubscriptionEntity subscription = subscriptionRepository.findByUserId(userId)
                    .orElse(new SubscriptionEntity());

            // Əgər bazadakı vaxt Google-dan gələn vaxtla eynidirsə, əlavə iş görməyə ehtiyac yoxdur
            if (subscription.getExpiryTime() != null && subscription.getExpiryTime().equals(expiryDate)) {
                log.info("İstifadəçi {} üçün abunəlik onsuz da günceldir.", userId);
                return;
            }

            subscription.setUser(user);
            subscription.setPurchaseToken(purchaseToken);
            subscription.setSubscriptionId(subscriptionId);
            subscription.setExpiryTime(expiryDate);
            subscription.setIsActive(true);

            subscriptionRepository.save(subscription);
            log.info("İstifadəçi {} üçün abunəlik yeniləndi/aktiv edildi. Yeni bitmə vaxtı: {}", userId, expiryDate);

        } catch (Exception e) {
            log.error("Google Play ödənişi təsdiqlənərkən xəta baş verdi!", e);
            throw new InvalidRequestException("Ödəniş təsdiqlənmədi: " + e.getMessage());
        }
    }







//    @Transactional
//    public void activateOrUpdateSubscription(Long userId, String purchaseToken, String subscriptionId) {
//        // 1. TƏKRAR TOKEN YOXLAMASI: Eyni ödəniş 2-ci dəfə gəlibsə, qulaqardına vururuq (Heç nə etmirik)
//        if (subscriptionRepository.existsByPurchaseToken(purchaseToken)) {
//            log.info("Bu ödəniş tokeni artıq bazada var. İkinci dəfə gəlib. Token: {}", purchaseToken);
//            return;
//        }
//
//        try {
//            // 2. GOOGLE API İLƏ YOXLAMA VƏ TƏSDİQ (Refund-un qarşısını alırıq)
//            SubscriptionPurchase purchaseInfo = googlePlayService.verifyAndAcknowledge(subscriptionId, purchaseToken);
//
//            // 3. Google-un qaytardığı əsl bitmə vaxtını (Milli saniyə ilə gəlir) hesablayırıq
//            long expiryMillis = purchaseInfo.getExpiryTimeMillis();
//            LocalDateTime expiryDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiryMillis), ZoneOffset.ofHours(4));
//
//            // 4. Bazaya yazırıq
//            UserEntity user = userRepository.findById(userId)
//                    .orElseThrow(() -> new UserNotFoundException("İstifadəçi tapılmadı"));
//
//            SubscriptionEntity subscription = subscriptionRepository.findByUserId(userId)
//                    .orElse(new SubscriptionEntity());
//
//            subscription.setUser(user);
//            subscription.setPurchaseToken(purchaseToken);
//            subscription.setSubscriptionId(subscriptionId);
//            subscription.setExpiryTime(expiryDate); // Artıq manual +30 gün yox, Google-un dəqiq vaxtını veririk
//            subscription.setIsActive(true);
//
//            subscriptionRepository.save(subscription);
//            log.info("İstifadəçi {} üçün abunəlik aktiv edildi. Bitmə vaxtı: {}", userId, expiryDate);
//
//        } catch (Exception e) {
//            log.error("Google Play ödənişi təsdiqlənərkən xəta baş verdi!", e);
//            throw new InvalidRequestException("Ödəniş Google tərəfindən təsdiqlənmədi.");
//        }
//    }
}
