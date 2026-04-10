package ru.korobko.bonusservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bonus_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private BonusCard bonusCard;
    
    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    @Column(name = "amount", nullable = false)
    private Double amount;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "transaction_id", unique = true)
    private String transactionId;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = TransactionStatus.COMPLETED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum TransactionType {
        ACCRUAL,      // Начисление
        WRITE_OFF,    // Списание
        REFUND        // Возврат
    }
    
    public enum TransactionStatus {
        PENDING,      // В обработке
        COMPLETED,    // Завершено
        CANCELLED,    // Отменено
        FAILED,        // Не удалось
        REFUND // Осуществлён возврат
    }
}