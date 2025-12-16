package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.entity.DebtHistoryEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.DebtHistoryRepository;
import com.example.DebitCopybook.dao.repository.DebtRepository;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.example.DebitCopybook.exception.DebtNotFoundException;
import com.example.DebitCopybook.exception.InvalidRequestException;
import com.example.DebitCopybook.model.enums.DebtStatus;
import com.example.DebitCopybook.model.enums.HistoryEventType;
import com.example.DebitCopybook.model.mapper.DebtHistoryMapper;
import com.example.DebitCopybook.model.mapper.DebtMapper;
import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.response.DebtHistoryResponseDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import com.example.DebitCopybook.model.response.LegacyDebtResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtMapper debtMapper;
    private final UserRepository userRepository;
    private final DebtHistoryRepository debtHistoryRepository;
    private final DebtHistoryMapper debtHistoryMapper;

    private LegacyDebtResponseDto mapToLegacy(DebtEntity entity) {
        if (entity == null) return null;
        return LegacyDebtResponseDto.builder()
                .id(entity.getId())
                .debtorName(entity.getDebtorName())
                .description(entity.getDescription())
                .debtAmount(entity.getDebtAmount())
                .createdAt(entity.getCreatedAt().toString())
                .dueYear(entity.getDueYear())
                .dueMonth(entity.getDueMonth())
                .isFlexibleDueDate(entity.getIsFlexibleDueDate())
                .notes(entity.getNotes())
                .userId(entity.getUser().getId())
                .build();
    }

    private List<LegacyDebtResponseDto> mapListToLegacy(List<DebtEntity> entities) {
        return entities.stream().map(this::mapToLegacy).collect(Collectors.toList());
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserEntity)) {
            throw new IllegalStateException("Cari istifadəçi təyin olunmayıb.");
        }
        return ((UserEntity) authentication.getPrincipal()).getId();
    }

    @Transactional
    public LegacyDebtResponseDto createDebt(DebtRequestDto requestDto) {
        Long userId = getCurrentUserId();
        UserEntity currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new DebtNotFoundException("İstifadəçi tapılmadı ID: " + userId));

        int debtLimit = currentUser.isAdmin() ? 100 : 15;
        long currentDebtCount = debtRepository.countByUserId(userId);
        if (currentDebtCount >= debtLimit) {
            throw new IllegalStateException("Sizin borc siyahınızda limit dolub (" + debtLimit + " borc). ...");
        }

        String trimmedName = requestDto.getDebtorName().trim();
        Optional<DebtEntity> existingDebt = debtRepository.findPersonalDebtByName(userId, trimmedName);
        if (existingDebt.isPresent()) {
            throw new IllegalArgumentException("'" + trimmedName + "' adlı borcalan artıq bu siyahıda mövcuddur...");
        }

        if (requestDto.getDebtAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Borc məbləği 0 manatdan çox olmalıdır.");
        }

        requestDto.setDebtorName(trimmedName);

        DebtEntity debtEntity = debtMapper.mapRequestDtoToEntity(requestDto);
        debtEntity.setUser(currentUser);
        DebtEntity savedEntity = debtRepository.save(debtEntity);

        DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
                .debt(savedEntity).eventType(HistoryEventType.CREATED).description("Borc yaradıldı.")
                .amount(savedEntity.getDebtAmount()).eventDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
                .build();
        debtHistoryRepository.save(historyEntry);

        return mapToLegacy(savedEntity);
    }
    public List<LegacyDebtResponseDto> getAllDebts() {
        Long userId = getCurrentUserId();


        List<DebtEntity> debtEntities = debtRepository.findPersonalDebtsIncludeNull(userId, DebtStatus.SIMPLE);

        return mapListToLegacy(debtEntities);
    }

    public LegacyDebtResponseDto getDebtById(Long id) {
        Long userId = getCurrentUserId();
        DebtEntity debtEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
        return mapToLegacy(debtEntity);
    }

