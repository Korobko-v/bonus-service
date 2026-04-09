package ru.korobko.bonusservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korobko.bonusservice.dto.BonusTransactionDto;
import ru.korobko.bonusservice.dto.request.RefundRequest;
import ru.korobko.bonusservice.dto.request.TransactionRequest;
import ru.korobko.bonusservice.exception.BonusCardNotFoundException;
import ru.korobko.bonusservice.exception.InsufficientBonusException;
import ru.korobko.bonusservice.exception.InvalidTransactionException;
import ru.korobko.bonusservice.mapper.BonusTransactionMapper;
import ru.korobko.bonusservice.model.BonusCard;
import ru.korobko.bonusservice.model.BonusTransaction;
import ru.korobko.bonusservice.repository.BonusCardRepository;
import ru.korobko.bonusservice.repository.BonusTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BonusService {
    
    private final BonusCardRepository bonusCardRepository;
    private final BonusTransactionRepository bonusTransactionRepository;
    private final BonusTransactionMapper bonusTransactionMapper;
    
    @Transactional
    public BonusTransactionDto accrueBonus(TransactionRequest request) {
        log.info("Начисление бонусов на карту: {}, сумма: {}", request.getCardNumber(), request.getAmount());
        
        BonusCard card = findActiveCard(request.getCardNumber());

        BonusTransaction transaction = createTransaction(
                card,
                BonusTransaction.TransactionType.ACCRUAL,
                request.getAmount(),
                request.getDescription(),
                request.getOrderId()
        );

        card.setBalance(card.getBalance() + request.getAmount());
        bonusCardRepository.save(card);

        BonusTransaction savedTransaction = bonusTransactionRepository.save(transaction);
        
        log.info("Бонусы начислены успешно. Баланс: {}", card.getBalance());
        return bonusTransactionMapper.toDto(savedTransaction);
    }
    
    @Transactional
    public BonusTransactionDto writeOffBonus(TransactionRequest request) {
        log.info("Списание бонусов с карты: {}, сумма: {}", request.getCardNumber(), request.getAmount());
        
        BonusCard card = findActiveCard(request.getCardNumber());

        if (card.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBonusException(
                    String.format("Недостаточно бонусов. Доступно: %s, Запрошено: %s",
                            card.getBalance(), request.getAmount())
            );
        }

        BonusTransaction transaction = createTransaction(
                card,
                BonusTransaction.TransactionType.WRITE_OFF,
                request.getAmount(),
                request.getDescription(),
                request.getOrderId()
        );

        card.setBalance(card.getBalance() - request.getAmount());
        bonusCardRepository.save(card);

        BonusTransaction savedTransaction = bonusTransactionRepository.save(transaction);
        
        log.info("Бонусы списаны успешно. Баланс: {}", card.getBalance());
        return bonusTransactionMapper.toDto(savedTransaction);
    }
    
    @Transactional
    public BonusTransactionDto refundBonus(RefundRequest request) {
        log.info("Возврат на карту: {}", request.getCardNumber());
        
        BonusCard card = findActiveCard(request.getCardNumber());

        BonusTransaction originalTransaction = bonusTransactionRepository
                .findByTransactionId(request.getOriginalTransactionId())
                .orElseThrow(() -> new InvalidTransactionException(
                        "Транзакция не найдена: " + request.getOriginalTransactionId()
                ));

        if (!originalTransaction.getBonusCard().getId().equals(card.getId())) {
            throw new InvalidTransactionException("Транзакция не принадлежит данной карте");
        }

        BonusTransaction.TransactionType refundType;

        Double originalTransactionAmount = originalTransaction.getAmount();
        if (originalTransaction.getType() == BonusTransaction.TransactionType.WRITE_OFF) {
            refundType = BonusTransaction.TransactionType.REFUND;
            card.setBalance(card.getBalance() + originalTransactionAmount);
        } else if (originalTransaction.getType() == BonusTransaction.TransactionType.ACCRUAL) {
            refundType = BonusTransaction.TransactionType.REFUND;

            if (card.getBalance().compareTo(originalTransactionAmount) < 0) {
                throw new InsufficientBonusException(
                        String.format("Недостаточно бонусов для возврата. Доступно: %s, Запрошено: %s",
                                card.getBalance(), originalTransactionAmount)
                );
            }
            card.setBalance(card.getBalance() - originalTransactionAmount);
        } else {
            throw new InvalidTransactionException("Возврат возврата? Ты серьёзно?");
        }

        BonusTransaction refundTransaction = createTransaction(
                card,
                refundType,
                originalTransactionAmount,
                request.getDescription() != null ? 
                        request.getDescription() : 
                        "Возврат для транзакции: " + originalTransaction.getTransactionId(),
                request.getOrderId()
        );

        refundTransaction.setTransactionId(UUID.randomUUID().toString());

        bonusCardRepository.save(card);
        BonusTransaction savedTransaction = bonusTransactionRepository.save(refundTransaction);
        
        log.info("Успешный возврат. Баланс: {}", card.getBalance());
        return bonusTransactionMapper.toDto(savedTransaction);
    }
    
    public BigDecimal getBalance(String cardNumber) {
        log.info("Получение баланса по карте: {}", cardNumber);

        return bonusCardRepository.findBalanceByCardNumber(cardNumber)
                .orElseThrow(()-> new BonusCardNotFoundException(String.format("Карта %s не найдена", cardNumber)));
    }
    
    public List<BonusTransactionDto> getTransactionHistory(String cardNumber) {
        log.info("Получение истории транзакции по карте: {}", cardNumber);

        if (!bonusCardRepository.existsByCardNumber(cardNumber)) {
            throw new BonusCardNotFoundException("Бонусная карта не найдена: " + cardNumber);
        }
        
        List<BonusTransaction> transactions = bonusTransactionRepository
                .findHistoryByCardNumber(cardNumber);
        
        return transactions.stream()
                .map(bonusTransactionMapper::toDto)
                .collect(Collectors.toList());
    }
    
    private BonusCard findActiveCard(String cardNumber) {
        return bonusCardRepository.findByCardNumberAndIsActiveIsTrue(cardNumber)
                .orElseThrow(() -> new BonusCardNotFoundException(
                        "Активная бонусная карта не найдена: " + cardNumber
                ));
    }
    
    private BonusTransaction createTransaction(
            BonusCard card,
            BonusTransaction.TransactionType type,
            Double amount,
            String description,
            String orderId
    ) {
        return BonusTransaction.builder()
                .bonusCard(card)
                .type(type)
                .amount(amount)
                .description(description)
                .orderId(orderId)
                .transactionId(UUID.randomUUID().toString())
                .status(BonusTransaction.TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
    }
}