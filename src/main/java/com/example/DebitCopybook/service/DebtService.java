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
        Optional<DebtEntity> existingDebt = debtRepository.findByUserIdAndDebtorNameIgnoreCase(userId, trimmedName);
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
        List<DebtEntity> debtEntities = debtRepository.findAllByUserId(userId);
        return mapListToLegacy(debtEntities);
    }

    public LegacyDebtResponseDto getDebtById(Long id) {
        Long userId = getCurrentUserId();
        DebtEntity debtEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
        return mapToLegacy(debtEntity);
    }

    @Transactional
    public LegacyDebtResponseDto makePayment(Long id, BigDecimal paymentAmount) {
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

        // ... (Tarixçəyə yazma) ...

        BigDecimal newDebt = existingEntity.getDebtAmount().subtract(paymentAmount);
        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
            debtHistoryRepository.deleteAll(debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(id));
            debtRepository.delete(existingEntity);
            return null;
        } else {
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
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndDueYearAndDueMonth(userId, year, month);
        return mapListToLegacy(debtEntities);
    }

    public List<LegacyDebtResponseDto> getFlexibleDueDateDebts() {
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndIsFlexibleDueDateTrue(userId);
        return mapListToLegacy(debtEntities);
    }

    public List<LegacyDebtResponseDto> searchDebtsByDebtorName(String debtorName) {
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByUserIdAndDebtorNameContainingIgnoreCase(userId, debtorName);
        return mapListToLegacy(debtEntities);
    }



    @Transactional
    public LegacyDebtResponseDto updateDebt(Long id, DebtRequestDto requestDto) {
        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
        if (existingEntity.getStatus() == DebtStatus.CONFIRMED) {
            throw new InvalidRequestException("Qarşılıqlı təsdiqlənmiş borcları bu bölmədən dəyişmək mümkün deyil...");
        }
        // ... (bütün update məntiqi eyni qalır, ad dəyişməsi, məbləğ dəyişməsi və s.) ...

        DebtEntity updatedEntity = debtRepository.save(existingEntity);
        // ... (dəyişiklikləri tarixçəyə yazma) ...
        return mapToLegacy(updatedEntity);
    }

    @Transactional
    public LegacyDebtResponseDto increaseDebt(Long id, BigDecimal amountToAdd) {
        if (amountToAdd == null || amountToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Əlavə olunacaq məbləğ müsbət olmalıdır.");
        }
        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
        if (existingEntity.getStatus() == DebtStatus.CONFIRMED) {
            throw new InvalidRequestException("Qarşılıqlı təsdiqlənmiş borcları bu bölmədən dəyişmək mümkün deyil...");
        }

        BigDecimal oldAmount = existingEntity.getDebtAmount();
        BigDecimal newDebtAmount = oldAmount.add(amountToAdd);
        existingEntity.setDebtAmount(newDebtAmount);
        DebtEntity updatedEntity = debtRepository.save(existingEntity);

        // ... (tarixçəyə yazma) ...
        return mapToLegacy(updatedEntity);
    }

    public List<DebtHistoryResponseDto> getDebtHistory(Long debtId) {
        Long userId = getCurrentUserId();
        debtRepository.findByIdAndUserId(debtId, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + debtId + " ilə tapılmadı..."));
        List<DebtHistoryEntity> historyEntities = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(debtId);
        return debtHistoryMapper.toDtoList(historyEntities);
    }

    public List<LegacyDebtResponseDto> getDebtsByDescription(String description) {
        Long userId = getCurrentUserId();
        List<DebtEntity> debtEntities = debtRepository.findByDescriptionAndUserId(description, userId);
        return mapListToLegacy(debtEntities);
    }
}