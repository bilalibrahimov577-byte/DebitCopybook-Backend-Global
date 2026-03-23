package com.example.DebitCopybook.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequestDto {

    @NotBlank(message = "Purchase Token boş ola bilməz")
    private String purchaseToken;

    @NotBlank(message = "Product ID boş ola bilməz")
    private String productId; // Məsələn: "monthly_limit_100"
}
