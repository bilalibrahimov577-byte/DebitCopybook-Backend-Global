package com.example.DebitCopybook.security;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("debtSecurity")
@RequiredArgsConstructor
public class DebtSecurity {

    private final DebtRepository debtRepository;

    public boolean isOwner(Long debtId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserEntity)) {
            return false;
        }

        UserEntity currentUser = (UserEntity) authentication.getPrincipal();
        Long currentUserId = currentUser.getId();

        Optional<DebtEntity> debt = debtRepository.findById(debtId);


        return debt.isPresent() && debt.get().getUser().getId().equals(currentUserId);
    }
}