package com.example.DebitCopybook.model.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDebtRequestDto {

    @NotBlank(message = "borcalanın adı boş ola bilməz")
    @Size(min = 1, max = 65, message = "ad 1-65 simvol olmalıdır")
    private String debtorName;

    @NotBlank(message = "borcun növü boş ola bilməz")
    private String direction; // "mənim borcum" və ya "mənə olan borclar"

    @NotNull(message = "ümumi məbləğ boş ola bilməz")
    @Positive(message = "ümumi məbləğ müsbət olmalıdır")
    @Digits(integer = 9, fraction = 2, message = "məbləğ yanlış formatdadır")
    private BigDecimal totalAmount;

    @NotNull(message = "hissə məbləği boş ola bilməz")
    @Positive(message = "hissə məbləği müsbət olmalıdır")
    @Digits(integer = 9, fraction = 2, message = "məbləğ yanlış formatdadır")
    private BigDecimal installmentAmount;

    @NotNull(message = "ödəniş günü boş ola bilməz")
    @Min(value = 1, message = "gün 1 ilə 28 arasında olmalıdır")
    @Max(value = 28, message = "gün 1 ilə 28 arasında olmalıdır")
    private Integer dueDay;

    @Size(max = 500)
    private String notes;
}