//    @Transactional
//    public LegacyDebtResponseDto makePayment(Long id, BigDecimal paymentAmount) {
//        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Ödəniş məbləği müsbət olmalıdır.");
//        }
//        Long userId = getCurrentUserId();
//        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
//                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
//        if (existingEntity.getStatus() == DebtStatus.CONFIRMED) {
//            throw new InvalidRequestException("Qarşılıqlı təsdiqlənmiş borcları bu bölmədən dəyişmək mümkün deyil...");
//        }
//        if (paymentAmount.compareTo(existingEntity.getDebtAmount()) > 0) {
//            throw new IllegalArgumentException("Ödəniş məbləği mövcud borcdan çox ola bilməz.");
//        }
//
//        // ... (Tarixçəyə yazma) ...
//
//        BigDecimal newDebt = existingEntity.getDebtAmount().subtract(paymentAmount);
//        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
//            debtHistoryRepository.deleteAll(debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(id));
//            debtRepository.delete(existingEntity);
//            return null;
//        } else {
//            existingEntity.setDebtAmount(newDebt);
//            DebtEntity updatedEntity = debtRepository.save(existingEntity);
//            return mapToLegacy(updatedEntity);
//        }
//    }


    @Transactional
    public LegacyDebtResponseDto makePayment(Long id, BigDecimal paymentAmount) {
        // 1. Validasiyalar
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ödəniş məbləği müsbət olmalıdır.");
        }

        Long userId = getCurrentUserId();

        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));

        if (existingEntity.getStatus() == DebtStatus.CONFIRMED) {
            throw new InvalidRequestException("Qarşılıqlı təsdiqlənmiş borcları bu bölmədən dəyişmək mümkün deyil...");
        }

        if (paymentAmount.compareTo(existingEntity.getDebtAmount()) > 0) {
            throw new IllegalArgumentException("Ödəniş məbləği mövcud borcdan çox ola bilməz.");
        }

        // 2. Yeni borc məbləğini hesablayırıq
        BigDecimal oldDebt = existingEntity.getDebtAmount();
        BigDecimal newDebt = oldDebt.subtract(paymentAmount);

        // --- TARİXÇƏNİN YAZILMASI (Detallı) ---
        DebtHistoryEntity history = new DebtHistoryEntity();
        history.setDebt(existingEntity);
        history.setEventType(HistoryEventType.PAYMENT); // Event növü: ÖDƏNİŞ
        history.setEventDate(LocalDateTime.now(ZoneOffset.ofHours(4))); // Bakı vaxtı

        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
            // Əgər borc tam bağlanırsa
            history.setDescription(String.format("Tam ödəniş edildi: %s AZN. Borc tamamilə bağlandı.", paymentAmount));
            // Qeyd: Borc silinəndə tarixçə də silinir (aşağıdakı koda əsasən),
            // amma logika düzgün olsun deyə bura yazırıq.
        } else {
            // Əgər qalıq qalırsa
            history.setDescription(String.format("Ödəniş edildi: %s AZN. Qalıq borc: %s AZN.", paymentAmount, newDebt));
        }

        // Tarixçəni yadda saxlayırıq (Silinməzdən əvvəl)
        debtHistoryRepository.save(history);


        // 3. Borcun Yenilənməsi və ya Silinməsi
        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
            // Borc bitdisə - Silirik (Sənin köhnə məntiqin)
            // Diqqət: Bu sətir həmin borca aid bütün tarixçəni silir.
            debtHistoryRepository.deleteAll(debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(id));
            debtRepository.delete(existingEntity);
            return null;
        } else {
            // Borc qaldısa - Yeniləyirik
            existingEntity.setDebtAmount(newDebt);
            DebtEntity updatedEntity = debtRepository.save(existingEntity);
            return mapToLegacy(updatedEntity);
        }
    }





    @Transactional
    public void deleteDebt(Long id) {
        Long userId = getCurrentUserId();
        DebtEntity debtToDelete = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
        debtHistoryRepository.deleteAll(debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(id));
        debtRepository.delete(debtToDelete);
    }

    public List<LegacyDebtResponseDto> getDebtsByYearAndMonth(Integer year, Integer month) {
        Long userId = getCurrentUserId();
        // DƏYİŞDİ: findPersonalDebtsByDate çağırırıq
        List<DebtEntity> debtEntities = debtRepository.findPersonalDebtsByDate(userId, year, month);
        return mapListToLegacy(debtEntities);
    }

    public List<LegacyDebtResponseDto> getFlexibleDueDateDebts() {
        Long userId = getCurrentUserId();
        // DƏYİŞDİ: findPersonalFlexibleDebts çağırırıq
        List<DebtEntity> debtEntities = debtRepository.findPersonalFlexibleDebts(userId);
        return mapListToLegacy(debtEntities);
    }

    public List<LegacyDebtResponseDto> searchDebtsByDebtorName(String debtorName) {
        Long userId = getCurrentUserId();
        // DƏYİŞDİ: searchPersonalDebtsByName çağırırıq
        List<DebtEntity> debtEntities = debtRepository.searchPersonalDebtsByName(userId, debtorName);
        return mapListToLegacy(debtEntities);
    }





