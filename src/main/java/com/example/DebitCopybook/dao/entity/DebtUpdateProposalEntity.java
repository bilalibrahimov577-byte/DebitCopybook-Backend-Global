package com.example.DebitCopybook.dao.entity;

import com.example.DebitCopybook.model.enums.ProposalStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "debt_update_proposals")
public class DebtUpdateProposalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hansı borca aid təklif olduğunu göstərir
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id", nullable = false)
    private DebtEntity debt;

    // Təklifi kimin göndərdiyini göstərir
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_user_id", nullable = false)
    private UserEntity proposerUser;

    // --- Təklif edilən yeni dəyərlər ---
    @Column(name = "proposed_amount")
    private BigDecimal proposedAmount;

    @Column(name = "proposed_notes")
    private String proposedNotes;
    // Gələcəkdə başqa sahələri dəyişmək üçün bura yeni "proposed_..." sahələri əlavə etmək olar

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status;

    @Column(name = "request_expiry_time", nullable = false)
    private LocalDateTime requestExpiryTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}