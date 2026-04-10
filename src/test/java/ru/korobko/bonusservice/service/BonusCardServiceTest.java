package ru.korobko.bonusservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.korobko.bonusservice.dto.BonusCardDto;
import ru.korobko.bonusservice.dto.request.CreateCardRequest;
import ru.korobko.bonusservice.exception.BonusCardNotFoundException;
import ru.korobko.bonusservice.exception.InvalidTransactionException;
import ru.korobko.bonusservice.mapper.BonusCardMapper;
import ru.korobko.bonusservice.model.BonusCard;
import ru.korobko.bonusservice.repository.BonusCardRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BonusCardServiceTest {

    @Mock
    private BonusCardRepository bonusCardRepository;

    @Mock
    private BonusCardMapper bonusCardMapper;

    @InjectMocks
    private BonusCardService bonusCardService;

    @Test
    void CreateCardTest() {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("TEST-001");
        request.setClientId(1L);
        request.setClientName("Test User");
        request.setInitialBalance(100.0);

        when(bonusCardRepository.existsByCardNumber("TEST-001")).thenReturn(false);

        BonusCard savedCard = BonusCard.builder()
                .id(1L)
                .cardNumber("TEST-001")
                .clientId(1L)
                .clientName("Test User")
                .balance(BigDecimal.valueOf(100))
                .isActive(true)
                .build();
        when(bonusCardRepository.save(any(BonusCard.class))).thenReturn(savedCard);

        BonusCardDto dto = BonusCardDto.builder()
                .id(1L)
                .cardNumber("TEST-001")
                .clientId(1L)
                .clientName("Test User")
                .balance(BigDecimal.valueOf(100))
                .isActive(true)
                .build();
        when(bonusCardMapper.toDto(savedCard)).thenReturn(dto);

        BonusCardDto result = bonusCardService.createCard(request);

        assertThat(result).isNotNull();
        assertThat(result.getCardNumber()).isEqualTo("TEST-001");
        assertThat(result.isActive()).isTrue();
        verify(bonusCardRepository).save(any(BonusCard.class));
    }

    @Test
    void createCardWithDuplicatedNumberThenThrowsExceptionTest() {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("DUPLICATE");
        when(bonusCardRepository.existsByCardNumber("DUPLICATE")).thenReturn(true);

        assertThatThrownBy(() -> bonusCardService.createCard(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("уже существует");

        verify(bonusCardRepository, never()).save(any());
    }

    @Test
    void deactivateCardTest() {
        Long cardId = 1L;
        BonusCard card = BonusCard.builder().id(cardId).isActive(true).build();
        when(bonusCardRepository.findById(cardId)).thenReturn(Optional.of(card));

        bonusCardService.deactivateCard(cardId);

        assertThat(card.isActive()).isFalse();
    }

    @Test
    void deactivateAlreadyDeactivatedCardThenThrowsExceptionTest() {
        Long cardId = 1L;
        BonusCard card = BonusCard.builder().id(cardId).isActive(false).build();
        when(bonusCardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> bonusCardService.deactivateCard(cardId))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("уже деактивирована");

        verify(bonusCardRepository, never()).save(any());
    }

    @Test
    void deactivateCardNotFoundThenThrowsExceptionTest() {
        Long cardId = 999L;
        when(bonusCardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonusCardService.deactivateCard(cardId))
                .isInstanceOf(BonusCardNotFoundException.class);
    }
}