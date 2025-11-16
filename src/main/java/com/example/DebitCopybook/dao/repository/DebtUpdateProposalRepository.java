package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.DebtUpdateProposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtUpdateProposalRepository extends JpaRepository<DebtUpdateProposalEntity, Long> {
}