//    @Transactional
//    public DebtResponseDto updateDebt(Long id, DebtRequestDto requestDto) {
//        Long userId = getCurrentUserId();
//
//        // 1. Dəyişdiriləcək borcu bazadan tapırıq
//        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
//                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
//
//        // 2. Qarşılıqlı borcların buradan dəyişdirilməsinin qarşısını alırıq
//        if (existingEntity.getStatus() == DebtStatus.CONFIRMED) {
//            throw new InvalidRequestException("Qarşılıqlı təsdiqlənmiş borcları bu bölmədən dəyişmək mümkün deyil...");
//        }
//
//        // 3. ===== ƏSAS DÜZƏLİŞ BURADADIR =====
//        // Mapper-in köməkçi metodu ilə requestDto-dan gələn yeni məlumatları
//        // bazadan tapdığımız köhnə entity-nin üzərinə yazırıq.
//        debtMapper.updateEntityFromRequestDto(requestDto, existingEntity);
//
//        // 4. Dəyişiklikləri tarixçəyə yazırıq (bu hissəni öz koduna uyğunlaşdır)
//        // Məsələn:
//        DebtHistoryEntity history = new DebtHistoryEntity();
//        history.setDebt(existingEntity);
//        history.setEventType(HistoryEventType.UPDATED);
//        history.setDescription("Borc məlumatları yeniləndi.");
//        history.setEventDate(LocalDateTime.now(ZoneOffset.ofHours(4)));
//        debtHistoryRepository.save(history);
//
//
//        // 5. Artıq üzəri yenilənmiş entity-ni bazada yadda saxlayırıq
//        DebtEntity updatedEntity = debtRepository.save(existingEntity);
//
//        // 6. Nəticəni frontend-ə uyğun DTO-ya çevirib qaytarırıq
//        // Qeyd: Əgər sən hələ də LegacyDebtResponseDto istifadə edirsənsə, mapEntityToResponseDto yerinə onu yaz.
//        // Amma bütün sistemin eyni DTO ilə işləməsi daha yaxşıdır.
//        return debtMapper.mapEntityToResponseDto(updatedEntity);
//    }



    @Transactional
    public DebtResponseDto updateDebt(Long id, DebtRequestDto requestDto) {
        Long userId = getCurrentUserId();

        // 1. Dəyişdiriləcək borcu bazadan tapırıq
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));

        // 2. Qarşılıqlı (Təsdiqlənmiş) borcların buradan dəyişdirilməsinin qarşısını alırıq (Bura toxunmuruq)
        if (existingEntity.getStatus() == DebtStatus.CONFIRMED) {
            throw new InvalidRequestException("Qarşılıqlı təsdiqlənmiş borcları bu bölmədən dəyişmək mümkün deyil...");
        }

        // --- DƏYİŞİKLİKLƏRİ İZLƏMƏK ÜÇÜN KÖHNƏ DƏYƏRLƏRİ YADDA SAXLAYIRIQ ---
        String oldName = existingEntity.getDebtorName();
        BigDecimal oldAmount = existingEntity.getDebtAmount();
        String oldDescription = existingEntity.getDescription(); // Borcun növü (mənim borcum/mənə olan)
        String oldNotes = existingEntity.getNotes();

        // Köhnə tarixi oxunaqlı formata salırıq
        String oldDateStr;
        if (Boolean.TRUE.equals(existingEntity.getIsFlexibleDueDate())) {
            oldDateStr = "Müddətsiz";
        } else {
            oldDateStr = (existingEntity.getDueMonth() != null ? existingEntity.getDueMonth() : "-") + "/" +
                    (existingEntity.getDueYear() != null ? existingEntity.getDueYear() : "-");
        }


        // 3. Mapper vasitəsilə yeni məlumatları entity-nin üzərinə yazırıq
        debtMapper.updateEntityFromRequestDto(requestDto, existingEntity);


        // --- MÜQAYİSƏ VƏ TARİXÇƏ MƏTNİNİN YIĞILMASI ---
        StringBuilder changes = new StringBuilder();

        // 1. Ad yoxlaması
        if (oldName != null && !oldName.equals(existingEntity.getDebtorName())) {
            changes.append(String.format("Ad '%s' sözündən '%s' sözünə dəyişdirildi. ", oldName, existingEntity.getDebtorName()));
        }

        // 2. Məbləğ yoxlaması
        if (oldAmount != null && oldAmount.compareTo(existingEntity.getDebtAmount()) != 0) {
            changes.append(String.format("Məbləğ %s-dən %s-ə dəyişdirildi. ", oldAmount, existingEntity.getDebtAmount()));
        }

        // 3. Borc növü yoxlaması
        if (oldDescription != null && !oldDescription.equals(existingEntity.getDescription())) {
            changes.append(String.format("Borcun növü '%s'-dən '%s'-ə dəyişdirildi. ", oldDescription, existingEntity.getDescription()));
        }

        // 4. Tarix yoxlaması
        String newDateStr;
        if (Boolean.TRUE.equals(existingEntity.getIsFlexibleDueDate())) {
            newDateStr = "Müddətsiz";
        } else {
            newDateStr = (existingEntity.getDueMonth() != null ? existingEntity.getDueMonth() : "-") + "/" +
                    (existingEntity.getDueYear() != null ? existingEntity.getDueYear() : "-");
        }

        if (!oldDateStr.equals(newDateStr)) {
            changes.append(String.format("Qaytarılma tarixi '%s'-dən '%s'-ə dəyişdirildi. ", oldDateStr, newDateStr));
        }

        // 5. Qeyd (Notes) yoxlaması - Detallı
        String newNotes = existingEntity.getNotes();
        boolean oldHasNotes = oldNotes != null && !oldNotes.trim().isEmpty();
        boolean newHasNotes = newNotes != null && !newNotes.trim().isEmpty();

        if (!oldHasNotes && newHasNotes) {
            // Əvvəl yox idi, indi yazılıb
            changes.append(String.format("Qeyd əlavə edildi: '%s'. ", newNotes));
        } else if (oldHasNotes && !newHasNotes) {
            // Əvvəl var idi, indi silinib
            changes.append("Qeyd silindi. ");
        } else if (oldHasNotes && newHasNotes && !oldNotes.equals(newNotes)) {
            // Dəyişdirilib
            changes.append(String.format("Qeyd '%s' sözündən '%s' sözünə dəyişdirildi. ", oldNotes, newNotes));
        }

        // 4. Dəyişiklik varsa tarixçəyə yazırıq
        if (changes.length() > 0) {
            DebtHistoryEntity history = new DebtHistoryEntity();
            history.setDebt(existingEntity);
            history.setEventType(HistoryEventType.UPDATED);
            history.setDescription(changes.toString().trim()); // Yığılan mətni bura qoyuruq
            history.setEventDate(LocalDateTime.now(ZoneOffset.ofHours(4)));
            debtHistoryRepository.save(history);
        } else {
            // Əgər heç nə dəyişməyibsə, amma "Yadda saxla" basılıbsa, boş yerə history yaratmaya da bilərsən
            // və ya sadəcə "Yenilənmə cəhdi (dəyişiklik yoxdur)" yaza bilərsən.
            // Hazırda heç nə yazmır (boşuna yer tutmasın deyə).
        }

        // 5. Artıq üzəri yenilənmiş entity-ni bazada yadda saxlayırıq
        DebtEntity updatedEntity = debtRepository.save(existingEntity);

        // 6. Nəticəni qaytarırıq
        return debtMapper.mapEntityToResponseDto(updatedEntity);
    }









