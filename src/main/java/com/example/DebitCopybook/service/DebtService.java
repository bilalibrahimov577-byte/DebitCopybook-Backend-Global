package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.DebtRepository;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.example.DebitCopybook.exception.DebtNotFoundException;
import com.example.DebitCopybook.model.mapper.DebtMapper;
import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;



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

        DebtEntity savedEntity = debtRepository.save(debtEntity);
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
                    .debtAmount(BigDecimal.ZERO)
                    .createdAt(existingEntity.getCreatedAt())
                    .dueYear(existingEntity.getDueYear())
                    .dueMonth(existingEntity.getDueMonth())
                    .isFlexibleDueDate(existingEntity.getIsFlexibleDueDate())
                    .notes("Borc tam ödənildi və silindi.")
                    .userId(userId)
                    .build();
        } else {
            existingEntity.setDebtAmount(newDebt);
            DebtEntity updatedEntity = debtRepository.save(existingEntity);
            return debtMapper.mapEntityToResponseDto(updatedEntity);
        }
    }

    @Transactional
    public void deleteDebt(Long id) {

        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));
        debtRepository.delete(existingEntity);
    }

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

    @Transactional
    public DebtResponseDto updateDebt(Long id, DebtRequestDto requestDto) {

        Long userId = getCurrentUserId();
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        if (requestDto.getDebtorName() != null && !requestDto.getDebtorName().isBlank()) {
            String trimmedName = requestDto.getDebtorName().trim();


            Optional<DebtEntity> anotherDebtWithSameName = debtRepository.findByUserIdAndDebtorNameIgnoreCase(userId, trimmedName);


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



}