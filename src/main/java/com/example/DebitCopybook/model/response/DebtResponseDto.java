package com.example.DebitCopybook.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private Long userId; // BILAL, bu sahəni əlavə etmək MÜTLƏQDİR. Bunu etməsək tətbiq çoxistifadəçili rejimdə İŞLƏMƏYƏCƏK!
}