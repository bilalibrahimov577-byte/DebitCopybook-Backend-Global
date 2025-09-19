package com.example.DebitCopybook.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequestDto {

    @NotNull(message = "Məbləğ boş ola bilməz")
    @Positive(message = "Məbləğ müsbət olmalıdır")
    private BigDecimal amount;
}
