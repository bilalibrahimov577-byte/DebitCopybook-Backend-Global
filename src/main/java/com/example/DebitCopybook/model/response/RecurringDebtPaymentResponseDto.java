package com.example.DebitCopybook.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDebtPaymentResponseDto {
    private Long id;
    private BigDecimal amount;
    private BigDecimal remainingAfter;
    private LocalDateTime paidAt;
}