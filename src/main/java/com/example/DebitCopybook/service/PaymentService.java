package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.example.DebitCopybook.model.request.PaymentRequestDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;

    @Transactional
    public void verifyAndActivateSubscription(Long userId, PaymentRequestDto request) {
        // 1. Google API vasitəsilə yoxlama (Bu hissə sadələşdirilmiş məntiqdir)
        // Realda bura Google Credentials (JSON key) ilə qoşulma yazılmalıdır.
        boolean isValid = verifyWithGooglePlay(request.getPurchaseToken(), request.getProductId());

        if (!isValid) {
            throw new IllegalStateException("Ödəniş təsdiqlənmədi!");
        }

        // 2. İstifadəçini tap və statusunu dəyiş
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));

        user.setSubscriptionActive(true);
        // Abunəliyi bu gündən etibarən 30 gün uzadırıq
        user.setSubscriptionEndDate(LocalDateTime.now(ZoneOffset.ofHours(4)).plusDays(30));

        userRepository.save(user);
    }

    private boolean verifyWithGooglePlay(String token, String productId) {
        // BURADA DİQQƏTLİ OL:
        // Test mərhələsində bura həmişə 'true' qaytara bilərsən ki, yoxlaya biləsən.
        // Amma production-da bura Google Publisher API sorğusu yazılmalıdır.
        return token != null && !token.isEmpty();
    }
}