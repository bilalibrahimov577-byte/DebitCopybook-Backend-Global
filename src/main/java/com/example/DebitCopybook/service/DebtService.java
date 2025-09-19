package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.entity.DebtHistoryEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.DebtHistoryRepository;
import com.example.DebitCopybook.dao.repository.DebtRepository;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.example.DebitCopybook.exception.DebtNotFoundException;
import com.example.DebitCopybook.model.enums.HistoryEventType;
import com.example.DebitCopybook.model.mapper.DebtHistoryMapper;
import com.example.DebitCopybook.model.mapper.DebtMapper;
import com.example.DebitCopybook.model.request.DebtRequestDto;

import com.example.DebitCopybook.model.response.DebtHistoryResponseDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtMapper debtMapper;
    private final UserRepository userRepository;
    private final DebtHistoryRepository debtHistoryRepository;
    private final DebtHistoryMapper debtHistoryMapper;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {

            throw new IllegalStateException("Cari istifadəçi identifikasiya olunmayıb.");
        }


        if (authentication.getPrincipal() instanceof UserEntity) {
            UserEntity currentUser = (UserEntity) authentication.getPrincipal();
            return currentUser.getId();
        }


        throw new IllegalStateException("Cari istifadəçi məlumatları tapılmadı və ya gözlənilən formatda deyil.");
    }







    @Transactional
    public DebtResponseDto createDebt(DebtRequestDto requestDto) {
        Long userId = getCurrentUserId();
        UserEntity currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new DebtNotFoundException("İstifadəçi tapılmadı ID: " + userId));

        int debtLimit = currentUser.isAdmin() ? 100 : 25;
        long currentDebtCount = debtRepository.countByUserId(userId);
        if (currentDebtCount >= debtLimit) {
            throw new IllegalStateException("Sizin borc siyahınızda limit dolub (" + debtLimit + " borc). " +
                    "Yeni borc əlavə etmək üçün mövcud borcları bağlayın və ya whatsapp(+99450-740-28-09) vasitəsilə adminlə əlaqə saxlayın.");
        }

        String trimmedName = requestDto.getDebtorName().trim();
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

        requestDto.setDebtorName(trimmedName);
        DebtEntity debtEntity = debtMapper.mapRequestDtoToEntity(requestDto);
        debtEntity.setUser(currentUser);

        // Əsas borcu verilənlər bazasına yadda saxlayırıq
        DebtEntity savedEntity = debtRepository.save(debtEntity);

        // === BAŞLA: YENİ ƏLAVƏ EDİLƏN HİSSƏ ===

        // İndi bu əməliyyatın tarixçəsini yaradırıq
        DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
                .debt(savedEntity) // Hansı borca aid olduğunu göstəririk
                .eventType(HistoryEventType.CREATED) // Hadisənin növü: YARADILDI
                .description("Borc yaradıldı.") // İstifadəçinin görəcəyi açıqlama
                .amount(savedEntity.getDebtAmount()) // Yaradılan borcun ilkin məbləğini də qeyd edək
                .eventDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
                .build();

        // Tarixçə qeydini verilənlər bazasına yadda saxlayırıq
        debtHistoryRepository.save(historyEntry);

        // === SON: YENİ ƏLAVƏ EDİLƏN HİSSƏ ===

        // Ən sonda nəticəni istifadəçiyə qaytarırıq
        return debtMapper.mapEntityToResponseDto(savedEntity);
    }













    public List<DebtResponseDto> getAllDebts() {

        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findAllByUserId(userId);
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    public DebtResponseDto getDebtById(Long id) {

        Long userId = getCurrentUserId();
        DebtEntity debtEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));
        return debtMapper.mapEntityToResponseDto(debtEntity);
    }

    @Transactional
    public DebtResponseDto makePayment(Long id, BigDecimal paymentAmount) {

        // 1. GİRİŞ PARAMETRLƏRİNİ YOXLAYIRIQ
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ödəniş məbləği müsbət olmalıdır.");
        }

        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        BigDecimal currentDebt = existingEntity.getDebtAmount();

        if (paymentAmount.compareTo(currentDebt) > 0) {
            throw new IllegalArgumentException("Ödəniş məbləği (" + paymentAmount + " AZN) mövcud borcdan (" + currentDebt + " AZN) çox ola bilməz.");
        }

        // 2. ÖDƏNİŞ ƏMƏLİYYATINI TARİXÇƏYƏ YAZIRIQ
        String description = paymentAmount + " AZN ödəniş edildi.";
        DebtHistoryEntity paymentHistoryEntry = DebtHistoryEntity.builder()
                .debt(existingEntity)
                .eventType(HistoryEventType.PAYMENT)
                .description(description)
                .amount(paymentAmount.negate())
                .eventDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
                .build();
        debtHistoryRepository.save(paymentHistoryEntry);

        // 3. YENİ BORC MƏBLƏĞİNİ HESABLAYIRIQ
        BigDecimal newDebt = currentDebt.subtract(paymentAmount);

        // 4. ŞƏRTİ YOXLAYIRIQ: BORC TAM ÖDƏNİLDİMİ?
        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
            // BƏLİ, BORC TAM ÖDƏNİLDİ VƏ SİLİNMƏLİDİR

            // 4a. Silinmə əməliyyatını da tarixçəyə yazırıq (istəyə bağlı, amma informativdir)
            DebtHistoryEntity closingHistoryEntry = DebtHistoryEntity.builder()
                    .debt(existingEntity)
                    .eventType(HistoryEventType.UPDATED)
                    .description("Borc tam ödənildi və bütün məlumatlar silindi.")
                    .eventDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
                    .build();
            debtHistoryRepository.save(closingHistoryEntry);

            // 4b. Bu borca aid olan BÜTÜN tarixçə qeydlərini tapırıq
            List<DebtHistoryEntity> allHistoryOfThisDebt = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(id);

            // 4c. Və bütün bu tarixçəni silirik
            if (!allHistoryOfThisDebt.isEmpty()) {
                debtHistoryRepository.deleteAll(allHistoryOfThisDebt);
            }

            // 4d. Ən sonda, tarixçəsi təmizləndikdən sonra, əsas borcun özünü silirik
            debtRepository.delete(existingEntity);

            // 4e. İstifadəçiyə "borc silindi" məlumatı olan bir cavab qaytarırıq
            return DebtResponseDto.builder()
                    .id(id)
                    .debtorName(existingEntity.getDebtorName())
                    .debtAmount(BigDecimal.ZERO)
                    .notes("Borc tam ödənildi və bütün qeydlər silindi.")
                    .userId(userId)
                    .build();
        } else {
            // XEYR, BORC QİSMƏN ÖDƏNİLDİ

            // 5a. Sadəcə borcun qalıq məbləğini yeniləyirik
            existingEntity.setDebtAmount(newDebt);
            DebtEntity updatedEntity = debtRepository.save(existingEntity);

            // 5b. Və yenilənmiş borc məlumatlarını istifadəçiyə qaytarırıq
            return debtMapper.mapEntityToResponseDto(updatedEntity);
        }
    }



