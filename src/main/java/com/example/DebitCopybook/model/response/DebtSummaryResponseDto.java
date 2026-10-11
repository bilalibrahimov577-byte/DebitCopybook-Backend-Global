package com.example.DebitCopybook.model.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DebtSummaryResponseDto {
    private List<LegacyDebtResponseDto> myDebts;
    private BigDecimal myDebtsTotal;

    private List<LegacyDebtResponseDto> debtsToMe;
    private BigDecimal debtsToMeTotal;
}