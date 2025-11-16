package com.example.DebitCopybook.model.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class SharedDebtRequestDto {

    // Borcalanın adı. Bu, istifadəçinin özü tərəfindən daxil ediləcək.
    private String debtorName;

    // Sorğu göndərilən istifadəçinin borc ID-si (məsələn, "55-45")
    private String counterpartyDebtId;

    // Borcun məbləği
    private BigDecimal debtAmount;

    // "Mənim borcum" yoxsa "Mənə olan borc" olduğunu bildirir
    private String description;

    // Əlavə qeydlər
    private String notes;

    // Bu sahələr sadə borc üçün idi, amma qarşılıqlı borcda da istifadə edilə bilər.
    // Frontend-dən gəlməsə də olar, null qalacaq.
    private Integer dueYear;
    private Integer dueMonth;
    private Boolean isFlexibleDueDate;
}