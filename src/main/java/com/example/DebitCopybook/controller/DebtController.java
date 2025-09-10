package com.example.DebitCopybook.controller; // Paketi öz proyektinin adına uyğun dəyişdir

import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import com.example.DebitCopybook.service.DebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;



@Validated
//@SecurityRequirement(name = "X-API-KEY")
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/debts")
@Tag(
        name = "Borc Controller",
        description = "Borcların yaradılması, əldə edilməsi, yenilənməsi, ödənişi və silinməsi üçün son nöqtələr"
)
public class DebtController {
    private final DebtService debtService;

    @Operation(summary = "Yeni borc yarat")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DebtResponseDto> createDebt(
            @Valid @RequestBody DebtRequestDto debtRequestDto) {
        DebtResponseDto createdDebt =
                debtService.createDebt(debtRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDebt);
    }

    @Operation(summary = "ID-yə görə borcu tap")
    @GetMapping("/findDebtById/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DebtResponseDto getDebtById(@PathVariable("id") Long id) {
        return debtService.getDebtById(id);
    }


    @Operation(summary = "Bütün borcları göstər")
    @GetMapping("/findAllDebts")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getAllDebts() {
        List<DebtResponseDto> debts = debtService.getAllDebts();
        return ResponseEntity.ok(debts);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PatchMapping("/updateDebt/{id}")
    @Operation(summary = "Borcu barədə məlumatları dəyiş")
    @ResponseStatus(HttpStatus.OK)
    public DebtResponseDto updateDebt(@PathVariable Long id,
                                      @Valid @RequestBody DebtRequestDto debtRequestDto) {
        return debtService.updateDebt(id, debtRequestDto);
    }

    @Operation(summary = "Borca ödəniş et")
    @PatchMapping("/payDebt/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DebtResponseDto makePayment(@PathVariable Long id,
                                       @RequestParam BigDecimal amount) {
        return debtService.makePayment(id, amount);
    }

    @Operation(summary = "Borcu sil")
    @DeleteMapping("/deleteDebt/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void deleteDebt(@PathVariable Long id) {
        debtService.deleteDebt(id);
    }

    @Operation(summary = "İl və aya görə borcları tap")
    @GetMapping("/findByYearAndMonth")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getDebtsByYearAndMonth(
            @RequestParam @Min(value = 2024, message = "İl 2024 və 2060 arasında olmalıdır")
            @Max(value = 2060, message = "İl 2024 və 2060 arasında olmalıdır")
            Integer year,

            @RequestParam @Min(value = 1, message = "Ay 1 və 12 arasında olmalıdır")
            @Max(value = 12, message = "Ay 1 və 12 arasında olmalıdır")
            Integer month) {
        List<DebtResponseDto> debts = debtService.getDebtsByYearAndMonth(year, month);
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "'Pulum olanda' borclarını tap")
    @GetMapping("/findFlexibleDebts")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getFlexibleDueDateDebts() {
        List<DebtResponseDto> debts = debtService.getFlexibleDueDateDebts();
        return ResponseEntity.ok(debts);
    }


    @Operation(summary = "Borcalanın adına görə borcları axtar")
    @GetMapping("/searchByDebtorName")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> searchDebtsByDebtorName(
            @RequestParam String debtorName) {
        List<DebtResponseDto> debts = debtService.searchDebtsByDebtorName(debtorName);
        return ResponseEntity.ok(debts);
    }


    @Operation(summary = "Mövcud borcun məbləğini artır")
    @PatchMapping("/increaseDebt/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DebtResponseDto increaseDebt(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return debtService.increaseDebt(id, amount);
    }



}
