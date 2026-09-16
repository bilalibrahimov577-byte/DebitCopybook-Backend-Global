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
    private LocalDateTime createdAt;
    private Integer dueYear;
    private Integer dueMonth;
    private Boolean isFlexibleDueDate;
    private String notes;

    // --- BU SAHƏLƏR QARŞILIQLI BORC ÜÇÜN ƏLAVƏ OLUNUR ---

    // Borcun statusunu göstərir (məs: PENDING_APPROVAL, CONFIRMED, SIMPLE)
    private String status;

    // Sorğunun bitmə vaxtını göstərir (yalnız PENDING_APPROVAL statusunda mənası var)
    private LocalDateTime requestExpiryTime;

    // Borcu yaradan istifadəçinin məlumatları
    private UserDto user;

    // Borcun qarşı tərəfinin məlumatları
    private UserDto counterpartyUser;
}