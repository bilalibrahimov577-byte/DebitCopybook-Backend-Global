package com.example.DebitCopybook.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDebtResponseDto {
    private Long id;
    private String debtorName;
    private String direction;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal installmentAmount;
    private Integer dueDay;

    private LocalDate nextDueDate;        // növbəti (və ya gecikmiş) ödəniş tarixi, bitibsə null
    private BigDecimal amountDueNow;      // bu gün ödənməli olan məbləğ (gecikmə daxil)
    private BigDecimal overdueAmount;     // vaxtı keçmiş hissə
    private BigDecimal nextPaymentAmount; // ödənəcək növbəti məbləğ
    private String dueState;              // OK | DUE_TODAY | OVERDUE | COMPLETED
    private String status;                // ACTIVE | COMPLETED

    private String notes;
    private LocalDateTime createdAt;
}