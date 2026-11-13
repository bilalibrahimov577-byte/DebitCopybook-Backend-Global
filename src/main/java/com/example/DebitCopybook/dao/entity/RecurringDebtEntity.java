package com.example.DebitCopybook.dao.entity;

import com.example.DebitCopybook.model.enums.RecurringDebtStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_debts",
        indexes = @Index(name = "idx_recurring_debts_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDebtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Bilərəkdən @ManyToOne yox, sadə Long: UserEntity-yə toxunmuruq
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 65)
    private String debtorName;

    // "mənim borcum" / "mənə olan borclar" (DebtConstants ilə eyni)
    @Column(nullable = false)
    private String direction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal installmentAmount;

    @Column(nullable = false)
    private Integer dueDay;

    @Column(nullable = false)
    private LocalDate firstDueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringDebtStatus status;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}