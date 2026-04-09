package ru.korobko.bonusservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bonus_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusCard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_number", unique = true, nullable = false)
    private String cardNumber;
    
    @Column(name = "client_id", nullable = false)
    private String clientId;
    
    @Column(name = "client_name")
    private String clientName;
    
    @Column(name = "balance", nullable = false)
    private BigDecimal balance;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "bonusCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BonusTransaction> transactions = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        balance = BigDecimal.ZERO;
        isActive = true;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}