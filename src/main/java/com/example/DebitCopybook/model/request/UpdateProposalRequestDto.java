package com.example.DebitCopybook.model.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class UpdateProposalRequestDto {
    // Təklif edilən yeni məbləğ. Əgər məbləğ dəyişmirsə, frontend bunu null göndərə bilər.
    private BigDecimal proposedAmount;

    // Təklif edilən yeni qeydlər.
    private String proposedNotes;
}