package ru.korobko.bonusservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.korobko.bonusservice.dto.BonusTransactionDto;
import ru.korobko.bonusservice.model.BonusTransaction;

@Mapper(componentModel = "spring")
public interface BonusTransactionMapper {
    
    @Mapping(source = "bonusCard.cardNumber", target = "cardNumber")
    BonusTransactionDto toDto(BonusTransaction bonusTransaction);
}