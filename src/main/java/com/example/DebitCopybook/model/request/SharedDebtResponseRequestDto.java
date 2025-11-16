package com.example.DebitCopybook.model.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SharedDebtResponseRequestDto {
    // true - sorğu qəbul edildi
    // false - sorğu rədd edildi
    private boolean accepted;
}