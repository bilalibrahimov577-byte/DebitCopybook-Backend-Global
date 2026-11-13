package com.example.DebitCopybook.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    private UserEntity user;

    @Column(nullable = true)
    private String purchaseToken; // Google-dan gələn token

    @Column(nullable = true)
    private String subscriptionId; // Məsələn: monthly_50_qep

    @Column(nullable = true)
    private LocalDateTime expiryTime; // Abunəliyin bitmə vaxtı

    @Column(columnDefinition = "boolean default false")
    private Boolean isActive = false;
}