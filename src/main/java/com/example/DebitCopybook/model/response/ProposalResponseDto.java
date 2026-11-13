package com.example.DebitCopybook.model.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProposalResponseDto {
    private Long id;                // Təklifin ID-si (Respond etmək üçün lazımdır)
    private Long debtId;            // Hansı borca aiddir
    private String proposerName;    // Təklifi kim göndərib

    private BigDecimal originalAmount; // Köhnə məbləğ (müqayisə üçün)
    private BigDecimal proposedAmount; // Yeni təklif olunan məbləğ

    private String originalNotes;      // Köhnə qeyd
    private String proposedNotes;      // Yeni qeyd

    private LocalDateTime requestExpiryTime;
}