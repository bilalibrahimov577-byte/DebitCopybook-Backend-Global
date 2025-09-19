package com.example.DebitCopybook.model.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DebtHistoryResponseDto {
    private Long id;
    private String eventType;
    private String description;
    private BigDecimal amount;
    private LocalDateTime eventDate;
}