package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, Long> {


    List<DebtEntity> findByDueYearAndDueMonth(Integer dueYear, Integer dueMonth);


    List<DebtEntity> findByIsFlexibleDueDateTrueOrderByIdAsc();

    List<DebtEntity> findByDebtorNameContainingIgnoreCase(String debtorName);

    Optional<DebtEntity> findByDebtorNameIgnoreCase(String debtorName);
}