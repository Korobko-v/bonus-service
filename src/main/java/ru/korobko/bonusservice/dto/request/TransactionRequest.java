package ru.korobko.bonusservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "Введите номер карты")
    private String cardNumber;

    @NotNull(message = "Введите сумму")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "Требуется идентификатор оригинальной транзакции")
    private String originalTransactionId;

    @NotBlank(message = "Требуется идентификатор заказа")
    private String orderId;
}
