package com.example.DebitCopybook.model.mapper;

import com.example.DebitCopybook.dao.entity.DebtHistoryEntity;
import com.example.DebitCopybook.model.response.DebtHistoryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring") // Spring-in bu mapper-i tanıması üçün vacibdir
public interface DebtHistoryMapper {

    // Bu metod bir dənə Entity-ni bir dənə DTO-ya çevirir
    // Enum-u String-ə çevirmək üçün "eventType" sahəsini xüsusi olaraq qeyd edirik
    @Mapping(target = "eventType", expression = "java(entity.getEventType().name())")
    DebtHistoryResponseDto toDto(DebtHistoryEntity entity);

    // Bu metod Entity siyahısını DTO siyahısına çevirir
    List<DebtHistoryResponseDto> toDtoList(List<DebtHistoryEntity> entities);
}