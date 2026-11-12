package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.RecurringDebtEntity;
import com.example.DebitCopybook.model.enums.RecurringDebtStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringDebtRepository extends JpaRepository<RecurringDebtEntity, Long> {

    List<RecurringDebtEntity> findAllByUserIdOrderByStatusAscCreatedAtDesc(Long userId);

    Optional<RecurringDebtEntity> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, RecurringDebtStatus status);

    // Ödəniş zamanı eyni anda iki sorğu gəlsə, ikincisi birinciyə qədər gözləyir
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecurringDebtEntity r WHERE r.id = :id AND r.userId = :userId")
    Optional<RecurringDebtEntity> findByIdAndUserIdForUpdate(@Param("id") Long id,
                                                             @Param("userId") Long userId);



    @Modifying
    @Query("DELETE FROM RecurringDebtEntity r " +
            "WHERE r.status = com.example.DebitCopybook.model.enums.RecurringDebtStatus.COMPLETED " +
            "AND r.completedAt < :cutoff")
    int deleteCompletedBefore(@Param("cutoff") LocalDateTime cutoff);

}