package com.example.DebitCopybook.dao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_debt_payments",
        indexes = @Index(name = "idx_recurring_payments_debt", columnList = "recurring_debt_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDebtPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recurring_debt_id", nullable = false)
    private Long recurringDebtId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Bu ödənişdən sonra qalan ümumi borc
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingAfter;

    @Column(nullable = false, updatable = false)
    private LocalDateTime paidAt;
}