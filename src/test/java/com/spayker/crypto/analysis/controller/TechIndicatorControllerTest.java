package com.spayker.crypto.analysis.controller;

import com.spayker.crypto.analysis.config.SecurityConfig;
import com.spayker.crypto.analysis.dto.indicator.IndicatorMetaData;
import com.spayker.crypto.analysis.dto.indicator.IndicatorResponse;
import com.spayker.crypto.analysis.service.TechIndicatorManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TechIndicatorController.class)
@Import(SecurityConfig.class) // подключаем твой SecurityConfig
class TechIndicatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TechIndicatorManager techIndicatorManager;

    @Test
    void fetchAvailableIndicators_ShouldReturnList() throws Exception {
        Map<String, List<IndicatorMetaData>> mockList = Map.of(
                "btcusdt", List.of(
                        new IndicatorMetaData("SMA", 14, "50000.0", "hour"),
                        new IndicatorMetaData("EMA", 21, "51000.0", "hour")
                )
        );

        when(techIndicatorManager.getAvailableIndications()).thenReturn(mockList);

        mockMvc.perform(get("/v1/tech/indicators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.btcusdt[0].name").value("SMA"))
                .andExpect(jsonPath("$.btcusdt[0].size").value(14))
                .andExpect(jsonPath("$.btcusdt[0].last").value("50000.0"))
                .andExpect(jsonPath("$.btcusdt[0].timeFrame").value("hour"))
                .andExpect(jsonPath("$.btcusdt[1].name").value("EMA"))
                .andExpect(jsonPath("$.btcusdt[1].size").value(21))
                .andExpect(jsonPath("$.btcusdt[1].last").value("51000.0"))
                .andExpect(jsonPath("$.btcusdt[1].timeFrame").value("hour"));
    }

    @Test
    void fetchIndicatorDataByName_ShouldReturnIndicatorData() throws Exception {
        String symbol = "btcusdt";
        String indicatorName = "SMA";
        String timeFrame = "hour";

        IndicatorResponse mockResponse = new IndicatorResponse(symbol.toLowerCase(), timeFrame, List.of());
        when(techIndicatorManager.getTechIndicatorData(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/v1/{symbol}/tech/indicators/{indicatorName}", symbol, indicatorName)
                        .param("timeFrame", timeFrame))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coin").value(symbol))
                .andExpect(jsonPath("$.timeFrame").value(timeFrame));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void addIndicatorBySymbol_ShouldReturnCreated() throws Exception {
        String symbol = "ETH";
        String indicatorName = "EMA";
        String timeFrame = "minute";

        mockMvc.perform(post("/v1/{symbol}/tech/indicators/{indicatorName}", symbol, indicatorName)
                        .param("timeFrame", timeFrame))
                .andExpect(status().isCreated());

        verify(techIndicatorManager, times(1)).addIndicatorBySymbol(any());
    }
}