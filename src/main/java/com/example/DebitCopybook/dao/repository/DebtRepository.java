package com.example.DebitCopybook.dao.repository;
import com.example.DebitCopybook.dao.entity.DebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, Long> {
    List<DebtEntity> findAllByUserId(Long userId);
    Optional<DebtEntity> findByIdAndUserId(Long id, Long userId);
    List<DebtEntity> findByUserIdAndDueYearAndDueMonth(Long userId, Integer dueYear, Integer dueMonth);
    List<DebtEntity> findByUserIdAndIsFlexibleDueDateTrue(Long userId);
    List<DebtEntity> findByUserIdAndDebtorNameContainingIgnoreCase(Long userId, String debtorName);
    Optional<DebtEntity> findByUserIdAndDebtorNameIgnoreCase(Long userId, String debtorName);
    long countByUserId(Long userId);

    List<DebtEntity> findByDescriptionAndUserId(String description, Long userId);



    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.CONFIRMED AND (d.user.id = :userId OR d.counterpartyUser.id = :userId)")
    List<DebtEntity> findConfirmedSharedDebtsForUser(@Param("userId") Long userId);

    // --> YENİ METOD 2: İstifadəçiyə gələn təsdiq sorğularını tapmaq üçün <--
    // Statusu PENDING_APPROVAL olan və cari istifadəçinin qarşı tərəf (counterpartyUser) olduğu borcları gətirir.
    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.PENDING_APPROVAL AND d.counterpartyUser.id = :userId")
    List<DebtEntity> findPendingRequestsForUser(@Param("userId") Long userId);

    // --> YENİ METOD 3: İstifadəçinin göndərdiyi gözləyən sorğuları tapmaq üçün <--
    // Statusu PENDING_APPROVAL olan və cari istifadəçinin sorğunu yaradan (user) olduğu borcları gətirir.
    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.PENDING_APPROVAL AND d.user.id = :userId")
    List<DebtEntity> findPendingRequestsSentByUser(@Param("userId") Long userId);


}