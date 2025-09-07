package com.example.DebitCopybook.model.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtRequestDto {

    @NotBlank(message = "borcalanın adı boş ola bilməz")
    @Schema(description = "Borcalanın adını daxil eidin məsələn Həsən müəllim")
    @Size(min =1, max = 25)
    private String debtorName;

    @Schema(description = "Borcalanla bağlı 500 simvoldan ibarət təsvir daxil edə bilərsiz " +
            "məsələn Həsən müəllim nəyə görə borcu yaranıbsa onu yazın")
    @Size(min =1, max = 500)
    private String description;
    @NotNull(message = "borcun məbləği boş ola bilməz təxmini də olsa məbləğ yazılmalıdır")
    private BigDecimal debtAmount;

    @Schema(description = "Əgər borcun qaytarılma ilini və ayını seçirsizsə pulum olanda hissəsi boş olmalıdır" +
            "il 2024 və 2060 arasında bir il olmalıdır")
    @Min(value = 2024, message = "il 2024 və 2060 arasında bir il olmalıdır")
    @Max(value = 2060, message = "il 2024 və 2060 arasında bir il olmalıdır")
    private Integer dueYear;

    @Schema(description = "Əgər borcun qaytarılma ilini və ayını seçirsizsə pulum olanda hissəsi boş olmalıdır" +
            "ay 1-ci və 12-ci arasında olmalıdır")
    @Min(value = 1, message = "ay 1-ci və 12-ci arasında olmalıdır")
    @Max(value = 12, message = "ay 1-ci və 12-ci arasında olmalıdır")
    private Integer dueMonth;

    @Schema(description = "Əgər borcun qaytarılmasını pulum olanda  seçirsizsə il və ay hissəsi boş olmalıdır")
    private Boolean isFlexibleDueDate;

    @Schema(description = "Əgər xüsusi bir qeyd etmək istəyirsizsə yazın məsələn oğlu Rusiyadan gəlib borcu ödəyəcək")
    private String notes;


    @AssertTrue(message = "Ya il və ay daxil edilməli, ya da pulum olanda seçilməlidir, ikisi də eyni anda ola bilməz.")
    private boolean isValidDueDateConfiguration() {
        if (isFlexibleDueDate != null && isFlexibleDueDate) {

            return dueYear == null && dueMonth == null;
        } else {

            return dueYear != null && dueMonth != null;
        }
    }



}
