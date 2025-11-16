package com.example.DebitCopybook.controller;

import com.example.DebitCopybook.model.request.SharedDebtRequestDto;
import com.example.DebitCopybook.model.request.SharedDebtResponseRequestDto;
import com.example.DebitCopybook.model.request.UpdateProposalRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import com.example.DebitCopybook.service.SharedDebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/shared-debts") // URL-ləri ayırdıq
@RequiredArgsConstructor
@Tag(
        name = "Qarşılıqlı Borc Controller",
        description = "İstifadəçilər arasında qarşılıqlı borc sorğularının yaradılması və idarə edilməsi üçün son nöqtələr"
)
public class SharedDebtController {

    private final SharedDebtService sharedDebtService;

    @Operation(summary = "Yeni qarşılıqlı borc sorğusu yarat")
    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DebtResponseDto> createSharedDebtRequest(@Valid @RequestBody SharedDebtRequestDto requestDto) {
        DebtResponseDto createdRequest = sharedDebtService.createSharedDebtRequest(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

    // Gələcəkdə sorğuya cavab vermək, sorğuları listələmək kimi endpoint-lər bura əlavə olunacaq...

    @Operation(summary = "Qarşılıqlı borc sorğusuna cavab ver (qəbul/rədd)")
    @PostMapping("/{debtId}/respond")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DebtResponseDto> respondToSharedDebtRequest(
            @PathVariable Long debtId,
            @Valid @RequestBody SharedDebtResponseRequestDto responseDto) {

        DebtResponseDto result = sharedDebtService.respondToSharedDebtRequest(debtId, responseDto);
        return ResponseEntity.ok(result);
    }


    @Operation(summary = "Təsdiqlənmiş borc üçün dəyişiklik təklifi yarat")
    @PostMapping("/{debtId}/propose-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> createUpdateProposal(
            @PathVariable Long debtId,
            @Valid @RequestBody UpdateProposalRequestDto proposalDto) {

        sharedDebtService.createUpdateProposal(debtId, proposalDto);
        return ResponseEntity.ok().build(); // Uğurlu olduqda "200 OK" cavabı qaytarır
    }



    @Operation(summary = "Dəyişiklik təklifinə cavab ver (qəbul/rədd)")
    @PostMapping("/proposals/{proposalId}/respond")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DebtResponseDto> respondToUpdateProposal(
            @PathVariable Long proposalId,
            @Valid @RequestBody SharedDebtResponseRequestDto responseDto) {

        DebtResponseDto updatedDebt = sharedDebtService.respondToUpdateProposal(proposalId, responseDto);
        return ResponseEntity.ok(updatedDebt);
    }

    @Operation(summary = "Bütün təsdiqlənmiş qarşılıqlı borcları göstər")
    @GetMapping("/confirmed")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getConfirmedSharedDebts() {
        List<DebtResponseDto> debts = sharedDebtService.getConfirmedSharedDebts();
        return ResponseEntity.ok(debts);
    }

    @Operation(summary = "Mənə göndərilən və təsdiq gözləyən sorğuları göstər")
    @GetMapping("/requests/incoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getPendingRequestsForMe() {
        List<DebtResponseDto> requests = sharedDebtService.getPendingRequestsForMe();
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Mənim göndərdiyim və cavab gözləyən sorğuları göstər")
    @GetMapping("/requests/outgoing")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DebtResponseDto>> getPendingRequestsISent() {
        List<DebtResponseDto> requests = sharedDebtService.getPendingRequestsISent();
        return ResponseEntity.ok(requests);
    }

}