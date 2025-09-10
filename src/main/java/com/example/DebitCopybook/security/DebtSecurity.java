package com.example.DebitCopybook.security; // Security paketinə yerləşdirə bilərsən

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("debtSecurity") // BILAL, bu bean adı @PreAuthorize ifadəsində istifadə ediləcək
@RequiredArgsConstructor
public class DebtSecurity {

    private final DebtRepository debtRepository;

    // BILAL, bu metod cari daxil olmuş istifadəçinin müəyyən bir borcun sahibi olub-olmadığını yoxlayır.
    // Bunu əlavə etməsək, @PreAuthorize ifadəsi işləməyəcək və ya düzgün təhlükəsizlik təmin olunmayacaq.
    public boolean isOwner(Long debtId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserEntity)) {
            return false; // İstifadəçi daxil olmayıb və ya UserEntity deyil
        }

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        Long currentUserId = currentUser.getId();

        Optional<DebtEntity> debt = debtRepository.findById(debtId); // Borcu ID-yə görə tapırıq

        // Əgər borc tapılıbsa və onun istifadəçi ID-si cari istifadəçinin ID-si ilə eynidirsə
        return debt.isPresent() && debt.get().getUser().getId().equals(currentUserId);
    }
}