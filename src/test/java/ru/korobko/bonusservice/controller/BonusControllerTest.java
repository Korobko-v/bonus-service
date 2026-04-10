package ru.korobko.bonusservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.korobko.bonusservice.dto.BonusCardDto;
import ru.korobko.bonusservice.dto.BonusTransactionDto;
import ru.korobko.bonusservice.dto.request.CreateCardRequest;
import ru.korobko.bonusservice.dto.request.RefundRequest;
import ru.korobko.bonusservice.dto.request.TransactionRequest;
import ru.korobko.bonusservice.service.BonusCardService;
import ru.korobko.bonusservice.service.BonusService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.korobko.bonusservice.model.BonusTransaction.TransactionType.*;

@SpringBootTest
@AutoConfigureMockMvc
class BonusControllerFullTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BonusCardService bonusCardService;

    @MockBean
    private BonusService bonusService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCardAdminTest() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("NEW-001");
        request.setClientId(1L);
        request.setInitialBalance(100.0);

        BonusCardDto responseDto = BonusCardDto.builder()
                .id(1L)
                .cardNumber("NEW-001")
                .clientId(1L)
                .balance(BigDecimal.valueOf(100))
                .isActive(true)
                .build();

        when(bonusCardService.createCard(any(CreateCardRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/bonus/admin/card")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cardNumber").value("NEW-001"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCardUserThenReturn403Test() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("NEW-001");
        request.setClientId(1L);
        request.setInitialBalance(100.0);

        mockMvc.perform(post("/api/v1/bonus/admin/card")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void accrueBonusAdminTest() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234");
        request.setAmount(50.0);
        request.setOrderId("ORDER-001");
        request.setDescription("Test");

        BonusTransactionDto dto = BonusTransactionDto.builder()
                .transactionId(UUID.randomUUID().toString())
                .cardNumber("1234")
                .amount(BigDecimal.valueOf(50))
                .type(ACCRUAL)
                .build();

        when(bonusService.accrueBonus(any(TransactionRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/bonus/accrue")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void accrueBonusUserThenReturn403Test() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234");
        request.setAmount(50.0);
        request.setOrderId("ORDER-001");

        mockMvc.perform(post("/api/v1/bonus/accrue")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void writeOffBonusAdminTest() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234");
        request.setAmount(30.0);
        request.setOrderId("ORDER-002");

        BonusTransactionDto dto = BonusTransactionDto.builder()
                .transactionId(UUID.randomUUID().toString())
                .cardNumber("1234")
                .amount(BigDecimal.valueOf(30))
                .type(WRITE_OFF)
                .build();

        when(bonusService.writeOffBonus(any(TransactionRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/bonus/write-off")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void writeOffBonusUserThenReturnTest() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("1234");
        request.setAmount(30.0);
        request.setOrderId("ORDER-001");

        mockMvc.perform(post("/api/v1/bonus/write-off")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void refundBonusAdminTest() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setCardNumber("1234");
        request.setOriginalTransactionId("tx-123");
        request.setOrderId("ORDER-003");

        BonusTransactionDto dto = BonusTransactionDto.builder()
                .transactionId(UUID.randomUUID().toString())
                .cardNumber("1234")
                .amount(BigDecimal.valueOf(30))
                .type(REFUND)
                .build();

        when(bonusService.refundBonus(any(RefundRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/bonus/refund")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void refundBonusUserThenReturn403Test() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setCardNumber("1234");
        request.setOriginalTransactionId("some-uuid");
        request.setOrderId("ORDER-123");

        mockMvc.perform(post("/api/v1/bonus/refund")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void getBalanceForAdminTest() throws Exception {
        when(bonusService.getBalanceForAdmin("1234"))
                .thenReturn(BigDecimal.valueOf(100));

        mockMvc.perform(get("/api/v1/bonus/balance/admin/1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBalanceForAdminUserThenReturn403Test() throws Exception {
        mockMvc.perform(get("/api/v1/bonus/balance/admin/1234"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyBalanceUserTest() throws Exception {
        when(bonusService.getMyBalance("1234")).thenReturn(BigDecimal.valueOf(100));

        mockMvc.perform(get("/api/v1/bonus/balance/1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMyBalanceAdminTest() throws Exception {
        when(bonusService.getMyBalance("1234")).thenReturn(BigDecimal.valueOf(100));

        mockMvc.perform(get("/api/v1/bonus/balance/1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTransactionHistoryForAdminTest() throws Exception {
        List<BonusTransactionDto> history = List.of(
                BonusTransactionDto.builder().transactionId("1").amount(BigDecimal.TEN).build()
        );
        when(bonusService.getTransactionHistoryForAdmin("1234")).thenReturn(history);

        mockMvc.perform(get("/api/v1/bonus/history/admin/1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionId").value("1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getTransactionHistoryForAdminUserThenReturn403Test() throws Exception {
        mockMvc.perform(get("/api/v1/bonus/history/admin/1234"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyTransactionHistoryUserTest() throws Exception {
        List<BonusTransactionDto> history = List.of(
                BonusTransactionDto.builder().transactionId("2").amount(BigDecimal.ONE).build()
        );
        when(bonusService.getMyTransactionHistory("1234")).thenReturn(history);

        mockMvc.perform(get("/api/v1/bonus/my-history/1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionId").value("2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMyTransactionHistoryAdminTest() throws Exception {
        List<BonusTransactionDto> history = List.of(
                BonusTransactionDto.builder().transactionId("3").amount(BigDecimal.ZERO).build()
        );
        when(bonusService.getMyTransactionHistory("1234")).thenReturn(history);

        mockMvc.perform(get("/api/v1/bonus/my-history/1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionId").value("3"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateCardAdminTest() throws Exception {
        doNothing().when(bonusCardService).deactivateCard(1L);

        mockMvc.perform(patch("/api/v1/bonus/admin/card/1/deactivate")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deactivateCardUserThenReturn403Test() throws Exception {
        mockMvc.perform(patch("/api/v1/bonus/admin/card/1/deactivate")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}