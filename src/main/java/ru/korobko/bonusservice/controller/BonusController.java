package ru.korobko.bonusservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.korobko.bonusservice.dto.BonusTransactionDto;
import ru.korobko.bonusservice.dto.request.TransactionRequest;
import ru.korobko.bonusservice.dto.response.ApiResponse;
import ru.korobko.bonusservice.service.BonusService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bonus")
@RequiredArgsConstructor
public class BonusController {
    
    private final BonusService bonusService;
    
    @PostMapping("/accrue")
    public ResponseEntity<ApiResponse<BonusTransactionDto>> accrueBonus(
            @Valid @RequestBody TransactionRequest request) {
        
        BonusTransactionDto transaction = bonusService.accrueBonus(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Успешное зачисление", transaction));
    }
    
    @PostMapping("/write-off")
    public ResponseEntity<ApiResponse<BonusTransactionDto>> writeOffBonus(
            @Valid @RequestBody TransactionRequest request) {
        
        BonusTransactionDto transaction = bonusService.writeOffBonus(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Успешное списание", transaction));
    }
    
    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<BonusTransactionDto>> refundBonus(
            @Valid @RequestBody TransactionRequest request) {
        
        BonusTransactionDto transaction = bonusService.refundBonus(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Успешный возврат", transaction));
    }
    
    @GetMapping("/balance/{cardNumber}")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(
            @PathVariable String cardNumber) {
        
        BigDecimal balance = bonusService.getBalance(cardNumber);
        
        return ResponseEntity
                .ok(ApiResponse.success("Информация о балансе получена", balance));
    }
    
    @GetMapping("/history/{cardNumber}")
    public ResponseEntity<ApiResponse<List<BonusTransactionDto>>> getTransactionHistory(
            @PathVariable String cardNumber) {
        
        List<BonusTransactionDto> history = bonusService.getTransactionHistory(cardNumber);
        
        return ResponseEntity
                .ok(ApiResponse.success("Информация об истории транзакций получена", history));
    }
}