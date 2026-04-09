package ru.korobko.bonusservice.repository;

import org.hibernate.annotations.Where;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.korobko.bonusservice.model.BonusCard;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
@Where(clause = "is_active = true")
public interface BonusCardRepository extends JpaRepository<BonusCard, Long> {


    boolean existsByCardNumber(String cardNumber);

    Optional<BonusCard> findByCardNumber(String cardNumber);


    @Query("SELECT c.balance FROM BonusCard c WHERE c.cardNumber = :cardNumber")
    Optional<BigDecimal> findBalanceByCardNumber(@Param("cardNumber") String cardNumber);

}
