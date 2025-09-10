package com.example.DebitCopybook.model.mapper;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DebtMapper {

    public DebtEntity mapRequestDtoToEntity(DebtRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        return DebtEntity.builder()
                .debtorName(requestDto.getDebtorName())
                .description(requestDto.getDescription())
                .debtAmount(requestDto.getDebtAmount())
                .dueYear(requestDto.getDueYear())
                .dueMonth(requestDto.getDueMonth())
                .isFlexibleDueDate(requestDto.getIsFlexibleDueDate() != null ? requestDto.getIsFlexibleDueDate() : false)
                .notes(requestDto.getNotes())
                .build();
    }

    public DebtResponseDto mapEntityToResponseDto(DebtEntity entity) {
        if (entity == null) {
            return null;
        }
        return DebtResponseDto.builder()
                .id(entity.getId())
                .debtorName(entity.getDebtorName())
                .description(entity.getDescription())
                .debtAmount(entity.getDebtAmount())
                .createdAt(entity.getCreatedAt())
                .dueYear(entity.getDueYear())
                .dueMonth(entity.getDueMonth())
                .isFlexibleDueDate(entity.getIsFlexibleDueDate())
                .notes(entity.getNotes())
                .userId(entity.getUser().getId()) // BILAL, bu xətt əlavə edilməsi MÜTLƏQDİR.
                // Borcun hansı istifadəçiyə aid olduğunu DTO-ya əlavə edirik.
                .build();
    }

    public void updateEntityFromRequestDto(DebtRequestDto requestDto, DebtEntity entity) {
        if (requestDto == null || entity == null) {
            return;
        }

        if (requestDto.getDebtorName() != null) {
            entity.setDebtorName(requestDto.getDebtorName());
        }
        if (requestDto.getDescription() != null) {
            entity.setDescription(requestDto.getDescription());
        }
        if (requestDto.getDebtAmount() != null) {
            entity.setDebtAmount(requestDto.getDebtAmount());
        }
        if (requestDto.getDueYear() != null) {
            entity.setDueYear(requestDto.getDueYear());
        }
        if (requestDto.getDueMonth() != null) {
            entity.setDueMonth(requestDto.getDueMonth());
        }
        if (requestDto.getIsFlexibleDueDate() != null) {
            entity.setIsFlexibleDueDate(requestDto.getIsFlexibleDueDate());
        } else {
            // Əgər isFlexibleDueDate RequestDto-da null gəlirsə, entity-nin mövcud dəyərini saxla
        }

        if (requestDto.getNotes() != null) {
            entity.setNotes(requestDto.getNotes());
        }
    }

    public List<DebtResponseDto> mapEntityListToResponseDtoList(List<DebtEntity> entities) {
        return entities.stream()
                .map(this::mapEntityToResponseDto)
                .collect(Collectors.toList());
    }
}