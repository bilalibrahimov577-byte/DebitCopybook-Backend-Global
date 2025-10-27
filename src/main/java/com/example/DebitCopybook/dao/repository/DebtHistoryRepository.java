package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.DebtHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtHistoryRepository extends JpaRepository<DebtHistoryEntity, Long> {


    List<DebtHistoryEntity> findAllByDebtIdOrderByEventDateDesc(Long debtId);

}