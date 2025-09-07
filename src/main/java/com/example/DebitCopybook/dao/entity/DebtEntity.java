package com.example.DebitCopybook.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        // Əgər isFlexibleDueDate null gəlsə, default olaraq false təyin edək
        if (isFlexibleDueDate == null) {
            isFlexibleDueDate = false;
        }
    }
}