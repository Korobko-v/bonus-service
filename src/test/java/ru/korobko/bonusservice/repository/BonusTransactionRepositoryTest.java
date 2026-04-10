package ru.korobko.bonusservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.korobko.bonusservice.model.BonusCard;
import ru.korobko.bonusservice.model.BonusTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.korobko.bonusservice.model.BonusTransaction.TransactionStatus.COMPLETED;
import static ru.korobko.bonusservice.model.BonusTransaction.TransactionType.ACCRUAL;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BonusTransactionRepositoryTest {

    @Autowired
    private BonusTransactionRepository bonusTransactionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private BonusCard card;
    private BonusTransaction transaction;
    private String transactionUuid;
    private String orderId = "ORD-001";

    @BeforeEach
    void setUp() {
        card = BonusCard.builder()
                .cardNumber("ACTIVE-001")
                .clientId(1L)
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();
        entityManager.persistAndFlush(card);

        transactionUuid = UUID.randomUUID().toString();
        transaction = BonusTransaction.builder()
                .transactionId(transactionUuid)
                .amount(100.0)
                .bonusCard(card)
                .description("Какая-то тестовая транзакция")
                .orderId(orderId)
                .status(COMPLETED)
                .type(ACCRUAL)
                .build();
        entityManager.persistAndFlush(transaction);
    }

    @Test
    void findByTransactionId_ShouldReturnTransaction() {
        Optional<BonusTransaction> found = bonusTransactionRepository
                .findByTransactionId(transactionUuid);

        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(orderId);
        assertThat(found.get().getAmount()).isEqualTo(100.0);
        assertThat(found.get().getDescription()).isEqualTo("Какая-то тестовая транзакция");
    }

    @Test
    void findByOrderId_ShouldReturnTransaction() {
        Optional<BonusTransaction> found = bonusTransactionRepository
                .findByOrderId(orderId);

        assertThat(found).isPresent();
        assertThat(found.get().getTransactionId()).isEqualTo(transactionUuid);
        assertThat(found.get().getAmount()).isEqualTo(100.0);
        assertThat(found.get().getDescription()).isEqualTo("Какая-то тестовая транзакция");
    }

    @Test
    void findHistoryByCardNumber_ShouldReturnList() {
        List<BonusTransaction> found = bonusTransactionRepository
                .findHistoryByCardNumber(card.getCardNumber());

        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getTransactionId()).isEqualTo(transactionUuid);
        assertThat(found.get(0).getAmount()).isEqualTo(100.0);
        assertThat(found.get(0).getDescription()).isEqualTo("Какая-то тестовая транзакция");
    }
}