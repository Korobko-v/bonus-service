package ru.korobko.bonusservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import ru.korobko.bonusservice.dto.BonusTransactionDto;
import ru.korobko.bonusservice.dto.request.RefundRequest;
import ru.korobko.bonusservice.dto.request.TransactionRequest;
import ru.korobko.bonusservice.exception.AccessException;
import ru.korobko.bonusservice.exception.BonusCardNotFoundException;
import ru.korobko.bonusservice.exception.InsufficientBonusException;
import ru.korobko.bonusservice.exception.InvalidTransactionException;
import ru.korobko.bonusservice.mapper.BonusTransactionMapper;
import ru.korobko.bonusservice.model.BonusCard;
import ru.korobko.bonusservice.model.BonusTransaction;
import ru.korobko.bonusservice.model.User;
import ru.korobko.bonusservice.repository.BonusCardRepository;
import ru.korobko.bonusservice.repository.BonusTransactionRepository;
import ru.korobko.bonusservice.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BonusServiceTest {

    @Mock
    private BonusCardRepository bonusCardRepository;

    @Mock
    private BonusTransactionRepository bonusTransactionRepository;

    @Mock
    private BonusTransactionMapper bonusTransactionMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BonusService bonusService;

    private User currentUser;
    private BonusCard testCard;
    private Authentication authentication;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("admin")
                .role("ADMIN")
                .build();

        testCard = BonusCard.builder()
                .id(1L)
                .cardNumber("1234-5678")
                .clientId(2L) // не равен currentUser.id
                .balance(BigDecimal.valueOf(100))
                .isActive(true)
                .build();

        // Настраиваем SecurityContext
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("admin");
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accrueBonusTest() {
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234-5678");
        request.setAmount(50.0);
        request.setOrderId("ORDER-1");
        request.setDescription("Test");

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));
        when(bonusTransactionRepository.findByOrderId("ORDER-1")).thenReturn(Optional.empty());
        when(bonusTransactionRepository.save(any(BonusTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(bonusCardRepository.save(any(BonusCard.class))).thenReturn(testCard);
        when(bonusTransactionMapper.toDto(any(BonusTransaction.class))).thenReturn(BonusTransactionDto.builder().build());

        BonusTransactionDto result = bonusService.accrueBonus(request);

        assertThat(result).isNotNull();
        assertThat(testCard.getBalance()).isEqualTo(BigDecimal.valueOf(150.0));
        verify(bonusTransactionRepository).save(any(BonusTransaction.class));
        verify(bonusCardRepository).save(testCard);
    }

    @Test
    void accrueBonusSelfAccrualThenThrowsAccessExceptionTest() {
        testCard.setClientId(currentUser.getId());
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234-5678");

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> bonusService.accrueBonus(request))
                .isInstanceOf(AccessException.class)
                .hasMessageContaining("сам себе");
    }

    @Test
    void accrueBonusAlreadyProcessedThenThrowsInvalidTransactionExceptionTest() {
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234-5678");
        request.setOrderId("ORDER-1");

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));
        when(bonusTransactionRepository.findByOrderId("ORDER-1"))
                .thenReturn(Optional.of(mock(BonusTransaction.class)));

        assertThatThrownBy(() -> bonusService.accrueBonus(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("уже начислены");
    }

    @Test
    void writeOffBonusTest() {
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234-5678");
        request.setAmount(30.0);
        request.setOrderId("ORDER-2");

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));
        when(bonusTransactionRepository.findByOrderId("ORDER-2")).thenReturn(Optional.empty());
        when(bonusTransactionRepository.save(any(BonusTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(bonusCardRepository.save(any(BonusCard.class))).thenReturn(testCard);
        when(bonusTransactionMapper.toDto(any(BonusTransaction.class))).thenReturn(BonusTransactionDto.builder().build());

        BonusTransactionDto result = bonusService.writeOffBonus(request);

        assertThat(result).isNotNull();
        assertThat(testCard.getBalance()).isEqualTo(BigDecimal.valueOf(70.0));
    }

    @Test
    void writeOffBonusInsufficientBalanceThenThrowsInsufficientBonusExceptionTest() {
        testCard.setBalance(BigDecimal.valueOf(10));
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234-5678");
        request.setAmount(30.0);

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> bonusService.writeOffBonus(request))
                .isInstanceOf(InsufficientBonusException.class);
    }

    @Test
    void writeOffBonusAlreadyProcessedThenReturnsExistingDto() {
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234-5678");
        request.setOrderId("ORDER-2");

        BonusTransaction existingTx = BonusTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .build();
        BonusTransactionDto existingDto = BonusTransactionDto.builder()
                .transactionId(existingTx.getTransactionId())
                .build();

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));
        when(bonusTransactionRepository.findByOrderId("ORDER-2")).thenReturn(Optional.of(existingTx));
        when(bonusTransactionMapper.toDto(existingTx)).thenReturn(existingDto);

        BonusTransactionDto result = bonusService.writeOffBonus(request);

        assertThat(result).isSameAs(existingDto);
        verify(bonusTransactionRepository, never()).save(any());
        verify(bonusCardRepository, never()).save(any());
    }

    @Test
    void refundBonusSuccessForWriteOffTest() {
        RefundRequest request = new RefundRequest();
        request.setCardNumber("1234-5678");
        request.setOriginalTransactionId("tx-1");
        request.setOrderId("ORDER-3");

        BonusTransaction originalTx = BonusTransaction.builder()
                .id(1L)
                .transactionId("tx-1")
                .bonusCard(testCard)
                .type(BonusTransaction.TransactionType.WRITE_OFF)
                .amount(50.0)
                .status(BonusTransaction.TransactionStatus.COMPLETED)
                .build();

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));
        when(bonusTransactionRepository.findByOrderId("ORDER-3")).thenReturn(Optional.empty());
        when(bonusTransactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(originalTx));
        when(bonusTransactionRepository.save(any(BonusTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(bonusCardRepository.save(any(BonusCard.class))).thenReturn(testCard);
        when(bonusTransactionMapper.toDto(any(BonusTransaction.class))).thenReturn(BonusTransactionDto.builder().build());

        BonusTransactionDto result = bonusService.refundBonus(request);

        assertThat(result).isNotNull();
        assertThat(testCard.getBalance()).isEqualTo(BigDecimal.valueOf(150.0));
    }

    @Test
    void refundBonusSelfRefundThenThrowsAccessExceptionTest() {
        testCard.setClientId(currentUser.getId());
        RefundRequest request = new RefundRequest();
        request.setCardNumber("1234-5678");

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> bonusService.refundBonus(request))
                .isInstanceOf(AccessException.class)
                .hasMessageContaining("сам себе");
    }

    @Test
    void refundBonusAlreadyRefundedThenThrowsInvalidTransactionExceptionTest() {
        RefundRequest request = new RefundRequest();
        request.setCardNumber("1234-5678");
        request.setOriginalTransactionId("tx-1");

        BonusTransaction originalTx = BonusTransaction.builder()
                .id(1L)
                .bonusCard(testCard)
                .status(BonusTransaction.TransactionStatus.REFUND)
                .build();

        when(bonusCardRepository.findByCardNumberAndIsActiveIsTrue("1234-5678"))
                .thenReturn(Optional.of(testCard));
        when(bonusTransactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(originalTx));

        assertThatThrownBy(() -> bonusService.refundBonus(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Повторный возврат невозможен");
    }

    @Test
    void getBalanceForAdminTest() {
        when(bonusCardRepository.findBalanceByCardNumber("1234-5678"))
                .thenReturn(Optional.of(BigDecimal.valueOf(100)));

        BigDecimal balance = bonusService.getBalanceForAdmin("1234-5678");
        assertThat(balance).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void getBalanceForAdminCardNotFoundThenThrowsBonusCardNotFoundExceptionTest() {
        when(bonusCardRepository.findBalanceByCardNumber("1234-5678"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonusService.getBalanceForAdmin("1234-5678"))
                .isInstanceOf(BonusCardNotFoundException.class);
    }

    @Test
    void getMyBalanceTest() {
        when(bonusCardRepository.findBalanceByCardNumberAndClientId("1234-5678", currentUser.getId()))
                .thenReturn(Optional.of(BigDecimal.valueOf(100)));

        BigDecimal balance = bonusService.getMyBalance("1234-5678");
        assertThat(balance).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void getMyBalanceCardNotFoundOrNotOwnedThenThrowsBonusCardNotFoundExceptionTest() {
        when(bonusCardRepository.findBalanceByCardNumberAndClientId("1234-5678", currentUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonusService.getMyBalance("1234-5678"))
                .isInstanceOf(BonusCardNotFoundException.class);
    }

    @Test
    void getTransactionHistoryForAdminTest() {
        when(bonusCardRepository.existsByCardNumber("1234-5678")).thenReturn(true);
        when(bonusTransactionRepository.findHistoryByCardNumber("1234-5678"))
                .thenReturn(List.of(mock(BonusTransaction.class)));
        when(bonusTransactionMapper.toDto(any(BonusTransaction.class))).thenReturn(mock(BonusTransactionDto.class));

        List<BonusTransactionDto> history = bonusService.getTransactionHistoryForAdmin("1234-5678");
        assertThat(history).hasSize(1);
    }

    @Test
    void getTransactionHistoryForAdminCardNotFoundThenThrowsBonusCardNotFoundExceptionTest() {
        when(bonusCardRepository.existsByCardNumber("1234-5678")).thenReturn(false);

        assertThatThrownBy(() -> bonusService.getTransactionHistoryForAdmin("1234-5678"))
                .isInstanceOf(BonusCardNotFoundException.class);
    }

    @Test
    void getMyTransactionHistoryTest() {
        when(bonusCardRepository.existsByCardNumberAndIsActiveIsTrueAndClientId("1234-5678", currentUser.getId()))
                .thenReturn(true);
        when(bonusTransactionRepository.findHistoryByCardNumber("1234-5678"))
                .thenReturn(List.of(mock(BonusTransaction.class)));
        when(bonusTransactionMapper.toDto(any(BonusTransaction.class))).thenReturn(mock(BonusTransactionDto.class));

        List<BonusTransactionDto> history = bonusService.getMyTransactionHistory("1234-5678");
        assertThat(history).hasSize(1);
    }

    @Test
    void getMyTransactionHistoryAccessDeniedThenThrowsAccessExceptionTest() {
        when(bonusCardRepository.existsByCardNumberAndIsActiveIsTrueAndClientId("1234-5678", currentUser.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> bonusService.getMyTransactionHistory("1234-5678"))
                .isInstanceOf(AccessException.class);
    }
}