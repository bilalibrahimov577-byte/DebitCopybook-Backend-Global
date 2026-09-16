package com.example.DebitCopybook.controller;

import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.service.SubscriptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/verify")
    public ResponseEntity<String> verifySubscription(@RequestBody SubscriptionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = ((UserEntity) auth.getPrincipal()).getId();

        subscriptionService.activateOrUpdateSubscription(userId, request.getPurchaseToken(), request.getSubscriptionId());
        return ResponseEntity.ok("Abunəlik uğurla aktivləşdirildi.");
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> getSubscriptionStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = ((UserEntity) auth.getPrincipal()).getId();

        return ResponseEntity.ok(subscriptionService.hasActiveSubscription(userId));
    }
}

@Data
class SubscriptionRequest {
    private String purchaseToken;
    private String subscriptionId;
}
