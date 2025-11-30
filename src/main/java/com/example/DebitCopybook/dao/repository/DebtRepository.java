package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.model.enums.DebtStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, Long> {

    // --- ƏSAS SİYAHI ---
    List<DebtEntity> findAllByUserIdAndStatus(Long userId, DebtStatus status);

    // Köhnə (null statuslu) borcları da gətirən metod
    @Query("SELECT d FROM DebtEntity d WHERE d.user.id = :userId AND (d.status = :status OR d.status IS NULL)")
    List<DebtEntity> findPersonalDebtsIncludeNull(@Param("userId") Long userId, @Param("status") DebtStatus status);

    // --- ID İLƏ AXTARIŞ ---
    Optional<DebtEntity> findByIdAndUserId(Long id, Long userId);

    // =========================================================================
    // AŞAĞIDAKI METODLAR DƏYİŞDİRİLDİ (BUG HƏLLİ ÜÇÜN)
    // Artıq axtarış və filtrlər yalnız 'SIMPLE' (və ya NULL) borcları gətirəcək.
    // Qarşılıqlı borclar bura qarışmayacaq.
    // =========================================================================

    // 1. TARİXƏ GÖRƏ FİLTR (Yalnız Fərdi Borclar)
    @Query("SELECT d FROM DebtEntity d WHERE d.user.id = :userId AND d.dueYear = :year AND d.dueMonth = :month AND (d.status = 'SIMPLE' OR d.status IS NULL)")
    List<DebtEntity> findPersonalDebtsByDate(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    // 2. MÜDDƏTSİZ BORCLAR (Yalnız Fərdi Borclar)
    @Query("SELECT d FROM DebtEntity d WHERE d.user.id = :userId AND d.isFlexibleDueDate = true AND (d.status = 'SIMPLE' OR d.status IS NULL)")
    List<DebtEntity> findPersonalFlexibleDebts(@Param("userId") Long userId);

    // 3. ADA GÖRƏ AXTARIŞ (Yalnız Fərdi Borclar)
    @Query("SELECT d FROM DebtEntity d WHERE d.user.id = :userId AND LOWER(d.debtorName) LIKE LOWER(CONCAT('%', :name, '%')) AND (d.status = 'SIMPLE' OR d.status IS NULL)")
    List<DebtEntity> searchPersonalDebtsByName(@Param("userId") Long userId, @Param("name") String name);

    // 4. TƏSVİRƏ GÖRƏ (Mənim borcum / Mənə olan borc) - (Yalnız Fərdi Borclar)
    @Query("SELECT d FROM DebtEntity d WHERE d.user.id = :userId AND d.description = :desc AND (d.status = 'SIMPLE' OR d.status IS NULL)")
    List<DebtEntity> findPersonalDebtsByDescription(@Param("userId") Long userId, @Param("desc") String desc);

    // =========================================================================

    long countByUserId(Long userId);

    // --- QARŞILIQLI BORCLAR ---
    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.CONFIRMED AND (d.user.id = :userId OR d.counterpartyUser.id = :userId)")
    List<DebtEntity> findConfirmedSharedDebtsForUser(@Param("userId") Long userId);

    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.PENDING_APPROVAL AND d.counterpartyUser.id = :userId")
    List<DebtEntity> findPendingRequestsForUser(@Param("userId") Long userId);

    @Query("SELECT d FROM DebtEntity d WHERE d.status = com.example.DebitCopybook.model.enums.DebtStatus.PENDING_APPROVAL AND d.user.id = :userId")
    List<DebtEntity> findPendingRequestsSentByUser(@Param("userId") Long userId);

    @Query("SELECT d FROM DebtEntity d WHERE d.id = :id AND (d.user.id = :userId OR d.counterpartyUser.id = :userId)")
    Optional<DebtEntity> findSharedDebtForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT d FROM DebtEntity d WHERE d.user.id = :userId AND LOWER(d.debtorName) = LOWER(:name) AND (d.status = com.example.DebitCopybook.model.enums.DebtStatus.SIMPLE OR d.status IS NULL)")
    Optional<DebtEntity> findPersonalDebtByName(@Param("userId") Long userId, @Param("name") String name);


}