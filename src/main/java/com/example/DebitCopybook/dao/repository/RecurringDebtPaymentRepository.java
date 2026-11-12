package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.RecurringDebtPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecurringDebtPaymentRepository extends JpaRepository<RecurringDebtPaymentEntity, Long> {

    List<RecurringDebtPaymentEntity> findAllByRecurringDebtIdOrderByPaidAtDesc(Long recurringDebtId);

    @Modifying
    @Query("DELETE FROM RecurringDebtPaymentEntity p WHERE p.recurringDebtId = :debtId")
    void deleteAllByRecurringDebtId(@Param("debtId") Long debtId);


    @Modifying
    @Query("DELETE FROM RecurringDebtPaymentEntity p WHERE p.recurringDebtId IN " +
            "(SELECT r.id FROM RecurringDebtEntity r " +
            "WHERE r.status = com.example.DebitCopybook.model.enums.RecurringDebtStatus.COMPLETED " +
            "AND r.completedAt < :cutoff)")
    int deletePaymentsOfCompletedBefore(@Param("cutoff") LocalDateTime cutoff);
}