package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.DebtHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Bu interface-in bir Spring Repository olduğunu bildirir
public interface DebtHistoryRepository extends JpaRepository<DebtHistoryEntity, Long> {

    // Spring Data JPA bu metodun adına baxıb nə etməli olduğunu özü anlayır.
    // Metodun adı deyir:
    // "Find All" -> Bütün qeydləri tap
    // "ByDebtId" -> Amma yalnız "debt" sahəsinin "id"-si mənim verdiyim id-yə bərabər olanları
    // "OrderByEventDateDesc" -> Və onları "eventDate" sahəsinə görə azalan sıra ilə (ən yeni hadisə yuxarıda) sırala
    List<DebtHistoryEntity> findAllByDebtIdOrderByEventDateDesc(Long debtId);

}