//    @Transactional
//    public DebtResponseDto makePayment(Long id, BigDecimal paymentAmount) {
//
//        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Ödəniş məbləği müsbət olmalıdır.");
//        }
//
//        Long userId = getCurrentUserId();
//        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
//                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));
//
//        BigDecimal currentDebt = existingEntity.getDebtAmount();
//
//        // === YENİ: Əgər ödəniş borcdan çoxdursa, bunu xəta kimi qəbul edək ===
//        // Bu, istifadəçinin səhvən böyük rəqəm yazmasının qarşısını alar.
//        if (paymentAmount.compareTo(currentDebt) > 0) {
//            throw new IllegalArgumentException("Ödəniş məbləği (" + paymentAmount + " AZN) mövcud borcdan (" + currentDebt + " AZN) çox ola bilməz.");
//        }
//
//        BigDecimal newDebt = currentDebt.subtract(paymentAmount);
//
//        // === BAŞLA: YENİ ƏLAVƏ EDİLƏN HİSSƏ (Tarixçəni yaradaq) ===
//
//        // Açıqlama mətni hər iki hal üçün eyni ola bilər
//        String description = paymentAmount + " AZN ödəniş edildi.";
//
//        // Tarixçə qeydini əvvəlcədən hazırlayırıq
//        DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
//                .debt(existingEntity)
//                .eventType(HistoryEventType.PAYMENT) // Hadisənin növü: ÖDƏNİŞ
//                .description(description)
//                .amount(paymentAmount.negate()) // Ödəniş olduğu üçün məbləği mənfi işarə ilə saxlayaq
//                .eventDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
//                .build();
//
//        debtHistoryRepository.save(historyEntry);
//
//        // =========================================================================
//
//
//
//
//        // Sənin mövcud məntiqin
//        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
//            // Borc tam ödənildi.
//            // Tarixçəyə əlavə bir qeyd də ata bilərik (istəyə bağlı)
//            DebtHistoryEntity closingHistory = DebtHistoryEntity.builder()
//                    .debt(existingEntity)
//                    .eventType(HistoryEventType.UPDATED)
//                    .description("Borc tam ödənildi və siyahıdan silindi.")
//                    .build();
//            debtHistoryRepository.save(closingHistory);
//            debtHistoryRepository.flush();
//            debtRepository.delete(existingEntity);
//
//            // İstifadəçiyə cavab hazırlayırıq (sənin yazdığın kimi)
//            return DebtResponseDto.builder()
//                    .id(id)
//                    .debtorName(existingEntity.getDebtorName())
//                    .description(existingEntity.getDescription())
//                    .debtAmount(BigDecimal.ZERO)
//                    .createdAt(existingEntity.getCreatedAt())
//                    .dueYear(existingEntity.getDueYear())
//                    .dueMonth(existingEntity.getDueMonth())
//                    .isFlexibleDueDate(existingEntity.getIsFlexibleDueDate())
//                    .notes("Borc tam ödənildi və silindi.") // 'notes' sahəsini bu məqsədlə istifadə etmək əla fikirdir
//                    .userId(userId)
//                    .build();
//        } else {
//            // Borc qismən ödənildi
//            existingEntity.setDebtAmount(newDebt);
//            DebtEntity updatedEntity = debtRepository.save(existingEntity);
//            return debtMapper.mapEntityToResponseDto(updatedEntity);
//        }
//    }



    @Transactional
    public void deleteDebt(Long id) {
        Long userId = getCurrentUserId();


        DebtEntity debtToDelete = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));


        List<DebtHistoryEntity> historyToDelete = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(id);


        if (!historyToDelete.isEmpty()) {
            debtHistoryRepository.deleteAll(historyToDelete);
        }

        // 3. Yalnız bundan sonra əsas borcu silirik
        debtRepository.delete(debtToDelete);
    }







