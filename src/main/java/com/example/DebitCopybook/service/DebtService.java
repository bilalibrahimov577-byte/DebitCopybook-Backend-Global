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
//import java.time.ZoneId;
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
    private static final String DEFAULT_DEBT_TYPE = "Növü təyin edilməyib";
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

        int debtLimit = currentUser.isAdmin() ? 100 : 15;
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


        final String MY_DEBT_VALUE = "mənim borcum";
        final String DEBT_TO_ME_VALUE = "mənə olan borclar";

        String description = requestDto.getDescription();


        if (!MY_DEBT_VALUE.equals(description) && !DEBT_TO_ME_VALUE.equals(description)) {

            requestDto.setDescription("Zəhmət olmasa play marketdə yenilenme olub-olmadigini yoxlayin");
        }


        DebtEntity debtEntity = debtMapper.mapRequestDtoToEntity(requestDto);
        debtEntity.setUser(currentUser);


        DebtEntity savedEntity = debtRepository.save(debtEntity);


        DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
                .debt(savedEntity)
                .eventType(HistoryEventType.CREATED)
                .description("Borc yaradıldı.")
                .amount(savedEntity.getDebtAmount())

                .eventDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
                .build();

        debtHistoryRepository.save(historyEntry);

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
        DebtEntity existingEntity = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));

        BigDecimal currentDebt = existingEntity.getDebtAmount();

        if (paymentAmount.compareTo(currentDebt) > 0) {
            throw new IllegalArgumentException("Ödəniş məbləği (" + paymentAmount + " AZN) mövcud borcdan (" + currentDebt + " AZN) çox ola bilməz.");
        }


        String description = paymentAmount + " AZN ödəniş edildi.";
        DebtHistoryEntity paymentHistoryEntry = DebtHistoryEntity.builder()
                .debt(existingEntity)
                .eventType(HistoryEventType.PAYMENT)
                .description(description)
                .amount(paymentAmount.negate())

                .eventDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
                .build();
        debtHistoryRepository.save(paymentHistoryEntry);


        BigDecimal newDebt = currentDebt.subtract(paymentAmount);


        if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {



            DebtHistoryEntity closingHistoryEntry = DebtHistoryEntity.builder()
                    .debt(existingEntity)
                    .eventType(HistoryEventType.UPDATED)
                    .description("Borc tam ödənildi və bütün məlumatlar silindi.")

                    .eventDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
                    .build();
            debtHistoryRepository.save(closingHistoryEntry);


            List<DebtHistoryEntity> allHistoryOfThisDebt = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(id);

            if (!allHistoryOfThisDebt.isEmpty()) {
                debtHistoryRepository.deleteAll(allHistoryOfThisDebt);
            }


            debtRepository.delete(existingEntity);


            return DebtResponseDto.builder()
                    .id(id)
                    .debtorName(existingEntity.getDebtorName())
                    .debtAmount(BigDecimal.ZERO)
                    .notes("Borc tam ödənildi və bütün qeydlər silindi.")
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


        DebtEntity debtToDelete = debtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));


        List<DebtHistoryEntity> historyToDelete = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(id);


        if (!historyToDelete.isEmpty()) {
            debtHistoryRepository.deleteAll(historyToDelete);
        }


        debtRepository.delete(debtToDelete);
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



        if (requestDto.getDescription() != null) {
            final String MY_DEBT_VALUE = "mənim borcum";
            final String DEBT_TO_ME_VALUE = "mənə olan borclar";
            String newDescription = requestDto.getDescription();


            if (!MY_DEBT_VALUE.equals(newDescription) && !DEBT_TO_ME_VALUE.equals(newDescription)) {
                requestDto.setDescription("Zəhmət olmasa play marketdə yenilenme olub-olmadigini yoxlayin");
            }
        }



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
            String oldNotesText = (oldNotes == null || oldNotes.isBlank()) ? "[boş]" : "'" + oldNotes + "'";
            String newNotesText = (existingEntity.getNotes() == null || existingEntity.getNotes().isBlank()) ? "[boş]" : "'" + existingEntity.getNotes() + "'";
            changes.add("Qeyd " + oldNotesText + "-dan " + newNotesText + "-a dəyişdirildi.");
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
                   // .eventDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
                    .eventDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
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


        BigDecimal oldAmount = existingEntity.getDebtAmount();


        BigDecimal newDebtAmount = oldAmount.add(amountToAdd);
        existingEntity.setDebtAmount(newDebtAmount);

        DebtEntity updatedEntity = debtRepository.save(existingEntity);



        String description = "Borc " + oldAmount + " AZN-dən " + newDebtAmount + " AZN-ə artırıldı (" + amountToAdd + " AZN əlavə edildi).";

        DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
                .debt(updatedEntity)
                .eventType(HistoryEventType.UPDATED) // Bu da bir növ yeniləmədir
                .description(description)
                .amount(amountToAdd)
                .eventDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
                .build();

        debtHistoryRepository.save(historyEntry);



        return debtMapper.mapEntityToResponseDto(updatedEntity);
    }




    public List<DebtHistoryResponseDto> getDebtHistory(Long debtId) {
        Long userId = getCurrentUserId();


        debtRepository.findByIdAndUserId(debtId, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + debtId + " ilə tapılmadı və ya bu istifadəçiyə aid deyil."));


        List<DebtHistoryEntity> historyEntities = debtHistoryRepository.findAllByDebtIdOrderByEventDateDesc(debtId);


        return debtHistoryMapper.toDtoList(historyEntities);
    }



    public List<DebtResponseDto> getDebtsByDescription(String description) {
        Long userId = getCurrentUserId();

        List<DebtEntity> debtEntities = debtRepository.findByDescriptionAndUserId(description, userId);

        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }




}