//    @Transactional
//    public LegacyDebtResponseDto increaseDebt(Long id, BigDecimal amountToAdd) {
//        if (amountToAdd == null || amountToAdd.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Əlavə olunacaq məbləğ müsbət olmalıdır.");
//        }
//        Long userId = getCurrentUserId();
//        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
//                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
//        if (existingEntity.getStatus() == DebtStatus.CONFIRMED) {
//            throw new InvalidRequestException("Qarşılıqlı təsdiqlənmiş borcları bu bölmədən dəyişmək mümkün deyil...");
//        }
//
//        BigDecimal oldAmount = existingEntity.getDebtAmount();
//        BigDecimal newDebtAmount = oldAmount.add(amountToAdd);
//        existingEntity.setDebtAmount(newDebtAmount);
//        DebtEntity updatedEntity = debtRepository.save(existingEntity);
//
//        // ... (tarixçəyə yazma) ...
//        return mapToLegacy(updatedEntity);
//    }

    @Transactional
    public LegacyDebtResponseDto increaseDebt(Long id, BigDecimal amountToAdd) {
        // 1. Validasiyalar
        if (amountToAdd == null || amountToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Əlavə olunacaq məbləğ müsbət olmalıdır.");
        }

        Long userId = getCurrentUserId();

        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));

        // Qarşılıqlı borcları bloklayır
        if (existingEntity.getStatus() == DebtStatus.CONFIRMED) {
            throw new InvalidRequestException("Qarşılıqlı təsdiqlənmiş borcları bu bölmədən dəyişmək mümkün deyil...");
        }

        // 2. Hesablama
        BigDecimal oldAmount = existingEntity.getDebtAmount();
        BigDecimal newDebtAmount = oldAmount.add(amountToAdd);

        // --- TARİXÇƏNİN YAZILMASI (Detallı) ---
        DebtHistoryEntity history = new DebtHistoryEntity();
        history.setDebt(existingEntity);
        history.setEventType(HistoryEventType.UPDATED); // Status: YENİLƏNDİ
        // Mətni formalaşdırırıq: "Məbləğ 10.00 AZN artırıldı. Yeni borc: 30.00 AZN."
        history.setDescription(String.format("Məbləğ %s AZN artırıldı. Yeni borc: %s AZN.", amountToAdd, newDebtAmount));
        history.setEventDate(LocalDateTime.now(ZoneOffset.ofHours(4))); // Bakı vaxtı
        debtHistoryRepository.save(history);


        // 3. Yadda saxlama
        existingEntity.setDebtAmount(newDebtAmount);
        DebtEntity updatedEntity = debtRepository.save(existingEntity);

        return mapToLegacy(updatedEntity);
    }

























