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
import ru.korobko.bonusservice.dto.request.CreateCardRequest;
import ru.korobko.bonusservice.service.BonusCardService;
import ru.korobko.bonusservice.service.BonusService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BonusControllerTest {

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
    void createCardAsAdmin_shouldReturn201() throws Exception {
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCardAsUser_shouldReturn403() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("NEW-001");
        request.setClientId(1L);
        request.setInitialBalance(100.0);

        mockMvc.perform(post("/api/v1/bonus/admin/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}