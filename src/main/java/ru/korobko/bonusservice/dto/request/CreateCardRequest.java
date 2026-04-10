package ru.korobko.bonusservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCardRequest {

    @NotBlank(message = "Номер карты обязателен")
    private String cardNumber;

    @NotNull(message = "ID клиента обязателен")
    private Long clientId;

    private String clientName;

    @NotNull(message = "Начальный баланс обязателен")
    private Double initialBalance;
}