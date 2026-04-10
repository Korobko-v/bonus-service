package ru.korobko.bonusservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korobko.bonusservice.dto.BonusCardDto;
import ru.korobko.bonusservice.dto.request.CreateCardRequest;
import ru.korobko.bonusservice.exception.BonusCardNotFoundException;
import ru.korobko.bonusservice.exception.InvalidTransactionException;
import ru.korobko.bonusservice.mapper.BonusCardMapper;
import ru.korobko.bonusservice.model.BonusCard;
import ru.korobko.bonusservice.repository.BonusCardRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class BonusCardService {

    private final BonusCardRepository bonusCardRepository;
    private final BonusCardMapper bonusCardMapper;

    @Transactional
    public BonusCardDto createCard(CreateCardRequest request) {
        log.info("Создание новой карты: {}", request.getCardNumber());

        if (bonusCardRepository.existsByCardNumber(request.getCardNumber())) {
            throw new InvalidTransactionException("Карта с таким номером уже существует");
        }

        BonusCard card = BonusCard.builder()
                .cardNumber(request.getCardNumber())
                .clientId(request.getClientId())
                .clientName(request.getClientName())
                .balance(BigDecimal.valueOf(request.getInitialBalance()))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .transactions(new ArrayList<>())
                .build();

        BonusCard savedCard = bonusCardRepository.save(card);
        log.info("Карта {} успешно создана", savedCard.getCardNumber());

        return bonusCardMapper.toDto(savedCard);
    }

    @Transactional
    public void deactivateCard(Long id) {
        BonusCard card = bonusCardRepository.findById(id)
                .orElseThrow(() -> new BonusCardNotFoundException("Карта не найдена, ID: " + id));

        if (!card.isActive()) {
            throw new InvalidTransactionException("Карта уже деактивирована");
        }

        card.setActive(false);
    }
}
