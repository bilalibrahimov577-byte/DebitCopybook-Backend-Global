package com.example.DebitCopybook.dao.repository;
import com.example.DebitCopybook.dao.entity.DebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}