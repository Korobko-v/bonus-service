package ru.korobko.bonusservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.korobko.bonusservice.model.BonusCard;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BonusCardRepositoryTest {

    @Autowired
    private BonusCardRepository bonusCardRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByCardNumberAndIsActiveIsTrueTest() {
        BonusCard activeCard = BonusCard.builder()
                .cardNumber("ACTIVE-001")
                .clientId(1L)
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();
        entityManager.persist(activeCard);

        BonusCard inactiveCard = BonusCard.builder()
                .cardNumber("INACTIVE-001")
                .clientId(2L)
                .balance(BigDecimal.ZERO)
                .isActive(false)
                .build();
        entityManager.persist(inactiveCard);
        entityManager.flush();

        Optional<BonusCard> found = bonusCardRepository.findByCardNumberAndIsActiveIsTrue("ACTIVE-001");
        assertThat(found).isPresent();
        assertThat(found.get().getCardNumber()).isEqualTo("ACTIVE-001");
        assertThat(found.get().isActive()).isTrue();


        Optional<BonusCard> notFound = bonusCardRepository.findByCardNumberAndIsActiveIsTrue("INACTIVE-001");
        assertThat(notFound).isEmpty();
    }
}