package ru.korobko.bonusservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.korobko.bonusservice.dto.BonusCardDto;
import ru.korobko.bonusservice.dto.BonusTransactionDto;
import ru.korobko.bonusservice.dto.request.CreateCardRequest;
import ru.korobko.bonusservice.dto.request.RefundRequest;
import ru.korobko.bonusservice.dto.request.TransactionRequest;
import ru.korobko.bonusservice.dto.response.ApiResponse;
import ru.korobko.bonusservice.service.BonusCardService;
import ru.korobko.bonusservice.service.BonusService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bonus")
@RequiredArgsConstructor
public class BonusController {
    
    private final BonusService bonusService;
    private final BonusCardService bonusCardService;

    @PostMapping("/admin/card")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BonusCardDto>> createCard(
            @Valid @RequestBody CreateCardRequest request) {
        BonusCardDto card = bonusCardService.createCard(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Карта успешно создана", card));
    }
    
    @PostMapping("/accrue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BonusTransactionDto>> accrueBonus(
            @Valid @RequestBody TransactionRequest request) {
        
        BonusTransactionDto transaction = bonusService.accrueBonus(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Успешное зачисление", transaction));
    }
    
    @PostMapping("/write-off")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BonusTransactionDto>> writeOffBonus(
            @Valid @RequestBody TransactionRequest request) {
        
        BonusTransactionDto transaction = bonusService.writeOffBonus(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Успешное списание", transaction));
    }
    
    @PostMapping("/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BonusTransactionDto>> refundBonus(
            @Valid @RequestBody RefundRequest request) {
        
        BonusTransactionDto transaction = bonusService.refundBonus(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Успешный возврат", transaction));
    }
    
    @GetMapping("/balance/admin/{cardNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalanceForAdmin(
            @PathVariable String cardNumber) {
        
        BigDecimal balance = bonusService.getBalanceForAdmin(cardNumber);
        
        return ResponseEntity
                .ok(ApiResponse.success("Информация о балансе получена", balance));
    }

    @GetMapping("/balance/{cardNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BigDecimal>> getMyBalance(
            @PathVariable String cardNumber) {

        BigDecimal balance = bonusService.getMyBalance(cardNumber);

        return ResponseEntity
                .ok(ApiResponse.success("Информация о балансе получена", balance));
    }


    @GetMapping("/history/admin/{cardNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BonusTransactionDto>>> getTransactionHistoryForAdmin(
            @PathVariable String cardNumber) {
        
        List<BonusTransactionDto> history = bonusService.getTransactionHistoryForAdmin(cardNumber);
        
        return ResponseEntity
                .ok(ApiResponse.success("Информация об истории транзакций получена", history));
    }

    @GetMapping("/my-history/{cardNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BonusTransactionDto>>> getMyTransactionHistory(
            @PathVariable String cardNumber) {

        List<BonusTransactionDto> history = bonusService.getMyTransactionHistory(cardNumber);

        return ResponseEntity
                .ok(ApiResponse.success("Информация об истории транзакций текущего пользователя получена", history));
    }

    @PatchMapping("/admin/card/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateCard(@PathVariable Long id) {
        bonusCardService.deactivateCard(id);
        return ResponseEntity.ok(ApiResponse.success("Карта деактивирована", null));
    }
}