package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.repository.DebtRepository;
import com.example.DebitCopybook.exception.DebtNotFoundException;
import com.example.DebitCopybook.model.mapper.DebtMapper;
import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtMapper debtMapper;



    @Transactional
    public DebtResponseDto createDebt(DebtRequestDto requestDto) {

        if (requestDto.getIsFlexibleDueDate() != null && requestDto.getIsFlexibleDueDate()) {
            requestDto.setDueYear(null);
            requestDto.setDueMonth(null);
        }

        DebtEntity debtEntity = debtMapper.mapRequestDtoToEntity(requestDto);
        DebtEntity savedEntity = debtRepository.save(debtEntity);
        return debtMapper.mapEntityToResponseDto(savedEntity);
    }

    public List<DebtResponseDto> getAllDebts() {
        List<DebtEntity> debtEntities = debtRepository.findAll();
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    public DebtResponseDto getDebtById(Long id) {
        DebtEntity debtEntity = debtRepository.findById(id)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı."));
        return debtMapper.mapEntityToResponseDto(debtEntity);
    }

    @Transactional
    public DebtResponseDto updateDebt(Long id, DebtRequestDto requestDto) {
        DebtEntity existingEntity = debtRepository.findById(id)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı."));


        if (requestDto.getIsFlexibleDueDate() != null && requestDto.getIsFlexibleDueDate()) {
            requestDto.setDueYear(null);
            requestDto.setDueMonth(null);
        } else if (requestDto.getIsFlexibleDueDate() != null && !requestDto.getIsFlexibleDueDate()){

            if (requestDto.getDueYear() == null || requestDto.getDueMonth() == null) {
                throw new IllegalArgumentException("Konkret tarix üçün il və ay qeyd olunmalıdır.");
            }
        }

        debtMapper.updateEntityFromRequestDto(requestDto, existingEntity);
        DebtEntity updatedEntity = debtRepository.save(existingEntity);
        return debtMapper.mapEntityToResponseDto(updatedEntity);
    }

    @Transactional
    public DebtResponseDto makePayment(Long id, BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ödəniş məbləği müsbət olmalıdır.");
        }

        DebtEntity existingEntity = debtRepository.findById(id)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı."));

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
                    .build();
        } else {
            existingEntity.setDebtAmount(newDebt);
            DebtEntity updatedEntity = debtRepository.save(existingEntity);
            return debtMapper.mapEntityToResponseDto(updatedEntity);
        }
    }

    @Transactional
    public void deleteDebt(Long id) {
        if (!debtRepository.existsById(id)) {
            throw new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı.");
        }
        debtRepository.deleteById(id);
    }


    public List<DebtResponseDto> getDebtsByYearAndMonth(Integer year, Integer month) {

        if (year == null || month == null) {
            throw new IllegalArgumentException("Borcları il və aya görə axtarmaq üçün hər ikisi qeyd olunmalıdır.");
        }
        List<DebtEntity> debtEntities = debtRepository.findByDueYearAndDueMonth(year, month);
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }


    public List<DebtResponseDto> getFlexibleDueDateDebts() {
        List<DebtEntity> debtEntities = debtRepository.findByIsFlexibleDueDateTrueOrderByIdAsc();
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }

    public List<DebtResponseDto> searchDebtsByDebtorName(String debtorName) {
        if (debtorName == null || debtorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Axtarış üçün borcalanın adı boş ola bilməz.");
        }
        List<DebtEntity> debtEntities = debtRepository.findByDebtorNameContainingIgnoreCase(debtorName);
        return debtMapper.mapEntityListToResponseDtoList(debtEntities);
    }


}