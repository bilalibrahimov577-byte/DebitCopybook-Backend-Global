package com.example.DebitCopybook.model.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LegacyDebtResponseDto {
    private Long id;
    private String debtorName;
    private String description;
    private BigDecimal debtAmount;
    private String createdAt;
    private Integer dueYear;
    private Integer dueMonth;
    private boolean isFlexibleDueDate;
    private String notes;
    private Long userId;
}
