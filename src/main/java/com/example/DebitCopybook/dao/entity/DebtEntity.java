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
    private LocalDate createdAt;
    private Integer dueYear;
    private Integer dueMonth;
    private Boolean isFlexibleDueDate;
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DebtStatus status = DebtStatus.SIMPLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_user_id", nullable = true)
    private UserEntity counterpartyUser;

    @Column(name = "request_expiry_time", nullable = true)
    private LocalDateTime requestExpiryTime;





    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        if (isFlexibleDueDate == null) {
            isFlexibleDueDate = false;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;


}