//    @Transactional
//    public void deleteDebt(Long id) {
//
//        Long userId = getCurrentUserId();
//        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
//                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));
//        debtRepository.delete(existingEntity);
//    }

    public List<DebtResponseDto> getDebtsByYearAndMonth(Integer year, Integer month) {

        if (year == null || month == null) {
            throw new IllegalArgumentException("Borcları il və aya görə axtarmaq üçün hər ikisi qeyd olunmalıdır.");
        }
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndDueYearAndDueMonth(userId, year, month);
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    public List<DebtResponseDto> getFlexibleDueDateDebts() {

        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndIsFlexibleDueDateTrue(userId);
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    public List<DebtResponseDto> searchDebtsByDebtorName(String debtorName) {

        if (debtorName == null || debtorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Axtarış üçün borcalanın adı boş ola bilməz.");
        }
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndDebtorNameContainingIgnoreCase(userId, debtorName);
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }




//    @Transactional
//    public DebtResponseDto updateDebt(Long id, DebtRequestDto requestDto) {
//
//        Long userId = getCurrentUserId();
//        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
//                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));
//
//        // === BAŞLA: YENİ ƏLAVƏ EDİLƏN HİSSƏ (Köhnə dəyərləri yadda saxlayaq) ===
//        // Dəyişikliklərin açıqlamasını saxlamaq üçün bir siyahı yaradırıq
//        List<String> changes = new ArrayList<>();
//
//        // Köhnə dəyərləri bir-bir yadda saxlayırıq ki, sonra müqayisə edə bilək
//        String oldName = existingEntity.getDebtorName();
//        BigDecimal oldAmount = existingEntity.getDebtAmount();
//        String oldDescription = existingEntity.getDescription();
//        Integer oldDueYear = existingEntity.getDueYear();
//        Integer oldDueMonth = existingEntity.getDueMonth();
//        Boolean oldIsFlexible = existingEntity.getIsFlexibleDueDate();
//        // =====================================================================
//
//
//        // === SƏNİN MÖVCUD KODUN (DƏYİŞİKLİKLƏRİ TƏTBİQ EDİR) ===
//        // Bu hissəyə heç bir dəyişiklik edilməyib
//        if (requestDto.getDebtorName() != null && !requestDto.getDebtorName().isBlank()) {
//            String trimmedName = requestDto.getDebtorName().trim();
//            Optional<DebtEntity> anotherDebtWithSameName = debtRepository.findByUserIdAndDebtorNameIgnoreCase(userId, trimmedName);
//            if (anotherDebtWithSameName.isPresent() && !anotherDebtWithSameName.get().getId().equals(id)) {
//                throw new IllegalArgumentException("'" + trimmedName + "' adlı borcalan artıq mövcuddur...");
//            }
//            existingEntity.setDebtorName(trimmedName);
//        }
//
//        if (requestDto.getDebtAmount() != null) {
//            if (requestDto.getDebtAmount().compareTo(BigDecimal.ZERO) < 0) {
//                throw new IllegalArgumentException("Borc məbləği mənfi ola bilməz.");
//            }
//            existingEntity.setDebtAmount(requestDto.getDebtAmount());
//        }
//
//        if (requestDto.getDescription() != null) {
//            existingEntity.setDescription(requestDto.getDescription());
//        }
//        if (requestDto.getNotes() != null) {
//            existingEntity.setNotes(requestDto.getNotes());
//        }
//
//        if (requestDto.getIsFlexibleDueDate() != null) {
//            if (requestDto.getIsFlexibleDueDate()) {
//                existingEntity.setIsFlexibleDueDate(true);
//                existingEntity.setDueYear(null);
//                existingEntity.setDueMonth(null);
//            } else {
//                if (requestDto.getDueYear() == null || requestDto.getDueMonth() == null) {
//                    throw new IllegalArgumentException("Konkret tarixə keçmək üçün il və ay qeyd olunmalıdır.");
//                }
//                existingEntity.setIsFlexibleDueDate(false);
//                existingEntity.setDueYear(requestDto.getDueYear());
//                existingEntity.setDueMonth(requestDto.getDueMonth());
//            }
//        } else {
//            if (requestDto.getDueYear() != null) {
//                existingEntity.setDueYear(requestDto.getDueYear());
//            }
//            if (requestDto.getDueMonth() != null) {
//                existingEntity.setDueMonth(requestDto.getDueMonth());
//            }
//        }
//        // === SƏNİN KODUNUN SONU ===
//
//
//        // === BAŞLA: YENİ ƏLAVƏ EDİLƏN HİSSƏ (Dəyişiklikləri yoxlayıb tarixçə yaradaq) ===
//
//        // İndi köhnə və yeni dəyərləri müqayisə edirik
//        if (!Objects.equals(oldName, existingEntity.getDebtorName())) {
//            changes.add("Ad '" + oldName + "'-dan '" + existingEntity.getDebtorName() + "'-a dəyişdirildi.");
//        }
//        // BigDecimal müqayisəsi üçün compareTo istifadə edirik
//        if (oldAmount.compareTo(existingEntity.getDebtAmount()) != 0) {
//            changes.add("Məbləğ " + oldAmount + " AZN-dən " + existingEntity.getDebtAmount() + " AZN-ə dəyişdirildi.");
//        }
//        if (!Objects.equals(oldDescription, existingEntity.getDescription())) {
//            changes.add("Açıqlama yeniləndi.");
//        }
//        if (!Objects.equals(oldIsFlexible, existingEntity.getIsFlexibleDueDate()) ||
//                !Objects.equals(oldDueYear, existingEntity.getDueYear()) ||
//                !Objects.equals(oldDueMonth, existingEntity.getDueMonth())) {
//            changes.add("Son ödəmə tarixi yeniləndi.");
//        }
//
//        // Əgər hər hansı bir dəyişiklik varsa, tarixçəyə qeyd atırıq
//        if (!changes.isEmpty()) {
//            DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
//                    .debt(existingEntity)
//                    .eventType(HistoryEventType.UPDATED)
//                    .description(String.join(" \n", changes)) // Bütün dəyişiklikləri birləşdirib bir mətn edirik
//                    .eventDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
//                    .build();
//            debtHistoryRepository.save(historyEntry);
//        }
//        // =====================================================================
//
//        // Entity-nin son vəziyyətini bazada yadda saxlayırıq (bu sənin kodunda artıq var idi)
//        DebtEntity updatedEntity = debtRepository.save(existingEntity);
//        return debtMapper.mapEntityToResponseDto(updatedEntity);
//    }



    @Transactional
    public DebtResponseDto updateDebt(Long id, DebtRequestDto requestDto) {

        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        List<String> changes = new ArrayList<>();

        String oldName = existingEntity.getDebtorName();
        BigDecimal oldAmount = existingEntity.getDebtAmount();
        String oldDescription = existingEntity.getDescription();
        String oldNotes = existingEntity.getNotes();
        Integer oldDueYear = existingEntity.getDueYear();
        Integer oldDueMonth = existingEntity.getDueMonth();
        Boolean oldIsFlexible = existingEntity.getIsFlexibleDueDate();

        if (requestDto.getDebtorName() != null && !requestDto.getDebtorName().isBlank()) {
            String trimmedName = requestDto.getDebtorName().trim();
            Optional<DebtEntity> anotherDebtWithSameName = debtRepository.findByUserIdAndDebtorNameIgnoreCase(userId, trimmedName);
            if (anotherDebtWithSameName.isPresent() && !anotherDebtWithSameName.get().getId().equals(id)) {
                throw new IllegalArgumentException("'" + trimmedName + "' adlı borcalan artıq mövcuddur.");
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

        if (!Objects.equals(oldName, existingEntity.getDebtorName())) {
            changes.add("Ad '" + oldName + "'-dan '" + existingEntity.getDebtorName() + "'-a dəyişdirildi.");
        }

        if (oldAmount.compareTo(existingEntity.getDebtAmount()) != 0) {
            changes.add("Məbləğ " + oldAmount + " AZN-dən " + existingEntity.getDebtAmount() + " AZN-ə dəyişdirildi.");
        }

        if (!Objects.equals(oldDescription, existingEntity.getDescription())) {
            boolean oldDescWasEmpty = oldDescription == null || oldDescription.isBlank();
            boolean newDescIsEmpty = existingEntity.getDescription() == null || existingEntity.getDescription().isBlank();

            if (oldDescWasEmpty && !newDescIsEmpty) {
                changes.add("Açıqlama '" + existingEntity.getDescription() + "' olaraq təyin edildi.");
            } else if (!oldDescWasEmpty && newDescIsEmpty) {
                changes.add("Açıqlama ('" + oldDescription + "') silindi.");
            } else {
                changes.add("Açıqlama '" + oldDescription + "'-dan '" + existingEntity.getDescription() + "'-a dəyişdirildi.");
            }
        }

        if (!Objects.equals(oldNotes, existingEntity.getNotes())) {
            boolean oldNotesWasEmpty = oldNotes == null || oldNotes.isBlank();
            boolean newNotesIsEmpty = existingEntity.getNotes() == null || existingEntity.getNotes().isBlank();

            if (oldNotesWasEmpty && !newNotesIsEmpty) {
                changes.add("Qeyd əlavə edildi.");
            } else if (!oldNotesWasEmpty && newNotesIsEmpty) {
                changes.add("Qeyd silindi.");
            } else {
                changes.add("Qeyd yeniləndi.");
            }
        }

        if (!Objects.equals(oldIsFlexible, existingEntity.getIsFlexibleDueDate()) ||
                !Objects.equals(oldDueYear, existingEntity.getDueYear()) ||
                !Objects.equals(oldDueMonth, existingEntity.getDueMonth())) {
            changes.add("Son ödəmə tarixi yeniləndi.");
        }

        if (!changes.isEmpty()) {
            DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
                    .debt(existingEntity)
                    .eventType(HistoryEventType.UPDATED)
                    .description(String.join("\n", changes))
                    .eventDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
                    .build();
            debtHistoryRepository.save(historyEntry);
        }

        DebtEntity updatedEntity = debtRepository.save(existingEntity);
        return debtMapper.mapEntityToResponseDto(updatedEntity);
    }










    @Transactional
    public DebtResponseDto increaseDebt(Long id, BigDecimal amountToAdd) {

        if (amountToAdd == null || amountToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Əlavə olunacaq məbləğ müsbət olmalıdır.");
        }

        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        // === BAŞLA: YENİ ƏLAVƏ EDİLƏN HİSSƏ (Tarixçə üçün köhnə məbləği saxlayaq) ===
        BigDecimal oldAmount = existingEntity.getDebtAmount();
        // =========================================================================

        BigDecimal newDebtAmount = oldAmount.add(amountToAdd);
        existingEntity.setDebtAmount(newDebtAmount);

        DebtEntity updatedEntity = debtRepository.save(existingEntity);

        // === BAŞLA: YENİ ƏLAVƏ EDİLƏN HİSSƏ (Tarixçəni yaradaq) ===

        String description = "Borc " + oldAmount + " AZN-dən " + newDebtAmount + " AZN-ə artırıldı (" + amountToAdd + " AZN əlavə edildi).";

        DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
                .debt(updatedEntity)
                .eventType(HistoryEventType.UPDATED) // Bu da bir növ yeniləmədir
                .description(description)
                .amount(amountToAdd) // Nə qədər artırıldığını da qeyd edək
                .eventDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
                .build();

        debtHistoryRepository.save(historyEntry);

        // =========================================================================

        return debtMapper.mapEntityToResponseDto(updatedEntity);
    }




    public List<DebtHistoryResponseDto> getDebtHistory(Long debtId) {
        Long userId = getCurrentUserId();

        // Təhlükəsizlik yoxlaması: borc bu istifadəçiyə aiddirmi?
        debtRepository.findByIdAndUserId(debtId, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + debtId + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        // Borcun tarixçəsini bazadan çəkirik (ən yenidən ən köhnəyə)
        List<DebtHistoryEntity> historyEntities = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(debtId);

        // Entity siyahısını DTO siyahısına çevirib qaytarırıq
        return debtHistoryMapper.toDtoList(historyEntities);
    }

}