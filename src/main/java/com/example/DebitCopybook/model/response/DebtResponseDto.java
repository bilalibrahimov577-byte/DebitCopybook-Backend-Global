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
public class DebtResponseDto {
    private Long id;
    private String debtorName;
    private String description;
    private BigDecimal debtAmount;
    private LocalDate createdAt;
    private Integer dueYear;
    private Integer dueMonth;
    private Boolean isFlexibleDueDate;
    private String notes;

    // --- YENİ SAHƏLƏR ---
    private String status;
    private LocalDateTime requestExpiryTime;

    // `userId` yerinə tam DTO obyektlərini istifadə edirik
    private UserDto user;
    private UserDto counterpartyUser;
}