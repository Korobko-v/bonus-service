package ru.korobko.bonusservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusCardDto {
    private Long id;
    private String cardNumber;
    private String clientId;
    private String clientName;
    private BigDecimal balance;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}