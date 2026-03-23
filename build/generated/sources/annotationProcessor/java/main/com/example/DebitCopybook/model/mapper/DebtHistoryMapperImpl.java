package com.example.DebitCopybook.model.mapper;

import com.example.DebitCopybook.dao.entity.DebtHistoryEntity;
import com.example.DebitCopybook.model.response.DebtHistoryResponseDto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-23T22:19:56+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class DebtHistoryMapperImpl implements DebtHistoryMapper {

    @Override
    public DebtHistoryResponseDto toDto(DebtHistoryEntity entity) {
        if ( entity == null ) {
            return null;
        }

        DebtHistoryResponseDto.DebtHistoryResponseDtoBuilder debtHistoryResponseDto = DebtHistoryResponseDto.builder();

        debtHistoryResponseDto.id( entity.getId() );
        debtHistoryResponseDto.description( entity.getDescription() );
        debtHistoryResponseDto.amount( entity.getAmount() );
        debtHistoryResponseDto.eventDate( entity.getEventDate() );

        debtHistoryResponseDto.eventType( entity.getEventType().name() );

        return debtHistoryResponseDto.build();
    }

    @Override
    public List<DebtHistoryResponseDto> toDtoList(List<DebtHistoryEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<DebtHistoryResponseDto> list = new ArrayList<DebtHistoryResponseDto>( entities.size() );
        for ( DebtHistoryEntity debtHistoryEntity : entities ) {
            list.add( toDto( debtHistoryEntity ) );
        }

        return list;
    }
}
