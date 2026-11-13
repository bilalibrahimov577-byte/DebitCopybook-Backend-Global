package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.DebtUpdateProposalEntity;
import com.example.DebitCopybook.model.enums.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtUpdateProposalRepository extends JpaRepository<DebtUpdateProposalEntity, Long> {

    @Query("SELECT p FROM DebtUpdateProposalEntity p " +
            "WHERE p.status = :status " +
            "AND p.proposerUser.id <> :userId " +
            "AND (p.debt.user.id = :userId OR p.debt.counterpartyUser.id = :userId)")
    List<DebtUpdateProposalEntity> findIncomingProposals(@Param("userId") Long userId, @Param("status") ProposalStatus status);

    // 2. Mənim göndərdiyim təkliflər (Statusu PENDING olan və Mənim yaratdıqlarım)
    @Query("SELECT p FROM DebtUpdateProposalEntity p " +
            "WHERE p.status = :status " +
            "AND p.proposerUser.id = :userId")
    List<DebtUpdateProposalEntity> findOutgoingProposals(@Param("userId") Long userId, @Param("status") ProposalStatus status);


    @Query("SELECT COUNT(p) FROM DebtUpdateProposalEntity p WHERE p.debt.id = :debtId AND p.proposerUser.id = :userId AND p.status = :status")
    long countPendingProposalsByDebtAndUser(@Param("debtId") Long debtId, @Param("userId") Long userId, @Param("status") ProposalStatus status);



}
