package com.example.DebitCopybook.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Google-un verdiyi unikal ödəniş kodu.
    // Eyni ödənişin təkrar işlənməməsi üçün bu sahə UNIQUE olmalıdır.
    @Column(name = "purchase_token", unique = true, nullable = false, length = 1000)
    private String purchaseToken;

    @Column(name = "product_id")
    private String productId; // Məsələn: "limit_100_monthly"

    @Column(name = "amount")
    private BigDecimal amount; // 0.50 AZN

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}

