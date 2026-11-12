package com.example.DebitCopybook.controller;

import com.example.DebitCopybook.model.request.PaymentRequestDto;
import com.example.DebitCopybook.model.request.RecurringDebtRequestDto;
import com.example.DebitCopybook.model.response.RecurringDebtPaymentResponseDto;
import com.example.DebitCopybook.model.response.RecurringDebtResponseDto;
import com.example.DebitCopybook.service.RecurringDebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/recurring-debts")
@Tag(name = "Təkrarlanan Borc Controller", description = "Hissə-hissə aylıq ödənilən borclar")
public class RecurringDebtController {

    private final RecurringDebtService recurringDebtService;

    @Operation(summary = "Yeni təkrarlanan borc yarat")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<RecurringDebtResponseDto> create(@Valid @RequestBody RecurringDebtRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringDebtService.create(dto));
    }

    @Operation(summary = "Bütün təkrarlanan borcları göstər")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<RecurringDebtResponseDto>> getAll() {
        return ResponseEntity.ok(recurringDebtService.getAll());
    }

    @Operation(summary = "ID-yə görə təkrarlanan borcu göstər")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<RecurringDebtResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recurringDebtService.getById(id));
    }

    @Operation(summary = "Təkrarlanan borca ödəniş et")
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<RecurringDebtResponseDto> pay(@PathVariable Long id,
                                                        @Valid @RequestBody PaymentRequestDto request) {
        return ResponseEntity.ok(recurringDebtService.pay(id, request.getAmount()));
    }

    @Operation(summary = "Ödəniş tarixçəsi")
    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<RecurringDebtPaymentResponseDto>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(recurringDebtService.getPayments(id));
    }

    @Operation(summary = "Təkrarlanan borcu sil")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void delete(@PathVariable Long id) {
        recurringDebtService.delete(id);
    }
}