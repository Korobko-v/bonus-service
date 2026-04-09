package ru.korobko.bonusservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotBlank(message = "Введите номер карты")
    private String cardNumber;

    private String description;

    @NotBlank(message = "Требуется идентификатор оригинальной транзакции")
    private String originalTransactionId;

    @NotBlank(message = "Требуется идентификатор заказа")
    private String orderId;
}
