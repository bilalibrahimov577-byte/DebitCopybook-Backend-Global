package com.example.DebitCopybook.scheduler;

import com.example.DebitCopybook.dao.entity.SubscriptionEntity;
import com.example.DebitCopybook.dao.repository.SubscriptionRepository;
import com.example.DebitCopybook.service.GooglePlayService;
import com.google.api.services.androidpublisher.model.SubscriptionPurchase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final GooglePlayService googlePlayService;

   // @Scheduled(cron = "0 0 */6 * * ?", zone = "Asia/Baku")
   @Scheduled(fixedRate = 300000)
    @Transactional
    public void deactivateExpiredSubscriptions() {
        log.info("Abunəliklərin yoxlanması prosesi başladı...");

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Baku"));

        List<SubscriptionEntity> expiredSubs =
                subscriptionRepository.findAllByExpiryTimeBeforeAndIsActiveTrue(now);

        if (expiredSubs.isEmpty()) {
            log.info("Vaxtı bitmiş abunəlik tapılmadı.");
            return;
        }

        for (SubscriptionEntity sub : expiredSubs) {
            try {
                // Google-dan real statusu soruşuruq — kor-koranə false etmirik
                SubscriptionPurchase purchase = googlePlayService.getSubscriptionStatus(
                        sub.getSubscriptionId(), sub.getPurchaseToken());

                long newExpiryMillis = purchase.getExpiryTimeMillis();
                LocalDateTime newExpiry = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(newExpiryMillis), ZoneOffset.ofHours(4));

                if (newExpiry.isAfter(now)) {
                    // Google-da yenilənib, amma DB köhnə qalıb -> düzəlt
                    sub.setExpiryTime(newExpiry);
                    sub.setIsActive(true);
                    log.info("İstifadəçi ID: {} abunəliyi Google-da yenilənib, DB düzəldildi. Yeni vaxt: {}",
                            sub.getUser().getId(), newExpiry);
                } else {
                    // Google da təsdiqləyir ki, həqiqətən bitib/ləğv edilib
                    sub.setIsActive(false);
                    log.info("İstifadəçi ID: {} abunəliyi həqiqətən bitib. Passiv edildi.", sub.getUser().getId());
                }

            } catch (Exception e) {
                // Google-a sorğu alınmasa, ehtiyatlı davran — hazırda false etmə, sonra yenidən yoxlanacaq
                log.error("İstifadəçi ID: {} üçün Google Play statusu yoxlanılarkən xəta.", sub.getUser().getId(), e);
            }
        }

        subscriptionRepository.saveAll(expiredSubs);
        log.info("Yoxlama tamamlandı.");
    }

}









//    /**
//     * Hər gecə saat 03:00-da işləyir (Bakı vaxtı ilə).
//     * Cron: saniyə dəqiqə saat gün ay həftənin günü
//     */
//    //@Scheduled(cron = "0 50 12 * * ?", zone = "Asia/Baku")
//   // @Scheduled(fixedRate = 60000)
//    @Scheduled(cron = "0 0 */6 * * ?", zone = "Asia/Baku")
//    @Transactional
//    public void deactivateExpiredSubscriptions() {
//        log.info("Abunəliklərin təmizlənməsi prosesi başladı...");
//
//        // Bakı vaxtı ilə cari zamanı alırıq
//        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Baku"));
//
//        // Vaxtı bitmiş və hələ də aktiv olanları tapırıq
//        List<SubscriptionEntity> expiredSubs =
//                subscriptionRepository.findAllByExpiryTimeBeforeAndIsActiveTrue(now);
//
//        if (!expiredSubs.isEmpty()) {
//            for (SubscriptionEntity sub : expiredSubs) {
//                sub.setIsActive(false);
//                log.info("İstifadəçi ID: {} üçün abunəlik vaxtı bitdiyi üçün ləğv edildi. Bitmə vaxtı idi: {}",
//                        sub.getUser().getId(), sub.getExpiryTime());
//            }
//            // Hamısını birdən bazada yeniləyirik
//            subscriptionRepository.saveAll(expiredSubs);
//            log.info("Cəmi {} abunəlik passiv edildi.", expiredSubs.size());
//        } else {
//            log.info("Vaxtı bitmiş abunəlik tapılmadı.");
//        }
//    }
//}
