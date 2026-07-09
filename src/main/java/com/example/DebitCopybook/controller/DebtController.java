package com.example.DebitCopybook.controller;

import com.example.DebitCopybook.model.request.PaymentRequestDto;
import com.example.DebitCopybook.model.response.DebtHistoryResponseDto;
import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import com.example.DebitCopybook.model.response.LegacyDebtResponseDto; // DƏYİŞİKLİK
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

import static com.example.DebitCopybook.constants.DebtConstants.DEBT_TO_ME_DESCRIPTION;
import static com.example.DebitCopybook.constants.DebtConstants.MY_DEBT_DESCRIPTION;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/debts")
@Tag(name = "Borc Controller", description = "Sadə borcların idarə edilməsi üçün son nöqtələr")
public class DebtController {
    private final DebtService debtService;

    @Operation(summary = "Yeni borc yarat")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LegacyDebtResponseDto> createDebt(@Valid @RequestBody DebtRequestDto debtRequestDto) {
        LegacyDebtResponseDto createdDebt = debtService.createDebt(debtRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDebt);
    }

    @Operation(summary = "ID-yə görə borcu tap")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LegacyDebtResponseDto> getDebtById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(debtService.getDebtById(id));
    }

    @Operation(summary = "Bütün borcları göstər")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LegacyDebtResponseDto>> getAllDebts() {
        List<LegacyDebtResponseDto> debts = debtService.getAllDebts();
        return ResponseEntity.ok(debts);
    }

//   @Operation(summary = "Borc barədə məlumatları dəyiş")
//    @PutMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
//    public ResponseEntity<LegacyDebtResponseDto> updateDebt(@PathVariable Long id, @Valid @RequestBody DebtRequestDto debtRequestDto) {
//       return ResponseEntity.ok(debtService.updateDebt(id, debtRequestDto));
//   }

    @Operation(summary = "Borc barədə məlumatları dəyiş")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DebtResponseDto> updateDebt(@PathVariable Long id, @Valid @RequestBody DebtRequestDto debtRequestDto) {
        return ResponseEntity.ok(debtService.updateDebt(id, debtRequestDto));
    }




    @Operation(summary = "Borca ödəniş et")
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LegacyDebtResponseDto> makePayment(@PathVariable Long id, @Valid @RequestBody PaymentRequestDto paymentRequest) {
        LegacyDebtResponseDto result = debtService.makePayment(id, paymentRequest.getAmount());
        // Əgər borc tam ödənilibsə, servis null qaytarır, biz isə boş cavab (200 OK) göndəririk.
        if (result == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Borcu sil")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void deleteDebt(@PathVariable Long id) {
        debtService.deleteDebt(id);
    }

    @Operation(summary = "ID-yə görə borcun tarixçəsini göstər")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtHistoryResponseDto>> getDebtHistory(@PathVariable Long id) {
        List<DebtHistoryResponseDto> history = debtService.getDebtHistory(id);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "İl və aya görə borcları tap")
    @GetMapping("/filter/by-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LegacyDebtResponseDto>> getDebtsByYearAndMonth(
            @RequestParam @Min(2024) @Max(2060) Integer year,
            @RequestParam @Min(1) @Max(12) Integer month) {
        List<LegacyDebtResponseDto> debts = debtService.getDebtsByYearAndMonth(year, month);
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "'Pulum olanda' borclarını tap")
    @GetMapping("/filter/flexible")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LegacyDebtResponseDto>> getFlexibleDueDateDebts() {
        List<LegacyDebtResponseDto> debts = debtService.getFlexibleDueDateDebts();
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "Borcalanın adına görə borcları axtar")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LegacyDebtResponseDto>> searchDebtsByDebtorName(@RequestParam String name) {
        List<LegacyDebtResponseDto> debts = debtService.searchDebtsByDebtorName(name);
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "Mövcud borcun məbləğini artır")
    @PostMapping("/{id}/increase")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LegacyDebtResponseDto> increaseDebt(@PathVariable Long id, @Valid @RequestBody PaymentRequestDto increaseRequest) {
        return ResponseEntity.ok(debtService.increaseDebt(id, increaseRequest.getAmount()));
    }

    @Operation(summary = "Bütün 'mənim borcum' tipli borcları göstər")
    @GetMapping("/my-debts")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LegacyDebtResponseDto>> getMyDebts() {
        List<LegacyDebtResponseDto> debts = debtService.getDebtsByDescription(MY_DEBT_DESCRIPTION);
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "Bütün 'mənə olan borclar' tipli borcları göstər")
    @GetMapping("/debts-to-me")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LegacyDebtResponseDto>> getDebtsToMe() {
        List<LegacyDebtResponseDto> debts = debtService.getDebtsByDescription(DEBT_TO_ME_DESCRIPTION);
        return ResponseEntity.ok(debts);
    }
}