//    public List<DebtHistoryResponseDto> getDebtHistory(Long debtId) {
//        Long userId = getCurrentUserId();
//        debtRepository.findByIdAndUserId(debtId, userId)
//                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + debtId + " ilə tapılmadı..."));
//        List<DebtHistoryEntity> historyEntities = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(debtId);
//        return debtHistoryMapper.toDtoList(historyEntities);
//    }


    public List<DebtHistoryResponseDto> getDebtHistory(Long debtId) {
        Long userId = getCurrentUserId();

        // DƏYİŞİKLİK BURADADIR:
        // Əvvəl findByIdAndUserId idi (ancaq yaradanı yoxlayırdı).
        // İndi findSharedSharedDebtForUser istifadə edirik (hər iki tərəf görə bilsin).
        debtRepository.findSharedDebtForUser(debtId, userId)
                .orElseThrow(() -> new DebtNotFoundException("Bu borcu görmək üçün icazəniz yoxdur və ya borc tapılmadı. ID: " + debtId));

        List<DebtHistoryEntity> historyEntities = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(debtId);
        return debtHistoryMapper.toDtoList(historyEntities);
    }



    public List<LegacyDebtResponseDto> getDebtsByDescription(String description) {
        Long userId = getCurrentUserId();
        // DƏYİŞDİ: findPersonalDebtsByDescription çağırırıq
        List<DebtEntity> debtEntities = debtRepository.findPersonalDebtsByDescription(userId, description);
        return mapListToLegacy(debtEntities);
    }
}