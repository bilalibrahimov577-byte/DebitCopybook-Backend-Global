package com.example.DebitCopybook.controller;

import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.model.request.PaymentRequestDto;
import com.example.DebitCopybook.service.GoogleBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final GoogleBillingService googleBillingService;

    @PostMapping("/verify-subscription")
    public ResponseEntity<String> verifySubscription(
            @RequestBody PaymentRequestDto requestDto, // JSON body-ni bura qəbul edirik
            @AuthenticationPrincipal UserEntity currentUser) {

        try {
            // DTO-dan məlumatları çəkib servisə ötürürük
            googleBillingService.verifyAndActivateSubscription(
                    requestDto.getPurchaseToken(),
                    requestDto.getProductId(),
                    currentUser
            );
            return ResponseEntity.ok("Abunəlik uğurla aktiv edildi!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Xəta baş verdi: " + e.getMessage());
        }
    }
}
