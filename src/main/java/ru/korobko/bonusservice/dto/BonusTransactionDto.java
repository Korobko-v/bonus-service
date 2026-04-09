package ru.korobko.bonusservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.korobko.bonusservice.model.BonusTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusTransactionDto {
    private Long id;
    private String cardNumber;
    private BonusTransaction.TransactionType type;
    private BigDecimal amount;
    private String description;
    private String orderId;
    private String transactionId;
    private BonusTransaction.TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}