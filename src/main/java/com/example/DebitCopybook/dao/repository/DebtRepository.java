package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.model.enums.DebtStatus; // <-- BU IMPORT VACİBDİR
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, Long> {

    // Köhnə metod (bunu saxlayaq, bəlkə başqa yerdə lazımdır)
    List<DebtEntity> findAllByUserId(Long userId);

    // --- YENİ METOD (BU BİZƏ LAZIMDIR) ---
    // İstifadəçinin borclarını STATUSA görə gətirir.
    // Məsələn: ancaq SIMPLE (Fərdi) borcları gətir.
    List<DebtEntity> findAllByUserIdAndStatus(Long userId, DebtStatus status);
    // -------------------------------------

    Optional<DebtEntity> findByIdAndUserId(Long id, Long userId);
    List<DebtEntity> findByUserIdAndDueYearAndDueMonth(Long userId, Integer dueYear, Integer dueMonth);
    List<DebtEntity> findByUserIdAndIsFlexibleDueDateTrue(Long userId);
    List<DebtEntity> findByUserIdAndDebtorNameContainingIgnoreCase(Long userId, String debtorName);
    Optional<DebtEntity> findByUserIdAndDebtorNameIgnoreCase(Long userId, String debtorName);
    long countByUserId(Long userId);

    List<DebtEntity> findByDescriptionAndUserId(String description, Long userId);


    // --- QARŞILIQLI BORCLAR ÜÇÜN OLAN METODLAR ---

    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.CONFIRMED AND (d.user.id = :userId OR d.counterpartyUser.id = :userId)")
    List<DebtEntity> findConfirmedSharedDebtsForUser(@Param("userId") Long userId);

    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.PENDING_APPROVAL AND d.counterpartyUser.id = :userId")
    List<DebtEntity> findPendingRequestsForUser(@Param("userId") Long userId);

    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.PENDING_APPROVAL AND d.user.id = :userId")
    List<DebtEntity> findPendingRequestsSentByUser(@Param("userId") Long userId);

    @Query("SELECT d FROM DebtEntity d WHERE d.user.id = :userId AND (d.status = :status OR d.status IS NULL)")
    List<DebtEntity> findPersonalDebtsIncludeNull(@Param("userId") Long userId, @Param("status") DebtStatus status);

}