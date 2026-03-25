package com.example.DebitCopybook.scheduler;

import com.example.DebitCopybook.dao.entity.SubscriptionEntity;
import com.example.DebitCopybook.dao.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Hər gecə saat 03:00-da işləyir (Bakı vaxtı ilə).
     * Cron: saniyə dəqiqə saat gün ay həftənin günü
     */
    //@Scheduled(cron = "0 50 12 * * ?", zone = "Asia/Baku")
   // @Scheduled(fixedRate = 60000)
    @Scheduled(cron = "0 0 */6 * * ?", zone = "Asia/Baku")
    @Transactional
    public void deactivateExpiredSubscriptions() {
        log.info("Abunəliklərin təmizlənməsi prosesi başladı...");

        // Bakı vaxtı ilə cari zamanı alırıq
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Baku"));

        // Vaxtı bitmiş və hələ də aktiv olanları tapırıq
        List<SubscriptionEntity> expiredSubs =
                subscriptionRepository.findAllByExpiryTimeBeforeAndIsActiveTrue(now);

        if (!expiredSubs.isEmpty()) {
            for (SubscriptionEntity sub : expiredSubs) {
                sub.setIsActive(false);
                log.info("İstifadəçi ID: {} üçün abunəlik vaxtı bitdiyi üçün ləğv edildi. Bitmə vaxtı idi: {}",
                        sub.getUser().getId(), sub.getExpiryTime());
            }
            // Hamısını birdən bazada yeniləyirik
            subscriptionRepository.saveAll(expiredSubs);
            log.info("Cəmi {} abunəlik passiv edildi.", expiredSubs.size());
        } else {
            log.info("Vaxtı bitmiş abunəlik tapılmadı.");
        }
    }
}
