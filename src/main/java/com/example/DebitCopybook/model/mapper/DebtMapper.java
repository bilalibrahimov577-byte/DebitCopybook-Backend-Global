package com.example.DebitCopybook.model.mapper;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import com.example.DebitCopybook.model.response.UserDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DebtMapper {

    // KÖMƏKÇİ METOD: UserEntity -> UserDto (Null-dan qorunmuş)
    // Bu metod UserEntity obyektini frontend üçün təhlükəsiz UserDto-ya çevirir.
    private UserDto mapUserEntityToUserDto(UserEntity userEntity) {
        // 1. Əgər userEntity obyekti özü yoxdursa (null-dırsa),
        //    frontend-in çökməməsi üçün standart məlumatlarla dolu bir obyekt qaytarırıq.
        if (userEntity == null) {
            return new UserDto(0L, "Naməlum İstifadəçi", "email@yoxdur.com", "00-00");
        }

        // 2. Hər bir sahəni ayrı-ayrılıqda yoxlayırıq.
        //    Əgər hər hansı bir sahə boş (null və ya ancaq boşluqlardan ibarət) gələrsə,
        //    ona standart bir dəyər mənimsədirik.
        String name = StringUtils.hasText(userEntity.getName()) ? userEntity.getName() : "Naməlum İstifadəçi";
        String email = StringUtils.hasText(userEntity.getEmail()) ? userEntity.getEmail() : "email@yoxdur.com";
        String debtId = StringUtils.hasText(userEntity.getDebtId()) ? userEntity.getDebtId() : "00-00";

        return new UserDto(userEntity.getId(), name, email, debtId);
    }

    // ƏSAS ÇEVİRMƏ METODU: DebtEntity -> DebtResponseDto
    public DebtResponseDto mapEntityToResponseDto(DebtEntity entity) {
        if (entity == null) {
            return null;
        }

        // statusu Enum tipindən String tipinə təhlükəsiz şəkildə çeviririk.
        // null olarsa, "UNKNOWN" kimi gedəcək.
        String status = (entity.getStatus() != null) ? entity.getStatus().name() : "UNKNOWN";

        return DebtResponseDto.builder()
                .id(entity.getId())
                .debtorName(StringUtils.hasText(entity.getDebtorName()) ? entity.getDebtorName() : "Adsız")
                .description(entity.getDescription()) // Bu sahənin null olması normaldır
                .debtAmount(entity.getDebtAmount())
                .createdAt(entity.getCreatedAt())
                .dueYear(entity.getDueYear())
                .dueMonth(entity.getDueMonth())
                .isFlexibleDueDate(entity.getIsFlexibleDueDate())
                .notes(entity.getNotes())

                // YENİ DOLDURULAN SAHƏLƏR
                .status(status)
                .requestExpiryTime(entity.getRequestExpiryTime())
                .user(mapUserEntityToUserDto(entity.getUser())) // Köməkçi metodumuzu istifadə edirik
                .counterpartyUser(mapUserEntityToUserDto(entity.getCounterpartyUser())) // Köməkçi metodumuzu istifadə edirik
                .build();
    }

    // SİYAHI ÜÇÜN ÇEVİRMƏ METODU
    public List<DebtResponseDto> mapEntityListToResponseDtoList(List<DebtEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::mapEntityToResponseDto) // Hər bir elementi yuxarıdakı metodla çevirir
                .collect(Collectors.toList());
    }

    // BU METODLAR REQUEST-ləri idarə etdiyi üçün olduğu kimi qalır.
    // Onlar məlumatı qəbul edir, göndərmir.
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

    public void updateEntityFromRequestDto(DebtRequestDto requestDto, DebtEntity entity) {
        if (requestDto == null || entity == null) {
            return;
        }
        if (requestDto.getDebtorName() != null) entity.setDebtorName(requestDto.getDebtorName());
        if (requestDto.getDescription() != null) entity.setDescription(requestDto.getDescription());
        if (requestDto.getDebtAmount() != null) entity.setDebtAmount(requestDto.getDebtAmount());
        if (requestDto.getDueYear() != null) entity.setDueYear(requestDto.getDueYear());
        if (requestDto.getDueMonth() != null) entity.setDueMonth(requestDto.getDueMonth());
        if (requestDto.getIsFlexibleDueDate() != null) entity.setIsFlexibleDueDate(requestDto.getIsFlexibleDueDate());
        if (requestDto.getNotes() != null) entity.setNotes(requestDto.getNotes());
    }
}