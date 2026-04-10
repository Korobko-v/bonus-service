package ru.korobko.bonusservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.korobko.bonusservice.model.BonusTransaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface BonusTransactionRepository extends JpaRepository<BonusTransaction, Long> {
    
    Optional<BonusTransaction> findByTransactionId(String transactionId);

    Optional<BonusTransaction> findByOrderId(String orderId);

    @Query("SELECT t FROM BonusTransaction t WHERE t.bonusCard.cardNumber = :cardNumber " +
           "ORDER BY t.createdAt DESC")
    List<BonusTransaction> findHistoryByCardNumber(@Param("cardNumber") String cardNumber);
}