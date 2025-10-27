package com.example.DebitCopybook.model.mapper;

import com.example.DebitCopybook.dao.entity.DebtHistoryEntity;
import com.example.DebitCopybook.model.response.DebtHistoryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DebtHistoryMapper {


    @Mapping(target = "eventType", expression = "java(entity.getEventType().name())")
    DebtHistoryResponseDto toDto(DebtHistoryEntity entity);


    List<DebtHistoryResponseDto> toDtoList(List<DebtHistoryEntity> entities);
}