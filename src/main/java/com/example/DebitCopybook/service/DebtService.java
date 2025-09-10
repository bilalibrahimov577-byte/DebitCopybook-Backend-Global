package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.entity.UserEntity; // Yeni: UserEntity-ni import etdik
import com.example.DebitCopybook.dao.repository.DebtRepository;
import com.example.DebitCopybook.dao.repository.UserRepository; // Yeni: UserRepository-ni import etdik
import com.example.DebitCopybook.exception.DebtNotFoundException; // Sizin istifadə etdiyiniz xəta sinifi
import com.example.DebitCopybook.model.mapper.DebtMapper;
import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication; // Yeni: Spring Security üçün
import org.springframework.security.core.context.SecurityContextHolder; // Yeni: Spring Security üçün
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtMapper debtMapper;
    private final UserRepository userRepository; // Yeni: UserRepository-ni inject etdik


    // BILAL, bu metod ÇOX VACİBDİR. Bunu əlavə etməsək İŞLƏMƏYƏCƏK.
    // Bu metod cari daxil olmuş istifadəçinin ID-sini Spring Security Context-dən götürür.
    // Artıq bütün borc əməliyyatları bu istifadəçi ID-sinə görə filtrlənəcək.
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // Əgər istifadəçi identifikasiya olunmayıbsa, xəta atırıq. Bu vəziyyət əslində
            // Spring Security konfiqurasiyası düzgün qurulubsa, icazə verilməyən endpointlər üçün baş verməməlidir.
            throw new IllegalStateException("Cari istifadəçi identifikasiya olunmayıb.");
        }

        // SecurityConfiguration-da biz UserEntity obyektini Authentication obyekti kimi saxlayacağımız üçün,
        // burada onu UserEntity-ə cast edə bilirik.
        // Əgər siz CustomUserDetails implementasiyası edirsinizsə, o zaman CustomUserDetails-ə cast etməlisiniz
        // və User ID-sini həmin obyektdən almalısınız.
        if (authentication.getPrincipal() instanceof UserEntity) { // Sizin UserEntity adı
            UserEntity currentUser = (UserEntity) authentication.getPrincipal();
            return currentUser.getId();
        }

        // Əgər principal UserEntity tipində deyilsə, bu proqram məntiqində səhvdir.
        throw new IllegalStateException("Cari istifadəçi məlumatları tapılmadı və ya gözlənilən formatda deyil.");
    }

    // --- Metodların yenilənmiş versiyaları başlayır ---

    @Transactional
    public DebtResponseDto createDebt(DebtRequestDto requestDto) {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Çünki artıq borclar bir istifadəçiyə bağlanmalıdır.

        Long userId = getCurrentUserId(); // Cari istifadəçinin ID-sini alırıq
        UserEntity currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new DebtNotFoundException("İstifadəçi tapılmadı ID: " + userId)); // UserEntity-ni tapırıq

        String trimmedName = requestDto.getDebtorName().trim();

        // Borcalanın adı unikal olmalıdır, lakin HƏR İSTİFADƏÇİ ÜÇÜN unikal olmalıdır.
        // Yəni, bir istifadəçi üçün "Əli" adlı borcalan varsa, başqa bir istifadəçi üçün də "Əli" ola bilər.
        // BILAL, bu xətt dəyişməsək İŞLƏMƏYƏCƏK.
        Optional<DebtEntity> existingDebt = debtRepository.findByUserIdAndDebtorNameIgnoreCase(userId, trimmedName);

        if (existingDebt.isPresent()) {
            throw new IllegalArgumentException("'" + trimmedName + "' adlı borcalan artıq bu siyahıda mövcuddur. Zəhmət olmasa yeni borc əlavə etmək üçün 'Borcu Artır' funksiyasından istifadə edin.");
        }

        if (requestDto.getDebtAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Borc məbləği 0 manatdan çox olmalıdır.");
        }

        if (requestDto.getIsFlexibleDueDate() != null && requestDto.getIsFlexibleDueDate()) {
            requestDto.setDueYear(null);
            requestDto.setDueMonth(null);
        }

        requestDto.setDebtorName(trimmedName); // Trim edilmiş adı DTO-ya qaytarırıq

        DebtEntity debtEntity = debtMapper.mapRequestDtoToEntity(requestDto);
        debtEntity.setUser(currentUser); // BILAL, bu xətt ÇOX KRİTİKDİR. Borcu cari istifadəçiyə bağlayırıq.
        // Bunu etməsək borclar istifadəçiyə bağlanmayacaq və sistem İŞLƏMƏYƏCƏK.
        DebtEntity savedEntity = debtRepository.save(debtEntity);
        return debtMapper.mapEntityToResponseDto(savedEntity);
    }

    public List<DebtResponseDto> getAllDebts() {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Hər kəsin borclarını deyil, yalnız cari istifadəçinin borclarını gətirməlidir.
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findAllByUserId(userId); // Yeni repository metodundan istifadə edirik
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    public DebtResponseDto getDebtById(Long id) {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Təhlükəsizlik üçün borcun cari istifadəçiyə aid olduğunu yoxlamalıyıq.
        Long userId = getCurrentUserId();
        DebtEntity debtEntity = debtRepository.findByIdAndUserId(id, userId) // Yeni repository metodundan istifadə edirik
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));
        return debtMapper.mapEntityToResponseDto(debtEntity);
    }

    @Transactional
    public DebtResponseDto makePayment(Long id, BigDecimal paymentAmount) {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Təhlükəsizlik üçün borcun cari istifadəçiyə aid olduğunu yoxlamalıyıq.
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ödəniş məbləği müsbət olmalıdır.");
        }

        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId) // Yeni repository metodundan istifadə edirik
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        BigDecimal currentDebt = existingEntity.getDebtAmount();
        BigDecimal newDebt = currentDebt.subtract(paymentAmount);

        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
            debtRepository.delete(existingEntity);
            return DebtResponseDto.builder()
                    .id(id)
                    .debtorName(existingEntity.getDebtorName())
                    .description(existingEntity.getDescription())
                    .debtAmount(BigDecimal.ZERO) // DTO-da 'amount' olaraq əvəz etdik
                    .createdAt(existingEntity.getCreatedAt())
                    .dueYear(existingEntity.getDueYear())
                    .dueMonth(existingEntity.getDueMonth())
                    .isFlexibleDueDate(existingEntity.getIsFlexibleDueDate())
                    .notes("Borc tam ödənildi və silindi.")
                    .userId(userId) // BILAL, DTO-ya userId əlavə edilməlidir
                    .build();
        } else {
            existingEntity.setDebtAmount(newDebt);
            DebtEntity updatedEntity = debtRepository.save(existingEntity);
            return debtMapper.mapEntityToResponseDto(updatedEntity);
        }
    }

    @Transactional
    public void deleteDebt(Long id) {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Təhlükəsizlik üçün borcun cari istifadəçiyə aid olduğunu yoxlamalıyıq.
        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId) // Yeni repository metodundan istifadə edirik
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));
        debtRepository.delete(existingEntity);
    }

    public List<DebtResponseDto> getDebtsByYearAndMonth(Integer year, Integer month) {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Hər kəsin borclarını deyil, yalnız cari istifadəçinin borclarını gətirməlidir.
        if (year == null || month == null) {
            throw new IllegalArgumentException("Borcları il və aya görə axtarmaq üçün hər ikisi qeyd olunmalıdır.");
        }
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndDueYearAndDueMonth(userId, year, month); // Yeni repository metodundan istifadə edirik
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    public List<DebtResponseDto> getFlexibleDueDateDebts() {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Hər kəsin borclarını deyil, yalnız cari istifadəçinin borclarını gətirməlidir.
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndIsFlexibleDueDateTrue(userId); // Yeni repository metodundan istifadə edirik
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    public List<DebtResponseDto> searchDebtsByDebtorName(String debtorName) {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Hər kəsin borclarını deyil, yalnız cari istifadəçinin borclarını gətirməlidir.
        if (debtorName == null || debtorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Axtarış üçün borcalanın adı boş ola bilməz.");
        }
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndDebtorNameContainingIgnoreCase(userId, debtorName); // Yeni repository metodundan istifadə edirik
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    @Transactional
    public DebtResponseDto updateDebt(Long id, DebtRequestDto requestDto) {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Təhlükəsizlik üçün borcun cari istifadəçiyə aid olduğunu yoxlamalıyıq.
        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId) // Yeni repository metodundan istifadə edirik
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        if (requestDto.getDebtorName() != null && !requestDto.getDebtorName().isBlank()) {
            String trimmedName = requestDto.getDebtorName().trim();

            // Borcalan adının yoxlanılması da istifadəçiyə aid olmalıdır
            // BILAL, bu xətt dəyişməsək İŞLƏMƏYƏCƏK.
            Optional<DebtEntity> anotherDebtWithSameName = debtRepository.findByUserIdAndDebtorNameIgnoreCase(userId, trimmedName);

            // Əgər eyni adla başqa bir borc varsa və bu, cari borc deyil (ID-ləri fərqlidirsə)
            if (anotherDebtWithSameName.isPresent() && !anotherDebtWithSameName.get().getId().equals(id)) {
                throw new IllegalArgumentException("'" + trimmedName + "' adlı borcalan artıq mövcuddur. Onun adını siyahıdan tapıb borcu artıra bilərsiz");
            }

            existingEntity.setDebtorName(trimmedName);
        }

        if (requestDto.getDebtAmount() != null) {
            if (requestDto.getDebtAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Borc məbləği mənfi ola bilməz.");
            }
            existingEntity.setDebtAmount(requestDto.getDebtAmount());
        }

        if (requestDto.getDescription() != null) {
            existingEntity.setDescription(requestDto.getDescription());
        }
        if (requestDto.getNotes() != null) {
            existingEntity.setNotes(requestDto.getNotes());
        }

        if (requestDto.getIsFlexibleDueDate() != null) {
            if (requestDto.getIsFlexibleDueDate()) {
                existingEntity.setIsFlexibleDueDate(true);
                existingEntity.setDueYear(null);
                existingEntity.setDueMonth(null);
            } else {
                if (requestDto.getDueYear() == null || requestDto.getDueMonth() == null) {
                    throw new IllegalArgumentException("Konkret tarixə keçmək üçün il və ay qeyd olunmalıdır.");
                }
                existingEntity.setIsFlexibleDueDate(false);
                existingEntity.setDueYear(requestDto.getDueYear());
                existingEntity.setDueMonth(requestDto.getDueMonth());
            }
        } else {
            if (requestDto.getDueYear() != null) {
                existingEntity.setDueYear(requestDto.getDueYear());
            }
            if (requestDto.getDueMonth() != null) {
                existingEntity.setDueMonth(requestDto.getDueMonth());
            }
        }

        DebtEntity updatedEntity = debtRepository.save(existingEntity);
        return debtMapper.mapEntityToResponseDto(updatedEntity);
    }

    @Transactional
    public DebtResponseDto increaseDebt(Long id, BigDecimal amountToAdd) {
        // BILAL, bu metod tamamilə dəyişdirilməlidir. Köhnə şəkildə İŞLƏMƏYƏCƏK.
        // Təhlükəsizlik üçün borcun cari istifadəçiyə aid olduğunu yoxlamalıyıq.
        if (amountToAdd == null || amountToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Əlavə olunacaq məbləğ müsbət olmalıdır.");
        }

        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId) // Yeni repository metodundan istifadə edirik
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        BigDecimal currentDebt = existingEntity.getDebtAmount();
        BigDecimal newDebtAmount = currentDebt.add(amountToAdd);
        existingEntity.setDebtAmount(newDebtAmount);

        DebtEntity updatedEntity = debtRepository.save(existingEntity);

        return debtMapper.mapEntityToResponseDto(updatedEntity);
    }

    // DebtMapper-dən istifadə edirsiniz, ona görə mapEntityToResponseDto metoduna ehtiyac qalmır
    // Lakin Response DTO-ya userId-ni də əlavə etdiyimiz üçün, mapper-i də yeniləməlisən.
    // Əgər mapper-də əl ilə map etmirsənsə, o zaman DTO-ya userId sahəsini əlavə etməyin kifayətdir.
    // Əks halda, DebtMapper sinifini də yeniləməliyik.

}