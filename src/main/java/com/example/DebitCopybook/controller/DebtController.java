package com.example.DebitCopybook.controller;

// === YENİ: DTO və Servis importları ===
import com.example.DebitCopybook.model.request.PaymentRequestDto;
import com.example.DebitCopybook.model.response.DebtHistoryResponseDto;
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
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/debts") // Bütün endpoint-lərin başına "api/v1/debts" əlavə etdik
@Tag(
        name = "Borc Controller",
        description = "Borcların yaradılması, əldə edilməsi, yenilənməsi, ödənişi və silinməsi üçün son nöqtələr"
)
public class DebtController {
    private final DebtService debtService;

    @Operation(summary = "Yeni borc yarat")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DebtResponseDto> createDebt(@Valid @RequestBody DebtRequestDto debtRequestDto) {
        DebtResponseDto createdDebt = debtService.createDebt(debtRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDebt);
    }

    @Operation(summary = "ID-yə görə borcu tap")
    @GetMapping("/{id}") // Daha standart URL: /api/v1/debts/5
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DebtResponseDto getDebtById(@PathVariable("id") Long id) {
        return debtService.getDebtById(id);
    }

    @Operation(summary = "Bütün borcları göstər")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getAllDebts() {
        List<DebtResponseDto> debts = debtService.getAllDebts();
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "Borc barədə məlumatları dəyiş")
    @PutMapping("/{id}") // PUT daha standartdır (bütün obyekti yeniləyir)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DebtResponseDto updateDebt(@PathVariable Long id, @Valid @RequestBody DebtRequestDto debtRequestDto) {
        return debtService.updateDebt(id, debtRequestDto);
    }

    // === YENİ: Borca ödəniş etmək üçün standart endpoint ===
    @Operation(summary = "Borca ödəniş et")
    @PostMapping("/{id}/payments") // POST /api/v1/debts/5/payments
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DebtResponseDto makePayment(@PathVariable Long id, @Valid @RequestBody PaymentRequestDto paymentRequest) {
        return debtService.makePayment(id, paymentRequest.getAmount());
    }

    @Operation(summary = "Borcu sil")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void deleteDebt(@PathVariable Long id) {
        debtService.deleteDebt(id);
    }

    // === YENİ: Borcun tarixçəsini almaq üçün endpoint ===
    @Operation(summary = "ID-yə görə borcun tarixçəsini göstər")
    @GetMapping("/{id}/history") // GET /api/v1/debts/5/history
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtHistoryResponseDto>> getDebtHistory(@PathVariable Long id) {
        List<DebtHistoryResponseDto> history = debtService.getDebtHistory(id); // Bu metodu DebtService-də yaradacağıq
        return ResponseEntity.ok(history);
    }

    // Axtarış və filtr endpoint-ləri olduğu kimi qalır
    @Operation(summary = "İl və aya görə borcları tap")
    @GetMapping("/filter/by-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getDebtsByYearAndMonth(
            @RequestParam @Min(2024) @Max(2060) Integer year,
            @RequestParam @Min(1) @Max(12) Integer month) {
        List<DebtResponseDto> debts = debtService.getDebtsByYearAndMonth(year, month);
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "'Pulum olanda' borclarını tap")
    @GetMapping("/filter/flexible")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getFlexibleDueDateDebts() {
        List<DebtResponseDto> debts = debtService.getFlexibleDueDateDebts();
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "Borcalanın adına görə borcları axtar")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> searchDebtsByDebtorName(@RequestParam String name) {
        List<DebtResponseDto> debts = debtService.searchDebtsByDebtorName(name);
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "Mövcud borcun məbləğini artır")
    @PostMapping("/{id}/increase") // POST daha məntiqlidir, çünki yeni bir dəyər əlavə edir
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DebtResponseDto increaseDebt(@PathVariable Long id, @Valid @RequestBody PaymentRequestDto increaseRequest) {
        return debtService.increaseDebt(id, increaseRequest.getAmount());
    }
}