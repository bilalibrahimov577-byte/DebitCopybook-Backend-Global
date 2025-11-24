package com.example.DebitCopybook.dao.entity;

import com.example.DebitCopybook.model.enums.DebtStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List; // List üçün import lazımdır

@Entity
@Table(name ="Debts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String debtorName;
    private String description;
    private BigDecimal debtAmount;
    @Column(nullable = false, updatable = false)

    private LocalDateTime createdAt;
    private Integer dueYear;
    private Integer dueMonth;
    private Boolean isFlexibleDueDate;
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    private DebtStatus status = DebtStatus.SIMPLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_user_id", nullable = true)
    private UserEntity counterpartyUser;

    @Column(name = "request_expiry_time", nullable = true)
    private LocalDateTime requestExpiryTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // --- YENİ ƏLAVƏLƏR (Silinmə xətası üçün) ---

    // Borc silinəndə tarixçəsi də silinsin
    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DebtHistoryEntity> history;

    // Borc silinəndə ona aid dəyişiklik təklifləri də silinsin
    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DebtUpdateProposalEntity> updateProposals;

    // ------------------------------------------

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isFlexibleDueDate == null) {
            isFlexibleDueDate = false;
        }
    }
}