package com.example.DebitCopybook.dao.repository;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Query annotasiyasını artıq istifadə etmirik, ona görə çıxarıla bilər.

import java.util.List;
import java.util.Optional;

@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, Long> {

    // Bütün borcları cari istifadəçi ID-sinə görə tap
    // BILAL, bu metodu əlavə etməsək İŞLƏMƏYƏCƏK. Bu, hər bir istifadəçinin yalnız öz borclarını görməsini təmin edir.
    List<DebtEntity> findAllByUserId(Long userId);

    // Borcu ID və cari istifadəçi ID-sinə görə tap (yalnız istifadəçinin öz borcunu görməsi üçün)
    // BILAL, bu metodu da əlavə etməsək İŞLƏMƏYƏCƏK. Bu, bir borcun tapılması zamanı həmin borcun cari istifadəçiyə aid olduğunu yoxlayır.
    Optional<DebtEntity> findByIdAndUserId(Long id, Long userId);

    // İstifadəçi ID-sinə, il və aya görə borcları tap
    // BILAL, sizin `findByDueYearAndDueMonth` metodu bu ilə əvəz olunmalıdır, əks halda İŞLƏMƏYƏCƏK (hər kəsin borcunu göstərərdi).
    List<DebtEntity> findByUserIdAndDueYearAndDueMonth(Long userId, Integer dueYear, Integer dueMonth);

    // İstifadəçi ID-sinə görə 'Pulum olanda' borclarını tap
    // BILAL, sizin `findByIsFlexibleDueDateTrueOrderByIdAsc` metodu bu ilə əvəz olunmalıdır, əks halda İŞLƏMƏYƏCƏK.
    List<DebtEntity> findByUserIdAndIsFlexibleDueDateTrue(Long userId); // `OrderByIdAsc` burada lazımdırsa əlavə edə bilərik

    // İstifadəçi ID-sinə və borcalanın adına görə axtar
    // BILAL, sizin `findByDebtorNameContainingIgnoreCase` metodu bu ilə əvəz olunmalıdır, əks halda İŞLƏMƏYƏCƏK.
    List<DebtEntity> findByUserIdAndDebtorNameContainingIgnoreCase(Long userId, String debtorName);

    // Qeyd: findByDebtorNameIgnoreCase metodu əgər tətbiqdə hər bir istifadəçi üçün borcalan adının
    // unikal olduğunu fərz edirsinizsə və ya sadəcə 1 borc gətirmək üçün istifadə olunursa,
    // o zaman onu da findByUserIdAndDebtorNameIgnoreCase(Long userId, String debtorName) olaraq dəyişməlisiniz.
    // Əks halda, bu metodu istifadə etmək səhv nəticələr verə bilər.
    // Əgər sizə hələ də borcalanın adına görə tək borc tapmaq lazımdırsa, aşağıdakı kimi dəyişdirin:
    Optional<DebtEntity> findByUserIdAndDebtorNameIgnoreCase(Long userId, String debtorName);

    // Sizin əvvəlki @Query annotasiyasına ehtiyac qalmır, çünki JpaRepository method adlandırma konvensiyası ilə işimizi görürük.
    // Əgər spesifik bir sorğuya ehtiyacınız olarsa, onu da istifadəçi ID-sini nəzərə alaraq yeniləməlisiniz.

    long countByUserId(Long userId);
}