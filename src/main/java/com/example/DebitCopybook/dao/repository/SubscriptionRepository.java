package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//@Repository
//public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
//    Optional<SubscriptionEntity> findByUserId(Long userId);
//    boolean existsByPurchaseToken(String purchaseToken);
//}


@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Optional<SubscriptionEntity> findByUserId(Long userId);

    // Scheduler üçün: Vaxtı bitmiş amma hələ də aktiv qalanları tapmaq üçün
    List<SubscriptionEntity> findAllByExpiryTimeBeforeAndIsActiveTrue(java.time.LocalDateTime dateTime);
}
