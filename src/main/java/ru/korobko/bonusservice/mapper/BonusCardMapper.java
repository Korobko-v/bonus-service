package ru.korobko.bonusservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.korobko.bonusservice.dto.BonusCardDto;
import ru.korobko.bonusservice.model.BonusCard;

@Mapper(componentModel = "spring")
public interface BonusCardMapper {

    @Mapping(source = "active", target = "isActive")
    BonusCardDto toDto(BonusCard bonusCard);
    
    @Mapping(target = "transactions", ignore = true)
    BonusCard toEntity(BonusCardDto bonusCardDto);
}