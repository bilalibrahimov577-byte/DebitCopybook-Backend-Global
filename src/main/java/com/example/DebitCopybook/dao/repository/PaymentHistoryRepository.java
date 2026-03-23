package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.PaymentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistoryEntity, Long> {

    // Bu metod bizə eyni ödənişi bloklamaq üçün lazım olacaq
    boolean existsByPurchaseToken(String purchaseToken);
}