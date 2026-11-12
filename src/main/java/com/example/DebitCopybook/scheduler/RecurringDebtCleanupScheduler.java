package com.example.DebitCopybook.scheduler;

import com.example.DebitCopybook.dao.repository.RecurringDebtPaymentRepository;
import com.example.DebitCopybook.dao.repository.RecurringDebtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringDebtCleanupScheduler {

    private static final ZoneId BAKU_ZONE = ZoneId.of("Asia/Baku");
    private static final int RETENTION_DAYS = 30;

    private final RecurringDebtRepository debtRepository;
    private final RecurringDebtPaymentRepository paymentRepository;

    // Hər gecə 03:30 (Bakı vaxtı)
    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Baku")
    @Transactional
    public void deleteOldCompletedDebts() {
        LocalDateTime cutoff = LocalDateTime.now(BAKU_ZONE).minusDays(RETENTION_DAYS);
        int payments = paymentRepository.deletePaymentsOfCompletedBefore(cutoff); // əvvəl ödənişlər
        int debts = debtRepository.deleteCompletedBefore(cutoff);                 // sonra borclar
        if (debts > 0) {
            log.info("Təmizləmə: {} tamamlanmış təkrarlanan borc, {} ödəniş silindi", debts, payments);
        }
    }
}